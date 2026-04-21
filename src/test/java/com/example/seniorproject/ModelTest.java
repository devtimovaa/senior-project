package com.example.seniorproject;

import com.example.seniorproject.model.*;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

import static com.example.seniorproject.AlgorithmTest.createTestImage;
import static org.junit.jupiter.api.Assertions.*;

// Tests for the Model layer: algorithm routing (EmbeddingModel / ExtractingModel),
// PNG header detection, and all AnalyzingModel metrics (X-ray, heatmap, MSE, PSNR, formatBytes).
class ModelTest {

    // Shared model instances — they are stateless so one per class is fine
    private final EmbeddingModel embedder = new EmbeddingModel();
    private final ExtractingModel extractor = new ExtractingModel();
    private final AnalyzingModel analyzer = new AnalyzingModel();

    // --- Algorithm routing ---
    // The model selects the correct algorithm based on a string name ("LSB", "Randomized LSB", etc.)
    // These tests verify that embed → extract round-trips work through the routing layer.

    @Test
    void routing_lsb() {
        byte[] secret = "LSB test".getBytes(StandardCharsets.UTF_8);

        BufferedImage stego = embedder.embed(createTestImage(20, 20), secret, "LSB", 0);
        byte[] extracted = extractor.extract(stego, "LSB", 0);

        assertArrayEquals(secret, extracted);
    }

    @Test
    void routing_randomizedLsb() {
        byte[] secret = "Randomized test".getBytes(StandardCharsets.UTF_8);

        BufferedImage stego = embedder.embed(createTestImage(20, 20), secret, "Randomized LSB", 42);
        byte[] extracted = extractor.extract(stego, "Randomized LSB", 42);

        assertArrayEquals(secret, extracted);
    }

    @Test
    void routing_josephusLsb332() {
        byte[] secret = "Josephus test".getBytes(StandardCharsets.UTF_8);

        BufferedImage stego = embedder.embed(createTestImage(20, 20), secret, "Josephus LSB 3-3-2", 42);
        byte[] extracted = extractor.extract(stego, "Josephus LSB 3-3-2", 42);

        assertArrayEquals(secret, extracted);
    }

    // Passing an algorithm name the model doesn't recognise should throw immediately
    @Test
    void routing_unknownAlgorithm_throws() {
        BufferedImage img = createTestImage(10, 10);

        assertThrows(IllegalArgumentException.class,
                () -> embedder.embed(img, new byte[]{1}, "INVALID", 0));
        assertThrows(IllegalArgumentException.class,
                () -> extractor.extract(img, "INVALID", 0));
    }

    // --- PNG detection ---
    // After extraction, the app checks if the extracted bytes start with the PNG magic header
    // to decide whether to display the result as an image or as text.

    @Test
    void isPngBytes_validHeader() {
        // standard 8-byte PNG signature followed by one extra byte
        byte[] pngHeader = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        assertTrue(ExtractingModel.isPngBytes(pngHeader));
    }

    // null, too-short arrays and arrays without the PNG signature should all return false
    @Test
    void isPngBytes_invalidCases() {
        assertFalse(ExtractingModel.isPngBytes(null));
        assertFalse(ExtractingModel.isPngBytes(new byte[]{0x50, 0x4E}));
        assertFalse(ExtractingModel.isPngBytes(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8}));
    }

    // --- LSB X-ray ---
    // The X-ray amplifies the least-significant bit of each channel to 0 or 255
    // so hidden data patterns become visible.

    @Test
    void lsbXray_knownPixel() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        // 0x52 = 0101_0010 → LSB 0, 0xA1 = 1010_0001 → LSB 1, 0xC0 = 1100_0000 → LSB 0
        img.setRGB(0, 0, 0xFF_52_A1_C0);

        BufferedImage result = analyzer.lsbXray(img);
        int rgb = result.getRGB(0, 0);

        assertEquals(0, (rgb >> 16) & 0xFF);
        assertEquals(255, (rgb >> 8) & 0xFF);
        assertEquals(0, rgb & 0xFF);
    }

    // --- Difference Heatmap ---
    // Compares two images pixel-by-pixel and counts/visualises where they differ.

    // Two identical images should produce zero modified pixels
    @Test
    void heatmap_identicalImages_zeroModified() {
        BufferedImage img = createTestImage(10, 10);
        BufferedImage copy = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        copy.setRGB(0, 0, 10, 10, img.getRGB(0, 0, 10, 10, null, 0, 10), 0, 10);

        AnalyzingModel.HeatmapResult result = analyzer.differenceHeatmap(img, copy);

        assertEquals(0, result.modifiedPixels());
    }

    // Changing just the red channel of one pixel by 1 should register as exactly 1 modified pixel
    @Test
    void heatmap_singlePixelDiff_countsOne() {
        BufferedImage a = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        BufferedImage b = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        int base = 0xFF_808080;
        for (int y = 0; y < 2; y++)
            for (int x = 0; x < 2; x++) {
                a.setRGB(x, y, base);
                b.setRGB(x, y, base);
            }
        b.setRGB(0, 0, 0xFF_818080); // R channel differs by 1

        AnalyzingModel.HeatmapResult result = analyzer.differenceHeatmap(a, b);

        assertEquals(1, result.modifiedPixels());
    }

    // --- MSE (Mean Squared Error) ---
    // MSE measures the average squared difference per channel between two images.
    // Lower MSE = less distortion introduced by steganography.

    @Test
    void mse_identicalImages_returnsZero() {
        BufferedImage img = createTestImage(10, 10);

        double mse = analyzer.calculateMse(img, img);

        assertEquals(0.0, mse);
    }

    // Hand-calculated case: only red differs by 3 → MSE = 3² / (1 px × 3 ch) = 3.0
    @Test
    void mse_knownDifference() {
        BufferedImage a = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        BufferedImage b = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        a.setRGB(0, 0, 0xFF_000000);
        b.setRGB(0, 0, 0xFF_030000); // R differs by 3

        double mse = analyzer.calculateMse(a, b);

        // (3^2 + 0 + 0) / (1 pixel * 3 channels) = 9 / 3 = 3.0
        assertEquals(3.0, mse, 0.001);
    }

    // --- PSNR (Peak Signal-to-Noise Ratio) ---
    // PSNR is derived from MSE: PSNR = 10 * log10(255² / MSE).
    // Higher PSNR means less visible distortion; infinity when images are identical.

    @Test
    void psnr_zeroMse_returnsInfinity() {
        double psnr = analyzer.calculatePsnr(0.0);

        assertEquals(Double.POSITIVE_INFINITY, psnr);
    }

    // With MSE = 1.0 we can verify the formula gives 10 * log10(65025) ≈ 48.13 dB
    @Test
    void psnr_knownMse() {
        double psnr = analyzer.calculatePsnr(1.0);

        double expected = 10.0 * Math.log10(255.0 * 255.0 / 1.0);
        assertEquals(expected, psnr, 0.01);
    }

    // --- formatBytes ---
    // Utility that converts raw byte counts into human-readable strings (B, KB, MB)

    @Test
    void formatBytes_bytes() {
        assertEquals("500 B", analyzer.formatBytes(500));
    }

    @Test
    void formatBytes_kilobytes() {
        assertEquals("2.00 KB", analyzer.formatBytes(2048));
    }

    @Test
    void formatBytes_megabytes() {
        assertEquals("2.00 MB", analyzer.formatBytes(2 * 1024 * 1024));
    }
}
