package com.example.seniorproject.model;


import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.example.seniorproject.model.LSBMethods.readByteFromPixels;
import static com.example.seniorproject.model.LSBMethods.storeByteInPixels;


public class RandomizedLSBAlgorithm implements SteganographyAlgorithm {

    private static final byte MAGIC_0 = (byte) 0xAB;
    private static final byte MAGIC_1 = (byte) 0xCD;

    private final int seed;

    public RandomizedLSBAlgorithm(int seed) {
        this.seed = seed;
    }

    @Override
    public BufferedImage embed(BufferedImage base, byte[] data) {
        if (data == null) {
            data = new byte[0];
        }

        List<Integer> order = getShuffledOrder(base);

        // Checks if image is large enough
        int slotsNeeded = 2 + 4 + data.length + 1;
        if (slotsNeeded > order.size()) {
            throw new IllegalArgumentException("Image is too small to embed this message");
        }

        // Makes a copy of the image so the original is never touched
        int w = base.getWidth();
        int h = base.getHeight();
        BufferedImage copyImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        copyImage.setRGB(0, 0, w, h, base.getRGB(0, 0, w, h, null, 0, w), 0, w);

        int slot = 0;

        // We will use magic bytes so we can detect if a message exists
        storeByteInPixels(MAGIC_0, order.get(slot++), copyImage);
        storeByteInPixels(MAGIC_1, order.get(slot++), copyImage);

        // Write the length as 4 bytes becausea single byte can hold 0-255
        storeByteInPixels((byte) ((data.length >> 24) & 0xFF), order.get(slot++), copyImage);
        storeByteInPixels((byte) ((data.length >> 16) & 0xFF), order.get(slot++), copyImage);
        storeByteInPixels((byte) ((data.length >>  8) & 0xFF), order.get(slot++), copyImage);
        storeByteInPixels((byte)  (data.length        & 0xFF), order.get(slot++), copyImage);

        // Byte of message is written to the image
        for (byte b : data) {
            storeByteInPixels(b, order.get(slot++), copyImage);
        }

        storeByteInPixels(checksum(data), order.get(slot), copyImage);

        return copyImage;
    }

    @Override
    public byte[] extract(BufferedImage base) {
        List<Integer> order = getShuffledOrder(base);

        int slot = 0;

        // Check magic bytes
        byte m0 = readByteFromPixels(order.get(slot++), base);
        byte m1 = readByteFromPixels(order.get(slot++), base);

        if (m0 != MAGIC_0 || m1 != MAGIC_1) {
            throw new IllegalStateException("No hidden message found in this image");
        }

        // The reverse of the bit-shifting done during embed
        byte b1 = readByteFromPixels(order.get(slot++), base);
        byte b2 = readByteFromPixels(order.get(slot++), base);
        byte b3 = readByteFromPixels(order.get(slot++), base);
        byte b4 = readByteFromPixels(order.get(slot++), base);

        int size = ((0xFF & b1) << 24) | ((0xFF & b2) << 16)
                 | ((0xFF & b3) <<  8) |  (0xFF & b4);

        int maxSize = order.size() - 2 - 4 - 1;
        if (size < 0 || size > maxSize) {
            throw new IllegalStateException("Could not read message - did you use the right seed?");
        }

        // Read each byte of the message
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = readByteFromPixels(order.get(slot++), base);
        }

       
        byte storedChecksum = readByteFromPixels(order.get(slot), base);
        if (checksum(result) != storedChecksum) {
            throw new RuntimeException("Checksum mismatch - message may be corrupted or wrong seed used");
        }

        return result;
    }

    // X0R of all bytes
    private static byte checksum(byte[] data) {
        byte xor = 0;
        for (byte b : data) {
            xor ^= b;
        }
        return xor;
    }

    /*
    Each image has 3 colour channels (R, G, B)
    The total number of bits available is width × height × 3, and dividing by 8 gives the number of byte slots.
     */
    private List<Integer> getShuffledOrder(BufferedImage image) {

        int totalSlots = (image.getWidth() * image.getHeight() * 3) / 8;
        List<Integer> order = new ArrayList<>();

        for (int i = 0; i < totalSlots; i++) {
            order.add(i);
        }

        Collections.shuffle(order, new Random(seed)); // rearranges using new Random(seed)
        return order;
    }
}
