package com.example.seniorproject.algorithms;

import java.awt.image.BufferedImage;

public interface SteganographyAlgorithm {

    BufferedImage embed(BufferedImage base, byte[] secret);

    byte[] extract(BufferedImage base);
}
