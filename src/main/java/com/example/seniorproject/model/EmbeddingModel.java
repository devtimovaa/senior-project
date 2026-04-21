package com.example.seniorproject.model;

import com.example.seniorproject.model.algorithm.JosephusLSB332Algorithm;
import com.example.seniorproject.model.algorithm.LSBAlgorithm;
import com.example.seniorproject.model.algorithm.RandomizedLSBAlgorithm;

import java.awt.image.BufferedImage;

//Embedding to the chosen steganography algorithm
public class EmbeddingModel {

    public BufferedImage embed(BufferedImage coverImage, byte[] secret, String algorithm, int key) {
        if ("LSB".equals(algorithm)) {
            return new LSBAlgorithm().embed(coverImage, secret);
        } else if ("Randomized LSB".equals(algorithm)) {
            return new RandomizedLSBAlgorithm(key).embed(coverImage, secret);
        } else if ("Josephus LSB 3-3-2".equals(algorithm)) {
            return new JosephusLSB332Algorithm(key).embed(coverImage, secret);
        }
        throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
    }
}
