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
 * Sample algorithm that retrieves the earliest N samples based on their timestamp.
 * This implementation sorts samples by timestamp in ascending order and returns the first N entries.
 *
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */
public class EarliestSample implements SampleAlgorithm {

    /**
     * Applies sampling to the provided data by selecting the earliest N samples.
     *
     * @param arrayData      Map of timestamped waveform data
     * @param maxSampleCount Maximum number of samples to retain
     * @return A LinkedHashMap containing the earliest samples in chronological order
     */
    @Override
    public LinkedHashMap<Instant, double[]> applySampling(Map<Instant, double[]> arrayData,
                                                          int maxSampleCount) {
        // Handle edge cases: null input, empty data, or non-positive sample count
        if (arrayData == null || arrayData.isEmpty() || maxSampleCount <= 0) {
            return new LinkedHashMap<>();
        }

        // Sort entries by timestamp in ascending order
        List<Map.Entry<Instant, double[]>> sortedEntries = new ArrayList<>(arrayData.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        // Ensure we don't exceed the available number of samples
        int endIndex = Math.min(maxSampleCount, sortedEntries.size());
        List<Map.Entry<Instant, double[]>> earliestEntries = sortedEntries.subList(0, endIndex);

        // Collect the earliest entries into a LinkedHashMap to preserve order
        return earliestEntries.stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing, // Merge function (shouldn't be called)
                        LinkedHashMap::new
                ));
    }
}