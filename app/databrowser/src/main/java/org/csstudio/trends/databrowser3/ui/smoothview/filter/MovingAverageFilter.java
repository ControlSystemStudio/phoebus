package org.csstudio.trends.databrowser3.ui.smoothview.filter;

/**
 * Moving Average Filter implementation
 * <p>
 * Implements the FilterAlgorithm interface to provide moving average filtering
 * for one-dimensional signals. This implementation uses prefix sum optimization
 * for O(n) time complexity.
 *
 * @author Mingtao Li
 * @see FilterAlgorithm
 * <p>
 * China Spallation Neutron Sources
 * @since 5.0
 */
public class MovingAverageFilter implements FilterAlgorithm {

    @Override
    public double[] applyFilter(double[] signal, int windowSize) {
        validateParameters(signal, windowSize);

        final int length = signal.length;
        final double[] filtered = new double[length];
        final int halfWindow = windowSize / 2;

        // Prefix sum array for optimization (reduces time complexity from O(n*k) to O(n))
        final double[] prefixSum = new double[length + 1];
        for (int i = 0; i < length; i++) {
            prefixSum[i + 1] = prefixSum[i] + signal[i];
        }

        for (int i = 0; i < length; i++) {
            // Calculate effective window boundaries
            int start = Math.max(0, i - halfWindow);
            int end = Math.min(length - 1, i + halfWindow);

            // Calculate sum using prefix sums
            double sum = prefixSum[end + 1] - prefixSum[start];
            filtered[i] = sum / (end - start + 1);
        }
        return filtered;
    }

    /**
     * Validate input parameters
     *
     * @param signal     Input signal array
     * @param windowSize Filter window size
     * @throws IllegalArgumentException for invalid parameters
     */
    private void validateParameters(double[] signal, int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be  odd number");
        }
        if (signal == null || signal.length == 0) {
            throw new IllegalArgumentException("Signal cannot be empty");
        }
    }
}