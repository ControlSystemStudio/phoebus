package org.csstudio.trends.databrowser3.ui.smoothview.filter;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gaussian Filter implementation
 * Implements the FilterAlgorithm interface to provide Gaussian filtering
 * for one-dimensional signals. This implementation includes:
 * - Kernel caching for performance optimization
 * - Automatic kernel normalization
 * - Adaptive sigma calculation based on window size
 * - Ensures window size is always odd for symmetric kernel
 *
 * <p>
 * @see FilterAlgorithm
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */
public class GaussianFilter implements FilterAlgorithm {
    // Cache for Gaussian kernels (sigma <= CACHE_THRESHOLD)
    private static final ConcurrentHashMap<String, double[]> KERNEL_CACHE
            = new ConcurrentHashMap<>();
    private static final double CACHE_THRESHOLD = 100.0;


    @Override
    public double[] applyFilter(double[] signal, int windowSize) {

        // Ensure window size is odd
        if (windowSize % 2 == 0) {
            windowSize++;
        }

        validateParameters(signal, windowSize);

        // Calculate sigma based on window size (empirical formula)
        final double sigma = windowSize / 6.0;
        // Use precise formatting for cache key
        final String cacheKey = String.format("%d_%.6f", windowSize, sigma);

        // Retrieve or generate Gaussian kernel
        int finalWindowSize = windowSize;
        double[] kernel = KERNEL_CACHE.computeIfAbsent(cacheKey, k ->
                (sigma <= CACHE_THRESHOLD) ? generateGaussianKernel(finalWindowSize, sigma) : null
        );

        final int halfWindow = windowSize / 2;
        final double[] filtered = new double[signal.length];

        for (int i = 0; i < signal.length; i++) {
            double weightedSum = 0;
            double totalWeight = 0;

            // Apply Gaussian convolution
            for (int j = -halfWindow; j <= halfWindow; j++) {
                final int index = i + j;
                if (index >= 0 && index < signal.length) {
                    final double weight = (kernel != null)
                            ? kernel[j + halfWindow]
                            : 1.0; // Fallback to uniform kernel if cache miss
                    weightedSum += signal[index] * weight;
                    totalWeight += weight;
                }
            }
            filtered[i] = weightedSum / totalWeight;
        }
        return filtered;
    }

    /**
     * Generate Gaussian kernel using Normal Distribution
     *
     * @param windowSize Size of the kernel window (must be odd)
     * @param sigma      Standard deviation parameter
     * @return Normalized Gaussian kernel
     */
    private double[] generateGaussianKernel(int windowSize, double sigma) {
        // Ensure window size is odd
        if (windowSize % 2 == 0) {
            windowSize++;
        }

        final double[] kernel = new double[windowSize];
        final NormalDistribution dist = new NormalDistribution(0, sigma);
        final int halfWindow = windowSize / 2;

        for (int i = -halfWindow; i <= halfWindow; i++) {
            kernel[i + halfWindow] = dist.density(i);
        }

        // Normalize kernel to maintain energy conservation
        final double sum = Arrays.stream(kernel).sum();
        return Arrays.stream(kernel).map(k -> k / sum).toArray();
    }

    /**
     * Validate input parameters
     *
     * @param signal     Input signal array
     * @param windowSize Filter window size (must be positive odd number)
     * @throws IllegalArgumentException for invalid parameters
     */
    private void validateParameters(double[] signal, int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive number");
        }
        if (signal == null || signal.length == 0) {
            throw new IllegalArgumentException("Signal cannot be empty");
        }
    }
}