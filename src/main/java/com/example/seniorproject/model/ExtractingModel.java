package com.example.seniorproject.model;

import java.awt.image.BufferedImage;

public class ExtractingModel {

    public byte[] extract(BufferedImage image, String algorithm, int seed) {
        if ("LSB".equals(algorithm)) {
            return new LSBAlgorithm().extract(image);
        } else if ("Randomized LSB".equals(algorithm)) {
            return new RandomizedLSBAlgorithm(seed).extract(image);
        } else if ("Josephus LSB 3-3-2".equals(algorithm)) {
            return new JosephusLSB332Algorithm(seed).extract(image);
        }
        return new byte[0];
    }

    // Detect if text or image was hidden
    public static boolean isPngBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 8) return false;
        return (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50  
                && bytes[2] == 0x4E  
                && bytes[3] == 0x47  
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }
}
