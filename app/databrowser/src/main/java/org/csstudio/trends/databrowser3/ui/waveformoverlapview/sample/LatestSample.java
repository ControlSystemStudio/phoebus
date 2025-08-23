/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.csstudio.trends.databrowser3.ui.waveformoverlapview.sample;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */
public class LatestSample implements SampleAlgorithm {
    /**
     * Applies sampling to the provided data by selecting the latest N samples.
     *
     * @param arrayData      Map of timestamped waveform data
     * @param maxSampleCount Maximum number of samples to retain
     * @return A LinkedHashMap containing the latest samples in chronological order
     */
    @Override
    public LinkedHashMap<Instant, double[]> applySampling(Map<Instant, double[]> arrayData,
                                                          int maxSampleCount) {
        // Handle edge cases: empty input or non-positive sample count
        if (arrayData == null || arrayData.isEmpty() || maxSampleCount <= 0) {
            return new LinkedHashMap<>();
        }

        // Sort entries by timestamp in ascending order
        List<Map.Entry<Instant, double[]>> sortedEntries = new ArrayList<>(arrayData.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        // Calculate the start index to retain the latest samples
        int startIndex = Math.max(0, sortedEntries.size() - maxSampleCount);
        List<Map.Entry<Instant, double[]>> latestEntries = sortedEntries.subList(startIndex, sortedEntries.size());

        // Collect the latest entries into a LinkedHashMap to preserve order
        return latestEntries.stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing, // Merge function (shouldn't be called)
                        LinkedHashMap::new
                ));
    }
}