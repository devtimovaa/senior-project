package com.example.seniorproject;

import com.example.seniorproject.model.algorithm.*;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

// Tests for all three steganography algorithms: LSB, Randomized LSB and Josephus LSB 3-3-2
class AlgorithmTest {

    //Embeds a text message and checks that extraction returns the exact same bytes
    @Test
    void lsb_embedAndExtract_textMessage() {
        BufferedImage cover = createTestImage(20, 20);
        byte[] secret = "Hello, Steganography!".getBytes(StandardCharsets.UTF_8);
        LSBAlgorithm alg = new LSBAlgorithm();

        BufferedImage stego = alg.embed(cover, secret);
        byte[] extracted = alg.extract(stego);

        assertArrayEquals(secret, extracted);
    }

    //Embedding an empty payload should still work and extract to length 0
    @Test
    void lsb_embedAndExtract_emptyPayload() {
        BufferedImage cover = createTestImage(10, 10);
        LSBAlgorithm alg = new LSBAlgorithm();

        BufferedImage stego = alg.embed(cover, new byte[0]);
        byte[] extracted = alg.extract(stego);

        assertEquals(0, extracted.length);
    }

    //Embeds every possible byte value to make sure no value gets corrupted by bit manipulation
    @Test
    void lsb_embedAndExtract_allByteValues() {
        BufferedImage cover = createTestImage(50, 50);
        byte[] secret = new byte[256];
        for (int i = 0; i < 256; i++) secret[i] = (byte) i;
        LSBAlgorithm alg = new LSBAlgorithm();

        BufferedImage stego = alg.embed(cover, secret);
        byte[] extracted = alg.extract(stego);

        assertArrayEquals(secret, extracted);
    }

    //Fills the image almost to its max capacity
    @Test
    void lsb_embedAndExtract_maxCapacity() {
        BufferedImage cover = createTestImage(20, 20);
        int maxPayload = (cover.getWidth() * cover.getHeight() * 3) / 8 - 5;
        byte[] secret = new byte[maxPayload];
        new Random(99).nextBytes(secret);
        LSBAlgorithm alg = new LSBAlgorithm();

        BufferedImage stego = alg.embed(cover, secret);
        byte[] extracted = alg.extract(stego);

        assertArrayEquals(secret, extracted);
    }

    //1x1 image can't hold 100 bytes - the algorithm should reject it
    @Test
    void lsb_embed_imageTooSmall_throws() {
        BufferedImage tiny = createTestImage(1, 1);
        LSBAlgorithm alg = new LSBAlgorithm();

        assertThrows(IllegalArgumentException.class, () -> alg.embed(tiny, new byte[100]));
    }

    //Extracting from an image that was never embedded into should fail (no valid magic bytes found)
    @Test
    void lsb_extract_cleanImage_throws() {
        BufferedImage clean = createTestImage(10, 10);
        LSBAlgorithm alg = new LSBAlgorithm();

        assertThrows(IllegalStateException.class, () -> alg.extract(clean));
    }

    //The embed method should return a NEW image - the original cover must stay untouched
    @Test
    void lsb_embed_doesNotMutateOriginal() {
        BufferedImage cover = createTestImage(10, 10);
        int pixelBefore = cover.getRGB(0, 0);

        new LSBAlgorithm().embed(cover, "test".getBytes());

        assertEquals(pixelBefore, cover.getRGB(0, 0));
    }

    //Randomized LSB round-trip tests
    //Both embed and extract must use the same key to agree on pixel order
    @Test
    void randomized_embedAndExtract_textMessage() {
        BufferedImage cover = createTestImage(20, 20);
        byte[] secret = "Hidden with key!".getBytes(StandardCharsets.UTF_8);

        BufferedImage stego = new RandomizedLSBAlgorithm(42).embed(cover, secret);
        byte[] extracted = new RandomizedLSBAlgorithm(42).extract(stego);

        assertArrayEquals(secret, extracted);
    }

