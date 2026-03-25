package com.example.seniorproject.algorithms;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import static com.example.seniorproject.algorithms.LSBMethods.readByteFromPixels;
import static com.example.seniorproject.algorithms.LSBMethods.storeByteInPixels;

public class LSBSteganography implements SteganographyAlgorithm {

    private static final int HEADER_BYTES   = 4;
    private static final int CHECKSUM_BYTES = 1;

    @Override
    public BufferedImage embed(BufferedImage base, byte[] data) {
        byte[] payload = data == null ? new byte[0] : data;

        int bitsNeeded    = (HEADER_BYTES + payload.length + CHECKSUM_BYTES) * 8;
        int bitsAvailable = base.getWidth() * base.getHeight() * 3;
        if (bitsNeeded > bitsAvailable) {
            throw new IllegalArgumentException(
                    "Image too small: need " + bitsNeeded + " bits, have " + bitsAvailable);
        }

        int w = base.getWidth();
        int h = base.getHeight();
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        copy.setRGB(0, 0, w, h, base.getRGB(0, 0, w, h, null, 0, w), 0, w);

        storeByteInPixels((byte) ((payload.length >> 24) & 0xFF), 0, copy);
        storeByteInPixels((byte) ((payload.length >> 16) & 0xFF), 1, copy);
        storeByteInPixels((byte) ((payload.length >>  8) & 0xFF), 2, copy);
        storeByteInPixels((byte)  (payload.length        & 0xFF), 3, copy);

        for (int i = 0; i < payload.length; i++) {
            storeByteInPixels(payload[i], HEADER_BYTES + i, copy);
        }

        storeByteInPixels((byte) Arrays.hashCode(payload), HEADER_BYTES + payload.length, copy);

        return copy;
    }

    @Override
    public byte[] extract(BufferedImage base) {
        int maxPayload = (base.getWidth() * base.getHeight() * 3) / 8
                         - HEADER_BYTES - CHECKSUM_BYTES;
        if (maxPayload < 0) {
            throw new IllegalArgumentException("Image is too small to contain hidden data");
        }

        byte b1 = readByteFromPixels(0, base);
        byte b2 = readByteFromPixels(1, base);
        byte b3 = readByteFromPixels(2, base);
        byte b4 = readByteFromPixels(3, base);
        int size = ((0xFF & b1) << 24) | ((0xFF & b2) << 16)
                 | ((0xFF & b3) <<  8) |  (0xFF & b4);

        if (size < 0 || size > maxPayload) {
            throw new IllegalStateException(
                    "No valid hidden data found in this image (decoded length: " + size + ")");
        }

        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = readByteFromPixels(HEADER_BYTES + i, base);
        }

        byte checksum       = (byte) Arrays.hashCode(result);
        byte storedChecksum = readByteFromPixels(HEADER_BYTES + size, base);
        if (checksum != storedChecksum) {
            throw new RuntimeException("Checksum mismatch: data may be corrupted");
        }

        return result;
    }
}
