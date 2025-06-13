package org.csstudio.trends.databrowser3.ui.waveformoverlapview.sample;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Sample algorithm that selects outlier waveforms from equally-sized groups.
 * For each group of consecutive samples, the waveform most distant from the group's
 * centroid (average) is chosen as the representative, highlighting abnormal patterns.
 *
 * @author Mingtao Li
 * China Spallation Neutron Sources
 */
public class GroupOutlierSampling implements SampleAlgorithm {

    @Override
    public LinkedHashMap<Instant, double[]> applySampling(Map<Instant, double[]> arrayData,
                                                          int maxSampleCount) {
        // Handle edge cases: empty data or invalid sample count
        if (arrayData.isEmpty() || maxSampleCount <= 0) {
            return new LinkedHashMap<>();
        }

        // Convert to sorted list (by timestamp)
        List<Entry<Instant, double[]>> sortedEntries = new ArrayList<>(arrayData.entrySet());
        int totalSamples = sortedEntries.size();

        // Return all data if sample count is sufficient
        if (totalSamples <= maxSampleCount) {
            return new LinkedHashMap<>(arrayData);
        }

        // Calculate group size and initialize result
        int groupSize = (int) Math.ceil((double) totalSamples / maxSampleCount);
        LinkedHashMap<Instant, double[]> result = new LinkedHashMap<>(maxSampleCount);

        // Process each group to find the most outlying waveform
        for (int i = 0; i < maxSampleCount; i++) {
            int start = i * groupSize;
            // Prevent invalid sublist range: exit loop if start exceeds total samples
            if (start >= totalSamples) {
                break;
            }
            int end = Math.min(start + groupSize, totalSamples);
            List<Entry<Instant, double[]>> group = sortedEntries.subList(start, end);

            // Find outlier in the group
            Entry<Instant, double[]> outlier = findOutlierInGroup(group);
            if (outlier != null) {
                result.put(outlier.getKey(), outlier.getValue());
            }
        }

        return result;
    }

    /**
     * Find the most outlying waveform in a group by Euclidean distance from the centroid.
     *
     * @param group List of timestamped waveforms in the group
     * @return The outlier waveform entry, or null if the group is empty
     */
    private Entry<Instant, double[]> findOutlierInGroup(List<Entry<Instant, double[]>> group) {
        if (group.isEmpty()) {
            return null;
        }
        if (group.size() == 1) {
            return group.get(0); // Single waveform is trivially the outlier
        }

        // Calculate group centroid (average waveform)
        double[] centroid = computeCentroid(group);

        // Find waveform with maximum distance from centroid
        double maxDistance = -1.0;
        Entry<Instant, double[]> outlier = null;
        for (Entry<Instant, double[]> entry : group) {
            double distance = computeEuclideanDistance(entry.getValue(), centroid);
            if (distance > maxDistance) {
                maxDistance = distance;
                outlier = entry;
            }
        }
        return outlier;
    }

    /**
     * Compute the centroid (average waveform) of a group.
     *
     * @param group List of waveforms to compute centroid for
     * @return Centroid waveform as a double array
     */
    private double[] computeCentroid(List<Entry<Instant, double[]>> group) {
        int waveformLength = group.get(0).getValue().length;
        double[] centroid = new double[waveformLength];

        // Sum all waveforms in the group
        for (Entry<Instant, double[]> entry : group) {
            double[] waveform = entry.getValue();
            for (int i = 0; i < waveformLength; i++) {
                centroid[i] += waveform[i];
            }
        }

        // Normalize by group size
        int groupSize = group.size();
        for (int i = 0; i < waveformLength; i++) {
            centroid[i] /= groupSize;
        }
        return centroid;
    }

    /**
     * Compute Euclidean distance between two waveforms.
     *
     * @param w1 First waveform
     * @param w2 Second waveform
     * @return Euclidean distance between the two waveforms
     * @throws IllegalArgumentException if waveforms have different lengths
     */
    private double computeEuclideanDistance(double[] w1, double[] w2) {
        if (w1.length != w2.length) {
            throw new IllegalArgumentException("Waveforms must have the same length");
        }
        double sumOfSquares = 0.0;
        for (int i = 0; i < w1.length; i++) {
            double diff = w1[i] - w2[i];
            sumOfSquares += diff * diff;
        }
        return Math.sqrt(sumOfSquares);
    }
}