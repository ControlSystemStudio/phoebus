package org.csstudio.trends.databrowser3.ui.smoothview.filter;

import java.util.Arrays;

/**
 * Enumeration of available filter algorithms.
 * <p>
 * This enum serves as a centralized registry for all filter algorithm
 * implementations. It provides: - Mapping between algorithm classes and
 * user-friendly display names - Factory pattern for algorithm instantiation -
 * Type-safe enumeration of available filters
 *
 * <p>
 * Each enum constant represents a specific filter implementation and contains:
 * 1. The corresponding FilterAlgorithm implementation class 2. A human-readable
 * display name
 *
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */
public enum FilterAlgorithms {

    /**
     * Parallel Median filter implementation Two-Heap Balancing Algorithm
     * (Max-Heap + Min-Heap) Time Complexity: O(M⋅N)
     *
     * @see ParallelMedianFilter
     */
    MEDIAN_FILTER(ParallelMedianFilter.class, "Moving Median"),
    /**
     * Moving average filter implementation
     *
     * @see MovingAverageFilter
     */
    MOVING_AVERAGE_FILTER(MovingAverageFilter.class, "Moving Average"),

    /**
     * Gaussian filter implementation
     *
     * @see GaussianFilter
     */
    GAUSSIAN_FILTER(GaussianFilter.class, "Moving Gaussian Filter"),
    /**
     * Exponential smoothing filter implementation
     *
     * @see ExponentialSmoothingFilter
     */
    EXPONENTIAL_SMOOTHING_FILTER(ExponentialSmoothingFilter.class, "Moving Exponential Smoothing Filter"),
    /**
     * Moving Standard Deviation implementation
     *
     * @see MovingStandardDeviation
     */
    MOVING_STANDARD_DEVIATION(MovingStandardDeviation.class, "Moving Standard Deviation");


    private final Class<? extends FilterAlgorithm> algorithmClass;
    private final String displayName;

    /**
     * Constructor for filter algorithm enum entries.
     *
     * @param algorithmClass The implementation class of the filter algorithm
     * @param displayName    Human-readable name for UI display
     */
    FilterAlgorithms(Class<? extends FilterAlgorithm> algorithmClass, String displayName) {
        this.algorithmClass = algorithmClass;
        this.displayName = displayName;
    }

    /**
     * Get all available filter display names.
     *
     * @return Array of display names
     */
    public static String[] getAllDisplayNames() {
        return Arrays.stream(values())
                .map(FilterAlgorithms::getDisplayName)
                .toArray(String[]::new);
    }

    /**
     * Get an instance of the specified filter algorithm.
     * <p>
     * Uses reflection to instantiate the algorithm implementation. Throws
     * RuntimeException if instantiation fails.
     *
     * @param displayName The display name of the desired algorithm
     * @return New instance of the FilterAlgorithm implementation
     * @throws IllegalArgumentException if algorithm not found
     * @throws RuntimeException         if instantiation fails
     */
    public static FilterAlgorithm getAlgorithmInstance(String displayName) {
        for (FilterAlgorithms algo : values()) {
            if (algo.getDisplayName().equals(displayName)) {
                try {
                    return algo.getAlgorithmClass()
                            .getDeclaredConstructor()
                            .newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate filter algorithm: " + displayName, e);
                }
            }
        }
        throw new IllegalArgumentException("No such filter algorithm: " + displayName);
    }

    /**
     * Get the implementation class for this filter algorithm.
     *
     * @return The FilterAlgorithm implementation class
     */
    public Class<? extends FilterAlgorithm> getAlgorithmClass() {
        return algorithmClass;
    }

    /**
     * Get the display name for this filter algorithm.
     *
     * @return Human-readable name suitable for UI display
     */
    public String getDisplayName() {
        return displayName;
    }
}
