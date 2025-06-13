package org.csstudio.trends.databrowser3.ui.waveformoverlapview;

import javafx.scene.paint.Color;

/**
 * Jet colormap utility class that maps normalized values (0.0-1.0) to Jet color spectrum.
 * <p>
 * Key features:
 * - Precomputed color lookup table for optimal performance
 * - Automatic input range correction (0.0-1.0)
 * - Thread-safe static implementation
 * - Configurable table size (currently 1000 entries)
 * <p>
 * Jet colormap formula:
 * R = clamp(4x - 1.5)
 * G = clamp(4x - 0.5)
 * B = clamp(4x + 0.5)
 * where x ∈ [0, 1] and clamp(v) = max(0, min(1, v))
 */
public final class JetColorTable {
    // Color table parameters
    private static final int TABLE_SIZE = 1000;      // Number of precomputed colors
    private static final Color[] COLOR_TABLE = new Color[TABLE_SIZE];
    private static final double SCALE_FACTOR = 1.0 / (TABLE_SIZE - 1); // Normalization factor

    // Static initialization block for color table
    static {
        initializeColorTable();
    }

    // Prevent instantiation
    private JetColorTable() {
        throw new UnsupportedOperationException("Utility class, cannot be instantiated");
    }

    /**
     * Initializes the color lookup table during class loading.
     * Precomputes all 1000 color entries using the Jet colormap formula.
     */
    private static void initializeColorTable() {
        for (int i = 0; i < TABLE_SIZE; i++) {
            final double x = i * SCALE_FACTOR; // Normalized position [0.0, 1.0]
            COLOR_TABLE[i] = calculateJetColor(x);
        }
    }

    /**
     * Calculates a single Jet color component using the colormap formula.
     * Implements the mathematical formula for Jet color transformation:
     * R = 4x - 1.5 (clamped to [0,1])
     * G = 4x - 0.5 (clamped to [0,1])
     * B = 4x + 0.5 (clamped to [0,1])
     *
     * @param x Normalized input value (0.0-1.0)
     * @return Color object with calculated RGB values
     */
    private static Color calculateJetColor(double x) {
        // Calculate color components with clamping to valid [0,1] range
//        final double r = clamp(4.0 * x - 1.5);
//        final double g = clamp(4.0 * x - 0.5);
//        final double b = clamp(4.0 * x + 0.5);
        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;

        if (x < 0.25) {
            red = 0;
            green = 4 * x;
            blue = 1;
        } else if (x < 0.5) {
            red = 0;
            green = 1;
            blue = 1 - 4 * (x - 0.25);
        } else if (x < 0.75) {
            red = 4 * (x - 0.5);
            green = 1;
            blue = 0;
        } else {
            red = 1;
            green = 1 - 4 * (x - 0.75);
            blue = 0;
        }

        return Color.color(red, green, blue);
//        return Color.color(r, g, b);
    }

    /**
     * Ensures input value stays within [0.0, 1.0] range.
     *
     * @param value Input value to clamp
     * @return Clamped value between 0.0 and 1.0
     */
    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * Main entry point to get Jet color from normalized value.
     *
     * @param fraction Normalized input value (0.0-1.0). Values outside this range
     *                 will be automatically clamped to the nearest valid value.
     * @return Corresponding Jet color from the precomputed table
     */
    public static Color getColor(double fraction) {
        // Normalize input and calculate table index
        final int index = (int) Math.round(
                Math.max(0.0, Math.min(1.0, fraction)) * (TABLE_SIZE - 1)
        );
        return COLOR_TABLE[index];
    }

    /**
     * Returns the size of the color lookup table.
     *
     * @return Number of precomputed colors in the table
     */
    public static int getTableSize() {
        return TABLE_SIZE;
    }
}