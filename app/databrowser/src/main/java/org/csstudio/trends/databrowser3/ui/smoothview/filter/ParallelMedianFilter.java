package org.csstudio.trends.databrowser3.ui.smoothview.filter;

import java.util.Collections;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Parallel implementation of median filter using dual heaps (max-heap and min-heap)
 * for efficient median calculation. This filter processes the signal in parallel chunks
 * to leverage multi-core CPUs, making it suitable for large datasets.
 *
 * <p>The median filter is a non-linear digital filtering technique used to remove noise
 * from a signal. It is particularly effective against impulse noise ("salt and pepper" noise).
 *
 * <p>This implementation uses two heaps to maintain the sliding window, allowing O(log n)
 * time complexity for both insertion and deletion operations. The median can be retrieved
 * in O(1) time.
 *
 * <p>When the filter processes the signal, it divides the input into chunks and processes
 * each chunk in parallel. This significantly improves performance for large signals on
 * multi-core systems.
 *
 * @see FilterAlgorithm
 * <p>
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */
public class ParallelMedianFilter implements FilterAlgorithm {
    private final int threadCount = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    @Override
    public double[] applyFilter(double[] signal, int windowSize) {
        validateParameters(signal, windowSize);

        final int length = signal.length;
        final double[] filtered = new double[length];

        // Handle edge case where window size is 1 (no filtering needed)
        if (windowSize == 1) {
            System.arraycopy(signal, 0, filtered, 0, length);
            return filtered;
        }

        // Calculate chunk size for parallel processing
        int chunkSize = Math.max(1000, length / (threadCount * 2));
        int chunkCount = (length + chunkSize - 1) / chunkSize;

        // Create and submit tasks for each chunk

        CompletableFuture<?>[] futures = new CompletableFuture[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            final int startIdx = i * chunkSize;
            final int endIdx = Math.min((i + 1) * chunkSize, length);

            futures[i] = CompletableFuture.runAsync(() -> {
                // Use two heaps to maintain the sliding window for this chunk
                PriorityQueue<Double> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
                PriorityQueue<Double> minHeap = new PriorityQueue<>();

                // Initialize the window with elements before startIdx
                int windowStart = Math.max(0, startIdx - windowSize / 2);
                for (int j = windowStart; j < startIdx; j++) {
                    addNumber(signal[j], maxHeap, minHeap);
                }

                // Process each element in the chunk
                for (int j = startIdx; j < endIdx; j++) {
                    // Add new element entering the window
                    if (j + windowSize / 2 < length) {
                        addNumber(signal[j + windowSize / 2], maxHeap, minHeap);
                    }

                    // Calculate the median for current window
                    int effectiveWindow = Math.min(windowSize, j + windowSize / 2 - windowStart + 1);
                    filtered[j] = getMedian(maxHeap, minHeap, effectiveWindow);

                    // Remove element exiting the window
                    if (j >= windowStart + windowSize - 1) {
                        removeNumber(signal[j - windowSize + 1], maxHeap, minHeap);
                    }
                }
            });
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures).join();
        executor.shutdown();
        return filtered;
    }

    /**
     * Add a number to the heaps and maintain balance
     *
     * @param num     the number to add
     * @param maxHeap the max heap storing smaller half elements
     * @param minHeap the min heap storing larger half elements
     */
    private void addNumber(double num, PriorityQueue<Double> maxHeap, PriorityQueue<Double> minHeap) {
        if (maxHeap.isEmpty() || num <= maxHeap.peek()) {
            maxHeap.offer(num);
        } else {
            minHeap.offer(num);
        }
        rebalance(maxHeap, minHeap);
    }

    /**
     * Remove a number from the heaps and maintain balance
     *
     * @param num     the number to remove
     * @param maxHeap the max heap
     * @param minHeap the min heap
     */
    private void removeNumber(double num, PriorityQueue<Double> maxHeap, PriorityQueue<Double> minHeap) {
        boolean removedFromMax = maxHeap.remove(num);
        if (!removedFromMax) {
            minHeap.remove(num);
        }
        rebalance(maxHeap, minHeap);
    }

    /**
     * Rebalance the two heaps to ensure maxHeap.size() == minHeap.size() or maxHeap.size() == minHeap.size() + 1
     *
     * @param maxHeap the max heap
     * @param minHeap the min heap
     */
    private void rebalance(PriorityQueue<Double> maxHeap, PriorityQueue<Double> minHeap) {
        while (maxHeap.size() < minHeap.size()) {
            maxHeap.offer(minHeap.poll());
        }
        while (maxHeap.size() > minHeap.size() + 1) {
            minHeap.offer(maxHeap.poll());
        }
    }

    /**
     * Get the median value from the heaps
     *
     * @param maxHeap    the max heap
     * @param minHeap    the min heap
     * @param windowSize the current window size
     * @return the median value
     */
    private double getMedian(PriorityQueue<Double> maxHeap, PriorityQueue<Double> minHeap, int windowSize) {
        if (maxHeap.isEmpty()) {
            return 0.0;
        }

        if (windowSize % 2 == 0) {
            if (minHeap.isEmpty()) {
                return maxHeap.peek();
            }
            return (maxHeap.peek() + minHeap.peek()) / 2.0;
        } else {
            return maxHeap.peek();
        }
    }

    /**
     * Validate input parameters
     *
     * @param signal     the input signal array
     * @param windowSize the window size
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