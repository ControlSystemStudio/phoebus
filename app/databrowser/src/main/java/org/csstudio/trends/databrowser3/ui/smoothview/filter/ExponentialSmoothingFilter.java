package org.csstudio.trends.databrowser3.ui.smoothview.filter;

/**
 * Exponential Smoothing Filter implementation
 * <p>
 * Implements the FilterAlgorithm interface to provide simple exponential smoothing
 * (SES) for one-dimensional signals. This implementation uses a smoothing factor
 * (alpha) derived from the window size parameter, following the approximation:
 * α ≈ 2/(N+1), where N is the window size.
 *
 * <p>
 * Exponential smoothing is a time series forecasting method for univariate data
 * that produces smoothed values by giving more weight to recent observations.
  * @see FilterAlgorithm
 * <p>
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */

public class ExponentialSmoothingFilter implements FilterAlgorithm {
    @Override
    public double[] applyFilter(double[] signal, int windowSize) {
        validateParameters(signal, windowSize);

        final double[] filtered = new double[signal.length];
        final double alpha = 2.0 / (windowSize + 1);

        if (signal.length == 0)
            return filtered;

        filtered[0] = signal[0];
        for (int i = 1; i < signal.length; i++) {
            filtered[i] = alpha * signal[i] + (1 - alpha) * filtered[i - 1];
        }
        return filtered;
    }

    private void validateParameters(double[] signal, int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive");
        }
        if (signal == null || signal.length == 0) {
            throw new IllegalArgumentException("Signal cannot be empty");
        }
    }
}