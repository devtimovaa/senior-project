package com.example.seniorproject.model.algorithm;

import java.awt.image.BufferedImage;

//Common for all steganography algorithms
public interface SteganographyAlgorithm {

    BufferedImage embed(BufferedImage coverImage, byte[] secret);

    byte[] extract(BufferedImage stegoImage);
}
