package com.example.seniorproject.model;

import java.awt.image.BufferedImage;

public interface SteganographyAlgorithm {

    BufferedImage embed(BufferedImage coverImage, byte[] secret);

    byte[] extract(BufferedImage stegoImage);
}
