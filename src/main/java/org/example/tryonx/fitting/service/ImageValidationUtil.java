package org.example.tryonx.fitting.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class ImageValidationUtil {
    public static final double DEFAULT_WEIGHT_EDGE = 0.6;
    public static final double DEFAULT_WEIGHT_NONWHITE = 0.4;
    public static final double DEFAULT_SKIN_MAX_FRACTION = 0.45;
    public static final double DEFAULT_SKIN_PENALTY = 0.8;
    public static final double DEFAULT_DECISION_THRESHOLD = 0.18;

    public static Map<String, Object> analyze(byte[] bytes) {
        Map<String, Object> out = new HashMap<>();
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                out.put("error", "invalid_image");
                return out;
            }
            double nonwhite = computeNonWhiteFraction(img);
            double edge = computeEdgeDensity(img);
            double skin = computeSkinFraction(img);

            double base = DEFAULT_WEIGHT_EDGE * edge + DEFAULT_WEIGHT_NONWHITE * nonwhite;
            double ratio = Math.min(1.0, skin / DEFAULT_SKIN_MAX_FRACTION);
            double penalty = ratio * DEFAULT_SKIN_PENALTY;
            double confidence = base * (1.0 - penalty);
            confidence = Math.max(0.0, Math.min(1.0, confidence));
            boolean isClothing = confidence >= DEFAULT_DECISION_THRESHOLD;

            out.put("isClothing", isClothing);
            out.put("confidence", confidence);
            Map<String, Object> extras = new HashMap<>();
            extras.put("edge_density", edge);
            extras.put("nonwhite_fraction", nonwhite);
            extras.put("skin_fraction", skin);
            out.put("extras", extras);
            return out;
        } catch (Exception e) {
            out.put("error", "analysis_failed");
            out.put("message", e.getMessage());
            return out;
        }
    }

    private static double computeNonWhiteFraction(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        long count = 0; long total = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                double lum = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
                if (lum < 0.95) count++;
                total++;
            }
        }
        return total > 0 ? (double) count / (double) total : 0.0;
    }

    private static double computeEdgeDensity(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        double[][] gray = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                gray[y][x] = (0.2126 * r + 0.7152 * g + 0.0722 * b);
            }
        }
        int[][] kx = {{-1,0,1},{-2,0,2},{-1,0,1}};
        int[][] ky = {{-1,-2,-1},{0,0,0},{1,2,1}};
        int count = 0;
        double maxMag = 1e-9;
        double[][] mag = new double[h][w];
        for (int y = 1; y < h-1; y++) {
            for (int x = 1; x < w-1; x++) {
                double gx = 0.0, gy = 0.0;
                for (int j = -1; j <= 1; j++) {
                    for (int i = -1; i <= 1; i++) {
                        gx += kx[j+1][i+1] * gray[y+j][x+i];
                        gy += ky[j+1][i+1] * gray[y+j][x+i];
                    }
                }
                double g = Math.hypot(gx, gy);
                mag[y][x] = g;
                if (g > maxMag) maxMag = g;
                count++;
            }
        }
        if (count == 0) return 0.0;
        int strong = 0;
        for (int y = 1; y < h-1; y++) {
            for (int x = 1; x < w-1; x++) {
                double v = mag[y][x] / maxMag;
                if (v > 0.12) strong++;
            }
        }
        return (double) strong / (double) count;
    }

    private static double computeSkinFraction(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        long skinCount = 0; long total = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                double Y  =  0.299 * r + 0.587 * g + 0.114 * b;
                double Cb = 128.0 - 0.168736 * r - 0.331264 * g + 0.5 * b;
                double Cr = 128.0 + 0.5 * r - 0.418688 * g - 0.081312 * b;
                if (Cb >= 77 && Cb <= 127 && Cr >= 133 && Cr <= 173 && Y > 20) {
                    skinCount++;
                }
                total++;
            }
        }
        return total > 0 ? (double) skinCount / (double) total : 0.0;
    }
}
