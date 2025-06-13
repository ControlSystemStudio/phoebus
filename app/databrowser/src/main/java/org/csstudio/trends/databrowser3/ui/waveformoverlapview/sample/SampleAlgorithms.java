package org.csstudio.trends.databrowser3.ui.waveformoverlapview.sample;

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
 * @author Mingtao Li *
 * @since 5.0
 * China Spallation Neutron Sources
 */
public enum SampleAlgorithms {

    GroupTypicalSampling(GroupTypicalSampling.class, "GroupTypicalSampling"),
    GroupOutlierSampling(GroupOutlierSampling.class, "GroupOutlierSampling"),
    EQUAL_INTERVAL_SAMPLE(EqualIntervalSample.class, "Eqaul Interval Sample"),
    EQUAL_INTERVAL_AVERAGE(EqualIntervalAverage.class, "Equal Interval Average"),
    EARLIEST_SAMPLE(EarliestSample.class, "Earliest Sample"),
    LASTEST_SAMPLE(LatestSample.class, "Lastest Sample");

    private final Class<? extends SampleAlgorithm> algorithmClass;
    private final String displayName;

    /**
     * Constructor for filter algorithm enum entries.
     *
     * @param algorithmClass The implementation class of the filter algorithm
     * @param displayName    Human-readable name for UI display
     */
    SampleAlgorithms(Class<? extends SampleAlgorithm> algorithmClass, String displayName) {
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
                .map(SampleAlgorithms::getDisplayName)
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
    public static SampleAlgorithm getAlgorithmInstance(String displayName) {
        for (SampleAlgorithms algo : values()) {
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
        return null;
    }

    /**
     * Get the implementation class for this filter algorithm.
     *
     * @return The FilterAlgorithm implementation class
     */
    public Class<? extends SampleAlgorithm> getAlgorithmClass() {
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
