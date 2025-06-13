package org.csstudio.trends.databrowser3.ui.smoothview.filter;

/**
 * Moving Standard Deviation Filter implementation using rolling window technique.
 * <p>
 * This filter calculates the standard deviation over a sliding window
 * using a direct method that maintains sums and sums of squares for efficiency.
 *
 * @author Mingtao Li
 * @see FilterAlgorithm
 * @since 5.0
 */
public class MovingStandardDeviation implements FilterAlgorithm {
    @Override
    public double[] applyFilter(double[] signal, int windowSize) {
        validateParameters(signal, windowSize);
        // Ensure window size is at least 2, as standard deviation requires at least two data points
        // to calculate variance (single data point results in zero variance, which is not meaningful)
        if (windowSize == 1)
            windowSize++;
        final int length = signal.length;
        final double[] result = new double[length];

        // Initialize accumulators for sum and sum of squares
        double sum = 0.0;
        double sumOfSquares = 0.0;
        int count = 0;

        // Window boundaries
        int windowStart = 0;

        // Process each data point in the signal
        for (int i = 0; i < length; i++) {
            // Add current value to the window
            double currentValue = signal[i];
            sum += currentValue;
            sumOfSquares += currentValue * currentValue;
            count++;

            // If window exceeds the specified size, remove the oldest value
            if (count > windowSize) {
                double oldestValue = signal[windowStart];
                sum -= oldestValue;
                sumOfSquares -= oldestValue * oldestValue;
                count--;
                windowStart++;
            }

            // Calculate mean and variance
            double mean = sum / count;
            double variance = (sumOfSquares / count) - (mean * mean);

            // Ensure non-negative variance (due to floating-point precision)
            variance = Math.max(0, variance);

            // Compute standard deviation
            result[i] = Math.sqrt(variance);
        }

        return result;
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
            throw new IllegalArgumentException("Window size must be positive");
        }

        if (signal == null || signal.length == 0) {
            throw new IllegalArgumentException("Signal cannot be empty");
        }
    }
}