    @Test
    void randomized_embedAndExtract_allByteValues() {
        BufferedImage cover = createTestImage(50, 50);
        byte[] secret = new byte[256];
        for (int i = 0; i < 256; i++) secret[i] = (byte) i;

        BufferedImage stego = new RandomizedLSBAlgorithm(42).embed(cover, secret);
        byte[] extracted = new RandomizedLSBAlgorithm(42).extract(stego);

        assertArrayEquals(secret, extracted);
    }

    //Using a different key during extraction should fail because the pixel order won't match
    @Test
    void randomized_wrongKey_throws() {
        BufferedImage cover = createTestImage(20, 20);
        byte[] secret = "secret message".getBytes(StandardCharsets.UTF_8);

        BufferedImage stego = new RandomizedLSBAlgorithm(42).embed(cover, secret);

        assertThrows(IllegalStateException.class,
                () -> new RandomizedLSBAlgorithm(99).extract(stego));
    }

    //Embedding the same payload with two different keys should produce different stego images
    @Test
    void randomized_differentKeys_produceDifferentOutput() {
        BufferedImage cover = createTestImage(20, 20);
        byte[] secret = "same payload".getBytes(StandardCharsets.UTF_8);

        BufferedImage stego1 = new RandomizedLSBAlgorithm(1).embed(cover, secret);
        BufferedImage stego2 = new RandomizedLSBAlgorithm(2).embed(cover, secret);

        boolean anyDiff = false;
        for (int y = 0; y < stego1.getHeight() && !anyDiff; y++)
            for (int x = 0; x < stego1.getWidth() && !anyDiff; x++)
                if (stego1.getRGB(x, y) != stego2.getRGB(x, y)) anyDiff = true;

        assertTrue(anyDiff, "Different keys should scatter data differently");
    }

    //Josephus LSB 3-3-2 round-trip tests
    @Test
    void josephus_embedAndExtract_textMessage() {
        BufferedImage cover = createTestImage(20, 20);
        byte[] secret = "Josephus chaos!".getBytes(StandardCharsets.UTF_8);

        BufferedImage stego = new JosephusLSB332Algorithm(42).embed(cover, secret);
        byte[] extracted = new JosephusLSB332Algorithm(42).extract(stego);

        assertArrayEquals(secret, extracted);
    }

    @Test
    void josephus_embedAndExtract_allByteValues() {
        BufferedImage cover = createTestImage(50, 50);
        byte[] secret = new byte[256];
        for (int i = 0; i < 256; i++) secret[i] = (byte) i;

        BufferedImage stego = new JosephusLSB332Algorithm(42).embed(cover, secret);
        byte[] extracted = new JosephusLSB332Algorithm(42).extract(stego);

        assertArrayEquals(secret, extracted);
    }

    //Wrong key means wrong permutation, extraction should fail
    @Test
    void josephus_wrongKey_throws() {
        BufferedImage cover = createTestImage(20, 20);
        byte[] secret = "secret message".getBytes(StandardCharsets.UTF_8);

        BufferedImage stego = new JosephusLSB332Algorithm(42).embed(cover, secret);

        assertThrows(IllegalStateException.class,
                () -> new JosephusLSB332Algorithm(99).extract(stego));
    }

    //Two different keys should scatter the same payload to different pixel locations
    @Test
    void josephus_differentKeys_produceDifferentOutput() {
        BufferedImage cover = createTestImage(20, 20);
        byte[] secret = "same payload".getBytes(StandardCharsets.UTF_8);

        BufferedImage stego1 = new JosephusLSB332Algorithm(1).embed(cover, secret);
        BufferedImage stego2 = new JosephusLSB332Algorithm(2).embed(cover, secret);

        boolean anyDiff = false;
        for (int y = 0; y < stego1.getHeight() && !anyDiff; y++)
            for (int x = 0; x < stego1.getWidth() && !anyDiff; x++)
                if (stego1.getRGB(x, y) != stego2.getRGB(x, y)) anyDiff = true;

        assertTrue(anyDiff, "Different keys should scatter data differently");
    }

    //Creates a synthetic ARGB image (seed = 42) - every test run produces the same image, so results are reproducible
    static BufferedImage createTestImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Random rng = new Random(42);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                img.setRGB(x, y, 0xFF000000 | rng.nextInt(0xFFFFFF)); // full alpha + random RGB
        return img;
    }
}
