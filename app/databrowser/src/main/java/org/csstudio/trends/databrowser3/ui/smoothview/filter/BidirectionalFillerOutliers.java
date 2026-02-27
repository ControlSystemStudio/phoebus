package org.csstudio.trends.databrowser3.ui.smoothview.filter;

import java.util.Arrays;

/**
 * Implements a bidirectional outlier filling algorithm that replaces outliers
 * with nearest valid values using both forward and backward interpolation.
 *
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */
public class BidirectionalFillerOutliers {

    /**
     * Fills outliers in the data array using bidirectional nearest valid value interpolation.
     *
     * <p>This method first performs forward filling (left to right) to replace outliers
     * with the most recent valid value, then performs backward filling (right to left)
     * to refine the results using subsequent valid values.</p>
     *
     * @param data       The original data array containing potential outliers
     * @param lowerBound The lower bound for valid values
     * @param upperBound The upper bound for valid values
     * @return The filled data array with outliers replaced by nearest valid values
     */
    public static double[] fillOutliers(double[] data, double lowerBound, double upperBound) {
        if (data == null || data.length == 0) {
            return data;
        }

        double[] filledData = data.clone();
        int length = data.length;

        // Forward filling: replace outliers with previous valid values
        Double lastValid = null;
        for (int i = 0; i < length; i++) {
            if (isOutlier(data[i], lowerBound, upperBound)) {
                filledData[i] = lastValid != null ? lastValid : getFallbackValue(data);
            } else {
                lastValid = data[i];
            }
        }

        // Backward filling: replace forward-filled values with subsequent valid values
        lastValid = null;
        for (int i = length - 1; i >= 0; i--) {
            if (isOutlier(filledData[i], lowerBound, upperBound)) {
                if (lastValid != null) {
                    filledData[i] = lastValid; // Override forward-filled value with backward interpolation
                }
                // No action for the last element if it's an outlier (no subsequent values)
            } else {
                lastValid = filledData[i];
            }
        }

        return filledData;
    }

    /**
     * Checks if a value is an outlier based on the specified bounds.
     */
    private static boolean isOutlier(double value, double lowerBound, double upperBound) {
        return value < lowerBound || value > upperBound;
    }

    /**
     * Provides a fallback value when no valid values are available for interpolation.
     * Currently uses the array mean, but could be customized (e.g., median) for better robustness.
     */
    private static double getFallbackValue(double[] data) {
        return Arrays.stream(data).average().orElse(0.0);
    }
}