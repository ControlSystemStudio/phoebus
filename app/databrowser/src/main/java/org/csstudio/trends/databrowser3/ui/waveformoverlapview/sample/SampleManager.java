package org.csstudio.trends.databrowser3.ui.waveformoverlapview.sample;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages sample algorithms for waveform data processing.
 * This class provides thread-safe management of sampling algorithms,
 * allowing dynamic selection and application of different sampling strategies.
 *
 * @see SampleAlgorithm
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */
public class SampleManager {
    private final ReentrantLock lock = new ReentrantLock();
    private SampleAlgorithm currentAlgorithm;

    /**
     * Constructs a SampleManager with an initial sampling algorithm.
     *
     * @param initialAlgorithm The initial algorithm to use
     */
    public SampleManager(SampleAlgorithm initialAlgorithm) {
        this.currentAlgorithm = initialAlgorithm;
    }

    /**
     * Checks if a sampling algorithm is currently set.
     *
     * @return {@code true} if an algorithm is set, {@code false} otherwise
     */
    public boolean hasAlgorithm() {
        return currentAlgorithm != null;
    }

    /**
     * Sets the current sampling algorithm in a thread-safe manner.
     *
     * @param algorithm The new sampling algorithm to use
     */
    public void setSampleAlgorithm(SampleAlgorithm algorithm) {
        lock.lock();
        try {
            this.currentAlgorithm = algorithm;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Applies the current sampling algorithm to the data.
     *
     * @param arrayData      Timestamped waveform data to process
     * @param maxSampleCount Target number of representative samples
     * @return Processed data after applying the sampling algorithm
     * @throws IllegalStateException if no algorithm is set
     */
    public LinkedHashMap<Instant, double[]> applyCurrentFilter(Map<Instant, double[]> arrayData,
                                                               int maxSampleCount) {
        lock.lock();
        try {
            if (currentAlgorithm == null) {
                throw new IllegalStateException("Sampling algorithm not initialized");
            }
            return currentAlgorithm.applySampling(arrayData, maxSampleCount);
        } finally {
            lock.unlock();
        }
    }
}