package com.example.seniorproject.algorithms;

import java.awt.image.BufferedImage;
import java.util.Random;


public class RandomizedLSB implements SteganographyAlgorithm {

    private static final int LENGTH_BYTES = 4;
    private static final int BITS_PER_PIXEL = 3;

    private final String stegoKey;

    public RandomizedLSB(String stegoKey) {
        String k = stegoKey == null ? "" : stegoKey.trim();
        if (k.isEmpty())
            throw new IllegalArgumentException("Stego Key is required");
        this.stegoKey = k;
    }

    @Override
    public BufferedImage embed(BufferedImage base, byte[] secret) {
        int w = base.getWidth();
        int h = base.getHeight();
        int totalPixels = w * h;
        int[] pixels = base.getRGB(0, 0, w, h, null, 0, w);

        int payloadLen = secret == null ? 0 : secret.length;
        byte[] toEmbed = buildMessage(lengthToBytes(payloadLen), secret);
        int totalBits = toEmbed.length * 8;

        if (totalBits > totalPixels * BITS_PER_PIXEL) {
            throw new IllegalArgumentException("Image too small");
        }

        int[] order = buildPermutation(totalPixels, stegoKey);

        int bitIndex = 0;
        for (int k = 0; k < order.length && bitIndex < totalBits; k++) {
            int i = order[k];
            int p = pixels[i];
            int a = (p >> 24) & 0xFF;
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;

            r = setLsb(r, getBit(toEmbed, bitIndex));
            bitIndex++;
            if (bitIndex < totalBits) g = setLsb(g, getBit(toEmbed, bitIndex));
            bitIndex++;
            if (bitIndex < totalBits) b = setLsb(b, getBit(toEmbed, bitIndex));
            bitIndex++;

            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    @Override
    public byte[] extract(BufferedImage base) {
        int w = base.getWidth();
        int h = base.getHeight();
        int totalPixels = w * h;
        int[] pixels = base.getRGB(0, 0, w, h, null, 0, w);
        int[] order = buildPermutation(totalPixels, stegoKey);
        int maxBytes = (totalPixels * BITS_PER_PIXEL) / 8;
        byte[] raw = new byte[maxBytes];

        int byteIdx = 0;
        int bitCount = 0;
        int currentByte = 0;

        for (int k = 0; k < order.length && byteIdx < maxBytes; k++) {
            int p = pixels[order[k]];
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;

            currentByte = (currentByte << 1) | (r & 1);
            if (++bitCount == 8) { raw[byteIdx++] = (byte) currentByte; bitCount = 0; currentByte = 0; }
            if (byteIdx >= maxBytes) break;
            currentByte = (currentByte << 1) | (g & 1);
            if (++bitCount == 8) { raw[byteIdx++] = (byte) currentByte; bitCount = 0; currentByte = 0; }
            if (byteIdx >= maxBytes) break;
            currentByte = (currentByte << 1) | (b & 1);
            if (++bitCount == 8) { raw[byteIdx++] = (byte) currentByte; bitCount = 0; currentByte = 0; }
        }

        if (byteIdx < LENGTH_BYTES) return new byte[0];
        int len = readIntBigEndian(raw, 0);
        if (len < 0 || len > byteIdx - LENGTH_BYTES) return new byte[0];
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) result[i] = raw[LENGTH_BYTES + i];
        return result;
    }

    // --- Permutation from key ---

    private static int[] buildPermutation(int n, String key) {
        Random rng = new Random(seedFromKey(key));
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        return order;
    }

    private static long seedFromKey(String key) {
        byte[] b;
        try {
            b = key.getBytes("UTF-8");
        } catch (Exception e) {
            b = key.getBytes();
        }
        long seed = 0x123456789ABCDEF0L;
        for (int i = 0; i < b.length; i++) seed = 31 * seed + (b[i] & 0xFF);
        return seed;
    }


    private static byte[] lengthToBytes(int len) {
        return new byte[]{
            (byte) (len >> 24),
            (byte) (len >> 16),
            (byte) (len >> 8),
            (byte) len
        };
    }

    private static int readIntBigEndian(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16)
             | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

    private static byte[] buildMessage(byte[] lengthBytes, byte[] payload) {
        int n = LENGTH_BYTES + (payload == null ? 0 : payload.length);
        byte[] out = new byte[n];
        out[0] = lengthBytes[0];
        out[1] = lengthBytes[1];
        out[2] = lengthBytes[2];
        out[3] = lengthBytes[3];
        if (payload != null && payload.length > 0) {
            for (int i = 0; i < payload.length; i++) out[LENGTH_BYTES + i] = payload[i];
        }
        return out;
    }

    private static int getBit(byte[] data, int bitIndex) {
        int byteIndex = bitIndex / 8;
        int bitInByte = 7 - (bitIndex % 8);
        if (byteIndex >= data.length) return 0;
        return ((data[byteIndex] & 0xFF) >> bitInByte) & 1;
    }

    private static int setLsb(int colorComponent, int bit) {
        return (colorComponent & 0xFE) | (bit & 1);
    }
}
