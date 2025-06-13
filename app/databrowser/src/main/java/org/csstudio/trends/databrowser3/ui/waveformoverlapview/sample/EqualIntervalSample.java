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

/**
 * Sample algorithm that performs equal-interval sampling on timestamped waveform data.
 * This implementation selects samples at regular intervals from the sorted timeline,
 * ensuring a fixed number of representative samples.
 *
 * @author Mingtao Li
 * @author China Spallation Neutron Sources
 */
public class EqualIntervalSample implements SampleAlgorithm {

    /**
     * Applies equal-interval sampling to reduce the number of waveform samples.
     *
     * @param arrayData      Map of timestamped waveform data (sorted by timestamp)
     * @param maxSampleCount Target number of representative samples to retain
     * @return A LinkedHashMap containing samples at equal intervals
     */
    @Override
    public LinkedHashMap<Instant, double[]> applySampling(Map<Instant, double[]> arrayData,
                                                          int maxSampleCount) {
        // Handle edge cases early
        int totalSamples = arrayData.size();
        if (totalSamples <= maxSampleCount || maxSampleCount <= 0) {
            return new LinkedHashMap<>(arrayData);
        }

        // Precompute sampling interval
        int interval = Math.max(1, totalSamples / maxSampleCount);
        // Create result map with expected capacity to avoid resizing
        LinkedHashMap<Instant, double[]> result = new LinkedHashMap<>(maxSampleCount);

        // Optimized iteration using index-based approach
        List<Map.Entry<Instant, double[]>> entries = new ArrayList<>(arrayData.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        for (int i = 0; i < totalSamples; i += interval) {
            result.put(entries.get(i).getKey(), entries.get(i).getValue());

            // Early exit if we've reached the maximum count
            if (result.size() >= maxSampleCount) {
                break;
            }
        }

        return result;
    }
}