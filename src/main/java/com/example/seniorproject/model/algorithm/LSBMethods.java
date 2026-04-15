package com.example.seniorproject.model.algorithm;

import java.awt.image.BufferedImage;

// Helpers for LSB-based steganography algorithms.
class LSBMethods {

    private LSBMethods() {}

    //Reads one byte from pixel
    static byte readByteFromPixels(int byteIndex, BufferedImage image) {
        byte b = 0x00;
        for (int i = 0; i < 8; i++) {
            b = (byte) ((b << 1) | (readBitFromPixel(byteIndex * 8 + 7 - i, image) ? 1 : 0));
        }
        return b;
    }

    //Converts a bit index to pixel 
    private static boolean readBitFromPixel(int bitIndex, BufferedImage image) {
        int channel = bitIndex % 3;
        int pixelIndex = bitIndex / 3;
        int x = pixelIndex % image.getWidth();
        int y = pixelIndex / image.getWidth();
        return readBitFromPixel(x, y, channel, image);
    }

    //Gets the LSB of a color channel
    private static boolean readBitFromPixel(int x, int y, int channel, BufferedImage image) {
        assert channel >= 0 && channel < 3;
        int channelValue = image.getRGB(x, y) >> (16 - 8 * channel);
        return (channelValue & 0x01) == 0x01;
    }

    //Writes a byte into consecutive pixel LSBs
    static void storeByteInPixels(byte b, int byteIndex, BufferedImage image) {
        for (int i = 0; i < 8; i++) {
            storeBitInPixel((b & (1 << i)) != 0, byteIndex * 8 + i, image);
        }
    }

    //Converts a bit index to pixel coordinates and channel
    private static void storeBitInPixel(boolean bit, int bitIndex, BufferedImage image) {
        int channel = bitIndex % 3;
        int pixelIndex = bitIndex / 3;
        int x = pixelIndex % image.getWidth();
        int y = pixelIndex / image.getWidth();
        storeBitInPixel(bit, x, y, channel, image);
    }

    //Sets or clears the LSB of one channel
    private static void storeBitInPixel(boolean bit, int x, int y, int channel, BufferedImage image) {
        assert channel >= 0 && channel < 3;
        int shift = 16 - 8 * channel;
        int oldPixel = image.getRGB(x, y);
        int oldChannelValue = oldPixel >> shift;
        int newChannelValue = bit ? (oldChannelValue | 0x01) : (oldChannelValue & 0xFE);
        int newPixel = (oldPixel & ~(0xFF << shift)) | (newChannelValue << shift);
        image.setRGB(x, y, newPixel);
    }

    //Copy of an image so the original stays untouched
    static BufferedImage copyImage(BufferedImage original) {
        int w = original.getWidth();
        int h = original.getHeight();
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        copy.setRGB(0, 0, w, h, original.getRGB(0, 0, w, h, null, 0, w), 0, w);
        return copy;
    }

    //XOR checksum for data integrity
    static byte checksum(byte[] data) {
        byte xor = 0;
        for (byte b : data) {
            xor ^= b;
        }
        return xor;
    }
}
