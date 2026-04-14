package com.example.seniorproject.model;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static com.example.seniorproject.model.LSBMethods.checksum;

/*
 Josephus-permutation LSB steganography with 3-3-2 bit encoding
 Pixel locations are chosen via a chaotic logistic map (r=3.87) combined with  a Josephus elimination scheme
 Each pixel stores a full byte: 3 bits in Red, 3 in Green, 2 in Blue
*/

public class JosephusLSB332Algorithm implements SteganographyAlgorithm {

    private static final byte MAGIC_0 = (byte) 0xEF;
    private static final byte MAGIC_1 = (byte) 0xBE;
    private static final int MAGIC_BYTES = 2;
    private static final int HEADER_BYTES = 4;
    private static final int CHECKSUM_BYTES = 1;
    private static final double GROWTH_RATE = 3.87;

    private final int key;

    public JosephusLSB332Algorithm(int key) {
        this.key = key;
    }

    //Embeds secret data by scattering it chaotically in chosen pixel locations
    @Override
    public BufferedImage embed(BufferedImage coverImage, byte[] secret) {
        byte[] payload = secret == null ? new byte[0] : secret;

        int pixelCount = coverImage.getWidth() * coverImage.getHeight();
        int slotsNeeded = MAGIC_BYTES + HEADER_BYTES + payload.length + CHECKSUM_BYTES;

        if (slotsNeeded > pixelCount) {
            throw new IllegalArgumentException("Cover image is too small to embed this data");
        }

        List<Integer> availablePixels = buildPixelPool(pixelCount);
        double[] chaos = initChaoticState();

        //Work on a copy so the cover image stays untouched
        int w = coverImage.getWidth();
        int h = coverImage.getHeight();
        BufferedImage stegoImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        stegoImage.setRGB(0, 0, w, h, coverImage.getRGB(0, 0, w, h, null, 0, w), 0, w);

        //Magic bytes let us verify the correct key was used during extraction
        embedByte332(MAGIC_0, nextLocation(availablePixels, chaos), stegoImage);
        embedByte332(MAGIC_1, nextLocation(availablePixels, chaos), stegoImage);

        // Payload length split into 4 bytes, big-endian
        for (int i = 0; i < HEADER_BYTES; i++) {
            int shift = 24 - 8 * i;
            embedByte332((byte) ((payload.length >> shift) & 0xFF), nextLocation(availablePixels, chaos), stegoImage);
        }

        for (byte b : payload) {
            embedByte332(b, nextLocation(availablePixels, chaos), stegoImage);
        }

        embedByte332(checksum(payload), nextLocation(availablePixels, chaos), stegoImage);

        return stegoImage;
    }

    //Extracts hidden data by regenerating the same chaotic pixel sequence
    @Override
    public byte[] extract(BufferedImage stegoImage) {
        int pixelCount = stegoImage.getWidth() * stegoImage.getHeight();

        List<Integer> availablePixels = buildPixelPool(pixelCount);
        double[] chaos = initChaoticState();

        //If magic bytes don't match, either no data or wrong key
        byte m0 = extractByte332(nextLocation(availablePixels, chaos), stegoImage);
        byte m1 = extractByte332(nextLocation(availablePixels, chaos), stegoImage);

        if (m0 != MAGIC_0 || m1 != MAGIC_1) {
            throw new IllegalStateException("No hidden message found in this image or the key used was wrong");
        }

        // Reassemble the 4-byte big-endian header into an int
        int dataLen = 0;
        for (int i = 0; i < HEADER_BYTES; i++) {
            dataLen = (dataLen << 8) | (0xFF & extractByte332(nextLocation(availablePixels, chaos), stegoImage));
        }

        int maxLen = pixelCount - MAGIC_BYTES - HEADER_BYTES - CHECKSUM_BYTES;
        if (dataLen < 0 || dataLen > maxLen) {
            throw new IllegalStateException("Could not read message - did you use the right key?");
        }

        byte[] payload = new byte[dataLen];
        for (int i = 0; i < dataLen; i++) {
            payload[i] = extractByte332(nextLocation(availablePixels, chaos), stegoImage);
        }

        // Verify integrity
        byte computed = checksum(payload);
        byte stored = extractByte332(nextLocation(availablePixels, chaos), stegoImage);
        if (computed != stored) {
            throw new IllegalStateException("Checksum mismatch: data may be corrupted or wrong key used");
        }

        return payload;
    }

    //Creates a pool of all pixels for Josephus elimination 
    private List<Integer> buildPixelPool(int pixelCount) {
        List<Integer> pool = new ArrayList<>(pixelCount);
        for (int i = 0; i < pixelCount; i++) pool.add(i);
        return pool;
    }

    //Initialises and iterates the logistic map 100 times
    private double[] initChaoticState() {
        double[] chaos = {(key % 9999 + 1)/10000.0 };
        for (int i = 0; i < 100; i++) {
            chaos[0] = GROWTH_RATE * chaos[0] * (1 - chaos[0]);
        }
        return chaos;
    }

    //Josephus elimination priciple is that it picks a pixel from the pool using the chaotic value, then swap-removes it so it can't be chosen again
    private int nextLocation(List<Integer> availablePixels, double[] chaos) {
        chaos[0] = GROWTH_RATE * chaos[0] * (1 - chaos[0]);
        int position = Math.floorMod((int) (chaos[0] * 100_000_000), availablePixels.size());
        int lastIndex  = availablePixels.size() - 1;
        int pixelIndex = availablePixels.get(position);
        availablePixels.set(position, availablePixels.get(lastIndex));
        availablePixels.remove(lastIndex);
        return pixelIndex;
    }

    //Stores one byte in a single pixel using 3-3-2 encoding - 3 bits in Red, 3 bits in Green, 2 bits in Blue
    private void embedByte332(byte secretByte, int pixelIndex, BufferedImage img) {
        int val = secretByte & 0xFF;
        int x = pixelIndex % img.getWidth();
        int y = pixelIndex / img.getWidth();
        int rgb = img.getRGB(x, y);
        int alpha = (rgb >> 24) & 0xFF;
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        //Replace the lowest 3, 3 and 2 bits of each channel with secret bits
        red = (red & 0xF8) | ((val >> 5) & 0x07);
        green = (green & 0xF8) | ((val >> 2) & 0x07);
        blue = (blue & 0xFC) | ( val & 0x03);

        img.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
    }

    //Reads back the byte from a single pixel 
    private byte extractByte332(int pixelIndex, BufferedImage img) {
        int x = pixelIndex % img.getWidth();
        int y = pixelIndex / img.getWidth();
        int rgb = img.getRGB(x, y);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        return (byte) (((red & 0x07) << 5) | ((green & 0x07) << 2) | (blue & 0x03));
    }

}
