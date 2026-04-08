package com.example.seniorproject.algorithms;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class JosephusLSB332Algorithm implements SteganographyAlgorithm {

    private static final byte MAGIC_0 = (byte) 0xEF;
    private static final byte MAGIC_1 = (byte) 0xBE;
    private static final int MAGIC_BYTES    = 2;
    private static final int HEADER_BYTES   = 4;
    private static final int CHECKSUM_BYTES = 1;
    private static final double GROWTH_RATE = 3.87;

    private final int seed;
    private double chaoticValue;
    private int coverSize;

    public JosephusLSB332Algorithm(int seed) {
        this.seed = seed;
    }

    @Override
    public BufferedImage embed(BufferedImage coverImage, byte[] secretData) {
        if (secretData == null) secretData = new byte[0];

        int pixelCount  = coverImage.getWidth() * coverImage.getHeight();
        int slotsNeeded = MAGIC_BYTES + HEADER_BYTES + secretData.length + CHECKSUM_BYTES;

        if (slotsNeeded > pixelCount) {
            throw new IllegalArgumentException("Cover image is too small to embed this data");
        }

        coverSize = pixelCount;
        List<Integer> availablePixels = buildPixelPool(pixelCount);
        warmUpChaoticState();

        int w = coverImage.getWidth();
        int h = coverImage.getHeight();
        BufferedImage stegoImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        stegoImage.setRGB(0, 0, w, h, coverImage.getRGB(0, 0, w, h, null, 0, w), 0, w);

        embedByte332(MAGIC_0, nextLocation(availablePixels), stegoImage);
        embedByte332(MAGIC_1, nextLocation(availablePixels), stegoImage);

        embedByte332((byte) ((secretData.length >> 24) & 0xFF), nextLocation(availablePixels), stegoImage);
        embedByte332((byte) ((secretData.length >> 16) & 0xFF), nextLocation(availablePixels), stegoImage);
        embedByte332((byte) ((secretData.length >>  8) & 0xFF), nextLocation(availablePixels), stegoImage);
        embedByte332((byte)  (secretData.length        & 0xFF), nextLocation(availablePixels), stegoImage);

        for (byte secretByte : secretData) {
            embedByte332(secretByte, nextLocation(availablePixels), stegoImage);
        }

        embedByte332(checksum(secretData), nextLocation(availablePixels), stegoImage);

        return stegoImage;
    }

    @Override
    public byte[] extract(BufferedImage stegoImage) {
        int pixelCount = stegoImage.getWidth() * stegoImage.getHeight();
        coverSize = pixelCount;

        List<Integer> availablePixels = buildPixelPool(pixelCount);
        warmUpChaoticState();

        byte m0 = extractByte332(nextLocation(availablePixels), stegoImage);
        byte m1 = extractByte332(nextLocation(availablePixels), stegoImage);

        if (m0 != MAGIC_0 || m1 != MAGIC_1) {
            throw new IllegalStateException("No hidden message found in this image");
        }

        byte b1 = extractByte332(nextLocation(availablePixels), stegoImage);
        byte b2 = extractByte332(nextLocation(availablePixels), stegoImage);
        byte b3 = extractByte332(nextLocation(availablePixels), stegoImage);
        byte b4 = extractByte332(nextLocation(availablePixels), stegoImage);

        int secretLength = ((0xFF & b1) << 24) | ((0xFF & b2) << 16)
                         | ((0xFF & b3) <<  8) |  (0xFF & b4);

        int maxLength = pixelCount - MAGIC_BYTES - HEADER_BYTES - CHECKSUM_BYTES;
        if (secretLength < 0 || secretLength > maxLength) {
            throw new IllegalStateException("Could not read message - did you use the right key?");
        }

        byte[] secretData = new byte[secretLength];
        for (int i = 0; i < secretLength; i++) {
            secretData[i] = extractByte332(nextLocation(availablePixels), stegoImage);
        }

        byte storedChecksum = extractByte332(nextLocation(availablePixels), stegoImage);
        if (checksum(secretData) != storedChecksum) {
            throw new RuntimeException("Checksum mismatch - message may be corrupted or wrong key used");
        }

        return secretData;
    }

    private List<Integer> buildPixelPool(int pixelCount) {
        List<Integer> pool = new ArrayList<>(pixelCount);
        for (int i = 0; i < pixelCount; i++) pool.add(i);
        return pool;
    }

    private void warmUpChaoticState() {
        chaoticValue = (seed % 9999 + 1) / 10000.0;
        for (int i = 0; i < 100; i++) {
            chaoticValue = GROWTH_RATE * chaoticValue * (1 - chaoticValue);
        }
    }

    private int nextLocation(List<Integer> availablePixels) {
        chaoticValue = GROWTH_RATE * chaoticValue * (1 - chaoticValue);
        int position   = (int) (chaoticValue * coverSize) % availablePixels.size();
        int lastIndex  = availablePixels.size() - 1;
        int pixelIndex = availablePixels.get(position);
        availablePixels.set(position, availablePixels.get(lastIndex));
        availablePixels.remove(lastIndex);
        return pixelIndex;
    }

    private void embedByte332(byte secretByte, int pixelIndex, BufferedImage img) {
        int val   = secretByte & 0xFF;
        int x     = pixelIndex % img.getWidth();
        int y     = pixelIndex / img.getWidth();
        int rgb   = img.getRGB(x, y);
        int alpha = (rgb >> 24) & 0xFF;
        int red   = (rgb >> 16) & 0xFF;
        int green = (rgb >>  8) & 0xFF;
        int blue  =  rgb        & 0xFF;

        red   = (red   & 0xF8) | ((val >> 5) & 0x07);
        green = (green & 0xF8) | ((val >> 2) & 0x07);
        blue  = (blue  & 0xFC) | ( val       & 0x03);

        img.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
    }

    private byte extractByte332(int pixelIndex, BufferedImage img) {
        int x     = pixelIndex % img.getWidth();
        int y     = pixelIndex / img.getWidth();
        int rgb   = img.getRGB(x, y);
        int red   = (rgb >> 16) & 0xFF;
        int green = (rgb >>  8) & 0xFF;
        int blue  =  rgb        & 0xFF;

        return (byte) (((red & 0x07) << 5) | ((green & 0x07) << 2) | (blue & 0x03));
    }

    private static byte checksum(byte[] data) {
        byte xor = 0;
        for (byte b : data) xor ^= b;
        return xor;
    }
}
