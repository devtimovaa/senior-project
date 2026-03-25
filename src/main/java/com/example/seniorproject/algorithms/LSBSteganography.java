package com.example.seniorproject.algorithms;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LSBSteganography implements SteganographyAlgorithm {

    @Override
    public BufferedImage embed(BufferedImage base, String secret) {
        int w = base.getWidth();
        int h = base.getHeight();
        int[] pixels = base.getRGB(0, 0, w, h, null, 0, w);

        // message as bytes, plus one 0 byte at the end to mark "stop"
        byte[] msg = secret.getBytes(StandardCharsets.UTF_8);
        int totalBytes = msg.length + 1;
        int totalBits = totalBytes * 8;

        if (totalBits > pixels.length * 3) {
            throw new IllegalArgumentException("Image is too small for this message!");
        }

        int bitIndex = 0;
        for (int i = 0; i < pixels.length && bitIndex < totalBits; i++) {
            int a = (pixels[i] >> 24) & 0xFF;
            int r = (pixels[i] >> 16) & 0xFF;
            int g = (pixels[i] >> 8) & 0xFF;
            int b = pixels[i] & 0xFF;

            //put one bit of the text secret into the lowest bit of r, then g, then b
            r = putOneBit(r, getOneBit(msg, bitIndex));
            bitIndex++;
            if (bitIndex < totalBits) {
                g = putOneBit(g, getOneBit(msg, bitIndex));
                bitIndex++;
            }
            if (bitIndex < totalBits) {
                b = putOneBit(b, getOneBit(msg, bitIndex));
                bitIndex++;
            }

            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    @Override
    public String extract(BufferedImage base) {
        int w = base.getWidth();
        int h = base.getHeight();
        int[] pixels = base.getRGB(0, 0, w, h, null, 0, w);

        ArrayList<Byte> list = new ArrayList<>();
        int bitsInCurrentByte = 0;
        int currentByte = 0;

        for (int i = 0; i < pixels.length; i++) {
            int r = (pixels[i] >> 16) & 0xFF;
            int g = (pixels[i] >> 8) & 0xFF;
            int b = pixels[i] & 0xFF;

            int[] parts = {r, g, b};
            for (int part : parts) {
                int bit = part & 1;
                currentByte = (currentByte << 1) | bit;
                bitsInCurrentByte++;

                if (bitsInCurrentByte == 8) {
                    if (currentByte == 0) {
                        return bytesToString(list);
                    }
                    list.add((byte) currentByte);
                    currentByte = 0;
                    bitsInCurrentByte = 0;
                }
            }
        }

        return bytesToString(list);
    }

    //turn a list of bytes into a String
    private static String bytesToString(ArrayList<Byte> list) {
        byte[] arr = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return new String(arr, StandardCharsets.UTF_8);
    }

    //message + end 0
    private static int getOneBit(byte[] msg, int n) {
        int whichByte = n / 8;
        int whichBit = 7 - (n % 8);
        if (whichByte < msg.length) {
            return (msg[whichByte] >> whichBit) & 1;
        }
        return 0;
    }

    //replace the lowest bit of color with a bit
    private static int putOneBit(int color, int bit) {
        return (color & 0xFE) | (bit & 1);
    }
}
