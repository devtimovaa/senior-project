package com.example.seniorproject.model.algorithm;

import java.awt.image.BufferedImage;

import static com.example.seniorproject.model.algorithm.LSBMethods.checksum;
import static com.example.seniorproject.model.algorithm.LSBMethods.copyImage;
import static com.example.seniorproject.model.algorithm.LSBMethods.readByteFromPixels;
import static com.example.seniorproject.model.algorithm.LSBMethods.storeByteInPixels;

/* 
 Sequential LSB (Least Significant Bit) steganography
 This algorithm hides data by replacing the least significant bit of each RGB channel in consecutive pixels.
*/
public class LSBAlgorithm implements SteganographyAlgorithm {

    private static final int HEADER_BYTES   = 4;
    private static final int CHECKSUM_BYTES = 1;

    //Embeds secret data into a cover image containing the hidden data
    @Override
    public BufferedImage embed(BufferedImage coverImage, byte[] secret) {
        byte[] payload = secret == null ? new byte[0] : secret;

        //Each pixel has 3 color channels (R, G, B) and each provides 1 usable bit
        int bitsNeeded = (HEADER_BYTES + payload.length + CHECKSUM_BYTES) * 8;
        int bitsAvailable = coverImage.getWidth() * coverImage.getHeight() * 3;
        if (bitsNeeded > bitsAvailable) {
            throw new IllegalArgumentException(
                    "Image too small: need " + bitsNeeded + " bits, have " + bitsAvailable);
        }

        BufferedImage stegoImage = copyImage(coverImage);

        //Split payload length into 4 bytes (big-endian) and store sequentially
        for (int i = 0; i < HEADER_BYTES; i++) {
            int shift = 24 - 8 * i;
            storeByteInPixels((byte) ((payload.length >> shift) & 0xFF), i, stegoImage);
        }

        for (int i = 0; i < payload.length; i++) {
            storeByteInPixels(payload[i], HEADER_BYTES + i, stegoImage);
        }

        storeByteInPixels(checksum(payload), HEADER_BYTES + payload.length, stegoImage);

        return stegoImage;
    }

    //Extracts hidden data from a stego image.
    @Override
    public byte[] extract(BufferedImage stegoImage) {
        int maxLen = (stegoImage.getWidth() * stegoImage.getHeight() * 3) / 8
                     - HEADER_BYTES - CHECKSUM_BYTES;
        if (maxLen < 0) {
            throw new IllegalArgumentException("Image is too small to contain hidden data");
        }

        //Reassemble the 4-byte big-endian header into an int
        int dataLen = 0;
        for (int i = 0; i < HEADER_BYTES; i++) {
            dataLen = (dataLen << 8) | (0xFF & readByteFromPixels(i, stegoImage));
        }

        if (dataLen < 0 || dataLen > maxLen) {
            throw new IllegalStateException(
                    "No valid hidden data found (decoded length: " + dataLen + ")");
        }

        byte[] payload = new byte[dataLen];
        for (int i = 0; i < dataLen; i++) {
            payload[i] = readByteFromPixels(HEADER_BYTES + i, stegoImage);
        }

        //Verify integrity
        byte computed = checksum(payload);
        byte stored = readByteFromPixels(HEADER_BYTES + dataLen, stegoImage);
        if (computed != stored) {
            throw new IllegalStateException("Checksum mismatch: data may be corrupted");
        }

        return payload;
    }
}
