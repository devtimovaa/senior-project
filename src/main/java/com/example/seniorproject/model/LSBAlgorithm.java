package com.example.seniorproject.model;

import java.awt.image.BufferedImage;

// helper functions from the LSBMethods class
import static com.example.seniorproject.model.LSBMethods.checksum;
import static com.example.seniorproject.model.LSBMethods.readByteFromPixels;
import static com.example.seniorproject.model.LSBMethods.storeByteInPixels;

/*  
This is the class referencing the lSB algorithm.
LSB = Least Significant Bit - we change the smallest, least noticeable part of each color channel
*/
// The class will follow the rules of the interface
public class LSBAlgorithm implements SteganographyAlgorithm {

    // define the contsants
    private static final int HEADER_BYTES   = 4; // used to store the length of the data
    private static final int CHECKSUM_BYTES = 1; // used to verify the integrity extracted data 

    // embedding method
    @Override
    public BufferedImage embed(BufferedImage base, byte[] data) {

        byte[] payload = data == null ? new byte[0] : data; // treat no data as empty array

        int bitsNeeded = (HEADER_BYTES + payload.length + CHECKSUM_BYTES) * 8;  //how much  in bits we want to hide
        //pixel has 3 bits - one for red, one for blue, one for green
        int bitsAvailable = base.getWidth() * base.getHeight() * 3; //how much bits are available deppending on thsi image
        //check if the image is large enough
        if (bitsNeeded > bitsAvailable) {
            throw new IllegalArgumentException(
                    "The selected image too small: you want to hide " + bitsNeeded + " bits, but only " + bitsAvailable + " are available. ");
        }

        // Make a copy of the image so we don't change the original image - blank image of the same size
        int w = base.getWidth();
        int h = base.getHeight();
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        copy.setRGB(0,0, w, h, base.getRGB(0, 0, w, h, null, 0, w), 0, w);

        // Payload length stored as 4 bytes in big-endian order
        for (int i = 0; i < HEADER_BYTES; i++) {
            int shift = 24 - 8 * i;
            storeByteInPixels((byte) ((payload.length >> shift) & 0xFF), i, copy);
        }

        for (int i = 0; i < payload.length; i++) {
            storeByteInPixels(payload[i], HEADER_BYTES + i, copy);
        }

        storeByteInPixels(checksum(payload), HEADER_BYTES + payload.length, copy);

        return copy;
    }

    @Override
    public byte[] extract(BufferedImage base) {
        int maxPayload = (base.getWidth() * base.getHeight() * 3) / 8
                         - HEADER_BYTES - CHECKSUM_BYTES;
        if (maxPayload < 0) {
            throw new IllegalArgumentException("Image is too small to contain hidden data");
        }

        // Read the 4-byte header back into an int, reversing the big-endian split
        int size = 0;
        for (int i = 0; i < HEADER_BYTES; i++) {
            size = (size << 8) | (0xFF & readByteFromPixels(i, base));
        }

        if (size < 0 || size > maxPayload) {
            throw new IllegalStateException(
                    "No valid hidden data found in this image (decoded length: " + size + ")");
        }

        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = readByteFromPixels(HEADER_BYTES + i, base);
        }

        byte computed       = checksum(result);
        byte storedChecksum = readByteFromPixels(HEADER_BYTES + size, base);
        if (computed != storedChecksum) {
            throw new RuntimeException("Checksum mismatch: data may be corrupted");
        }

        return result;
    }
}
