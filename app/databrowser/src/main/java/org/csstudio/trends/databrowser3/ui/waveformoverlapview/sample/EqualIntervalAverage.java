package org.csstudio.trends.databrowser3.ui.waveformoverlapview.sample;

import java.time.Instant;
import java.util.*;

/**
 * Sample algorithm that generates equally-spaced average samples from the input data.
 * This implementation divides the timeline into equal intervals and computes the
 * average value for each interval, reducing the number of samples while preserving
 * overall trend.
 *
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */
public class EqualIntervalAverage implements SampleAlgorithm {

    /**
     * Applies equal-interval averaging to downsample the input data.
     *
     * @param arrayData      Map of timestamped waveform data (sorted by timestamp)
     * @param maxSampleCount Target number of representative samples to retain
     * @return A LinkedHashMap containing averaged samples at equal intervals
     */
    @Override
    public LinkedHashMap<Instant, double[]> applySampling(Map<Instant, double[]> arrayData,
                                                          int maxSampleCount) {
       
        // Handle edge cases: empty data or invalid sample count
        if (arrayData.isEmpty() || maxSampleCount <= 0) {
            return new LinkedHashMap<>();
        }

        // Convert map entries to sorted list (ascending by timestamp)
        List<Map.Entry<Instant, double[]>> sortedEntries = new ArrayList<>(arrayData.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());
        int totalSamples = sortedEntries.size();

        // Calculate interval size (ensure at least 1 sample per interval)
        int intervalSize = Math.max(1, totalSamples / maxSampleCount);
        if (totalSamples % maxSampleCount != 0) {
            intervalSize++; // Compensate for remainder to cover all samples
        }

        // Initialize result with linked hash map to maintain order
        LinkedHashMap<Instant, double[]> result = new LinkedHashMap<>();

        // Process each interval to compute average
        for (int i = 0; i < totalSamples; i += intervalSize) {
            int end = Math.min(i + intervalSize, totalSamples);
            List<Map.Entry<Instant, double[]>> intervalEntries = sortedEntries.subList(i, end);

            // Get reference waveform length from the first entry in the interval
            double[] firstWaveform = intervalEntries.get(0).getValue();
            double[] sum = new double[firstWaveform.length];
            Instant representativeTimestamp = intervalEntries.get(0).getKey();

            // Accumulate values in the interval
            for (Map.Entry<Instant, double[]> entry : intervalEntries) {
                double[] values = entry.getValue();
                for (int j = 0; j < values.length; j++) {
                    sum[j] += values[j];
                }
            }

            // Compute average and add to result
            double[] average = Arrays.stream(sum)
                    .map(v -> v / intervalEntries.size())
                    .toArray();
            result.put(representativeTimestamp, average);
        }

        return result;
    }
}