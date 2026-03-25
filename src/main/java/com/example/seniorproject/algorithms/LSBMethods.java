package com.example.seniorproject.algorithms;

import java.awt.image.BufferedImage;

class LSBMethods {

    private LSBMethods() {}

    static byte readByteFromPixels(int byteId, BufferedImage base) {
        byte b = 0x00;
        for (int i = 0; i < 8; i++) {
            b = (byte) ((b << 1) | (readBitFromPixel(byteId * 8 + 7 - i, base) ? 1 : 0));
        }
        return b;
    }

    private static boolean readBitFromPixel(int bitId, BufferedImage base) {
        int channel = bitId % 3;
        int pixelId = bitId / 3;
        int x = pixelId % base.getWidth();
        int y = pixelId / base.getWidth();
        return readBitFromPixel(x, y, channel, base);
    }

    private static boolean readBitFromPixel(int x, int y, int channel, BufferedImage base) {
        assert channel >= 0 && channel < 3;
        int channelValue = base.getRGB(x, y) >> (16 - 8 * channel);
        return (channelValue & 0x01) == 0x01;
    }

    static void storeByteInPixels(byte b, int byteId, BufferedImage base) {
        for (int i = 0; i < 8; i++) {
            storeBitInPixel((b & (1 << i)) != 0, byteId * 8 + i, base);
        }
    }

    private static void storeBitInPixel(boolean bit, int bitId, BufferedImage base) {
        int channel = bitId % 3;
        int pixelId = bitId / 3;
        int x = pixelId % base.getWidth();
        int y = pixelId / base.getWidth();
        storeBitInPixel(bit, x, y, channel, base);
    }

    private static void storeBitInPixel(boolean bit, int x, int y, int channel, BufferedImage base) {
        assert channel >= 0 && channel < 3;
        int shift           = 16 - 8 * channel;
        int oldPixel        = base.getRGB(x, y);
        int oldChannelValue = oldPixel >> shift;
        int newChannelValue = bit ? (oldChannelValue | 0x01) : (oldChannelValue & 0xFE);
        int newPixel        = (oldPixel & ~(0xFF << shift)) | (newChannelValue << shift);
        base.setRGB(x, y, newPixel);
    }
}
