package com.example.seniorproject.model.algorithm;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.example.seniorproject.model.algorithm.LSBMethods.checksum;
import static com.example.seniorproject.model.algorithm.LSBMethods.copyImage;
import static com.example.seniorproject.model.algorithm.LSBMethods.readByteFromPixels;
import static com.example.seniorproject.model.algorithm.LSBMethods.storeByteInPixels;

/*
 Randomized LSB steganography.
 Unlike the sequential LSB algorithm, byte slots are shuffled using a key so the hidden data is scattered across the image.
 Without the correct key, the data cannot be recovered.
*/

//Magic bytes are used to check if the key is the correct one
public class RandomizedLSBAlgorithm implements SteganographyAlgorithm {

    private static final byte MAGIC_0 = (byte) 0xAB;
    private static final byte MAGIC_1 = (byte) 0xCD;
    private static final int MAGIC_BYTES = 2;
    private static final int HEADER_BYTES = 4;
    private static final int CHECKSUM_BYTES = 1;

    private final int key;

    public RandomizedLSBAlgorithm(int key) {
        this.key = key;
    }

    //Embeds secret data at shuffled byte positions so it is scattered across the image
    @Override
    public BufferedImage embed(BufferedImage coverImage, byte[] secret) {
        byte[] payload = secret == null ? new byte[0] : secret;

        List<Integer> order = getShuffledOrder(coverImage);

        int slotsNeeded = MAGIC_BYTES + HEADER_BYTES + payload.length + CHECKSUM_BYTES;
        if (slotsNeeded > order.size()) {
            throw new IllegalArgumentException("The image is too small to embed this message");
        }

        BufferedImage stegoImage = copyImage(coverImage);

        int slot = 0;

        //Magic bytes let us detect if a message exists during extraction
        storeByteInPixels(MAGIC_0, order.get(slot++), stegoImage);
        storeByteInPixels(MAGIC_1, order.get(slot++), stegoImage);

        //Payload length is split into 4 bytes, big-endian
        for (int i = 0; i < HEADER_BYTES; i++) {
            int shift = 24 - 8 * i;
            storeByteInPixels((byte) ((payload.length >> shift) & 0xFF), order.get(slot++), stegoImage);
        }

        for (byte b : payload) {
            storeByteInPixels(b, order.get(slot++), stegoImage);
        }

        storeByteInPixels(checksum(payload), order.get(slot), stegoImage);

        return stegoImage;
    }

    //Extracts hidden data by regenerating the same shuffled order from the key
    @Override
    public byte[] extract(BufferedImage stegoImage) {
        List<Integer> order = getShuffledOrder(stegoImage);

        int slot = 0;

        //If magic bytes don't match, either no data or wrong key
        byte m0 = readByteFromPixels(order.get(slot++), stegoImage);
        byte m1 = readByteFromPixels(order.get(slot++), stegoImage);
        if (m0 != MAGIC_0 || m1 != MAGIC_1) {
            throw new IllegalStateException("No hidden message found in this image");
        }

        //Reassemble the 4-byte big-endian header into an int
        int dataLen = 0;
        for (int i = 0; i < HEADER_BYTES; i++) {
            dataLen = (dataLen << 8) | (0xFF & readByteFromPixels(order.get(slot++), stegoImage));
        }

        int maxLen = order.size() - MAGIC_BYTES - HEADER_BYTES - CHECKSUM_BYTES;
        if (dataLen < 0 || dataLen > maxLen) {
            throw new IllegalStateException("Could not read message - did you use the right key?");
        }

        byte[] payload = new byte[dataLen];
        for (int i = 0; i < dataLen; i++) {
            payload[i] = readByteFromPixels(order.get(slot++), stegoImage);
        }

        //Verify integrity
        byte computed = checksum(payload);
        byte stored = readByteFromPixels(order.get(slot), stegoImage);
        if (computed != stored) {
            throw new IllegalStateException("Checksum mismatch: data may be corrupted or wrong key used");
        }

        return payload;
    }

    //Builds a shuffled list for the given image - the same key always produces the same order
    private List<Integer> getShuffledOrder(BufferedImage image) {
        int totalSlots = (image.getWidth() * image.getHeight() * 3) / 8;
        List<Integer> order = new ArrayList<>(totalSlots);

        for (int i = 0; i < totalSlots; i++) {
            order.add(i);
        }

        Collections.shuffle(order, new Random(key));
        return order;
    }
}
