package com.example.seniorproject.model;

import com.example.seniorproject.model.algorithm.JosephusLSB332Algorithm;
import com.example.seniorproject.model.algorithm.LSBAlgorithm;
import com.example.seniorproject.model.algorithm.RandomizedLSBAlgorithm;

import java.awt.image.BufferedImage;

//Extraction to the chosen steganography algorithm
public class ExtractingModel {

    public byte[] extract(BufferedImage stegoImage, String algorithm, int key) {
        if ("LSB".equals(algorithm)) {
            return new LSBAlgorithm().extract(stegoImage);
        } else if ("Randomized LSB".equals(algorithm)) {
            return new RandomizedLSBAlgorithm(key).extract(stegoImage);
        } else if ("Josephus LSB 3-3-2".equals(algorithm)) {
            return new JosephusLSB332Algorithm(key).extract(stegoImage);
        }
        throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
    }

    //Checks if the extracted bytes are a PNG file based on the magic header
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
