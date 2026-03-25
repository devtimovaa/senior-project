package com.example.seniorproject.algorithms;

import java.awt.image.BufferedImage;

public class LSBSteganography implements SteganographyAlgorithm {

    private static final int HEADER_BYTES = 4;

    @Override
    public BufferedImage embed(BufferedImage base, byte[] secret) {
        byte[] msg = secret == null ? new byte[0] : secret;
        byte[] payload = withLengthHeader(msg);
        int w = base.getWidth();
        int h = base.getHeight();
        int[] pixels = base.getRGB(0, 0, w, h, null, 0, w);

        int bitsNeeded = payload.length * 8;
        if (bitsNeeded > pixels.length * 3) {
            throw new IllegalArgumentException(
                    "Image too small: need " + bitsNeeded + " bits, have " + pixels.length * 3);
        }

        int bitIndex = 0;
        for (int i = 0; i < pixels.length && bitIndex < bitsNeeded; i++) {
            int a = (pixels[i] >> 24) & 0xFF;
            int r = (pixels[i] >> 16) & 0xFF;
            int g = (pixels[i] >> 8) & 0xFF;
            int b = pixels[i] & 0xFF;

            r = (r & 0xFE) | bitAt(payload, bitIndex++);
            if (bitIndex < bitsNeeded) g = (g & 0xFE) | bitAt(payload, bitIndex++);
            if (bitIndex < bitsNeeded) b = (b & 0xFE) | bitAt(payload, bitIndex++);

            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, w, h, pixels, 0, w);
        return result;
    }

    @Override
    public byte[] extract(BufferedImage base) {
        int w = base.getWidth();
        int h = base.getHeight();
        int[] pixels = base.getRGB(0, 0, w, h, null, 0, w);

        byte[] header = readBytes(pixels, HEADER_BYTES, 0);
        int len = (header[0] & 0xFF) << 24 | (header[1] & 0xFF) << 16
                | (header[2] & 0xFF) << 8 | (header[3] & 0xFF);

        int maxMsg = (pixels.length * 3) / 8 - HEADER_BYTES;
        if (len < 0 || len > maxMsg) {
            return new byte[0];
        }

        return readBytes(pixels, len, HEADER_BYTES * 8);
    }

    private static byte[] withLengthHeader(byte[] msg) {
        int len = msg.length;
        byte[] out = new byte[HEADER_BYTES + len];
        out[0] = (byte) (len >> 24);
        out[1] = (byte) (len >> 16);
        out[2] = (byte) (len >> 8);
        out[3] = (byte) len;
        System.arraycopy(msg, 0, out, HEADER_BYTES, len);
        return out;
    }

    private static int bitAt(byte[] bytes, int index) {
        int byteIndex = index / 8;
        int bitInByte = 7 - (index % 8);
        return (bytes[byteIndex] >> bitInByte) & 1;
    }

    private static byte[] readBytes(int[] pixels, int byteCount, int startBit) {
        byte[] out = new byte[byteCount];
        int outIndex = 0;
        int bitPos = 0;
        int current = 0;
        int bitsSeen = 0;

        for (int rgb : pixels) {
            for (int c : new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF}) {
                if (bitsSeen++ < startBit) continue;
                current = (current << 1) | (c & 1);
                bitPos++;
                if (bitPos == 8) {
                    out[outIndex++] = (byte) current;
                    current = 0;
                    bitPos = 0;
                    if (outIndex >= byteCount) return out;
                }
            }
        }
        return outIndex == byteCount ? out : new byte[0];
    }
}
