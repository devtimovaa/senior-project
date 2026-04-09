package com.example.seniorproject.model;

import java.awt.image.BufferedImage;

public class EmbeddingModel {

    public BufferedImage embed(BufferedImage coverImage, byte[] data, String algorithm, int seed) {
        if ("LSB".equals(algorithm)) {
            return new LSBAlgorithm().embed(coverImage, data);
        } else if ("Randomized LSB".equals(algorithm)) {
            return new RandomizedLSBAlgorithm(seed).embed(coverImage, data);
        } else if ("Josephus LSB 3-3-2".equals(algorithm)) {
            return new JosephusLSB332Algorithm(seed).embed(coverImage, data);
        }
        return coverImage;
    }
}
