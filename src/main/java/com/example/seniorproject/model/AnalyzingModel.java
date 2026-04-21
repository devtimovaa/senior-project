package com.example.seniorproject.model;

import java.awt.image.BufferedImage;

//Image analysis 
public class AnalyzingModel {

    private static final int HEATMAP_AMPLIFICATION = 50;

    public record HeatmapResult(BufferedImage image, int modifiedPixels) {}

    //LSB of each channel to full brightness
    public BufferedImage lsbXray(BufferedImage source) {
        int w = source.getWidth(), h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = source.getRGB(x, y);
                int r = ((rgb >> 16) & 0x01) * 255;
                int g = ((rgb >>  8) & 0x01) * 255;
                int b = (rgb & 0x01) * 255;
                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    //Highlights pixel differences between the original and stego images
    public HeatmapResult differenceHeatmap(BufferedImage original, BufferedImage stego) {
        int w = stego.getWidth(), h = stego.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int modified = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int oRgb = original.getRGB(x, y);
                int sRgb = stego.getRGB(x, y);
                int r = Math.min(Math.abs(((oRgb >> 16) & 0xFF) - ((sRgb >> 16) & 0xFF)) * HEATMAP_AMPLIFICATION, 255);
                int g = Math.min(Math.abs(((oRgb >> 8) & 0xFF) - ((sRgb >> 8) & 0xFF)) * HEATMAP_AMPLIFICATION, 255);
                int b = Math.min(Math.abs((oRgb & 0xFF) - (sRgb & 0xFF)) * HEATMAP_AMPLIFICATION, 255);
                if (r != 0 || g != 0 || b != 0) modified++;
                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return new HeatmapResult(out, modified);
    }

    //MSE - average squared difference per channel across all pixels
    public double calculateMse(BufferedImage original, BufferedImage stego) {
        int w = original.getWidth(), h = original.getHeight();
        long mseSum = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int oRgb = original.getRGB(x, y);
                int sRgb = stego.getRGB(x, y);
                for (int shift : new int[]{16, 8, 0}) {
                    int diff = ((oRgb >> shift) & 0xFF) - ((sRgb >> shift) & 0xFF);
                    mseSum += (long) diff * diff;
                }
            }
        }
        return mseSum / (double)(w * h * 3);
    }

    //PSNR - measures how close the stego image is to the original
    public double calculatePsnr(double mse) {
        if (mse == 0) return Double.POSITIVE_INFINITY;
        return 10.0 * Math.log10(255.0 * 255.0 / mse);
    }

    //Converts a byte count into a readable string
    public String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }
}
