/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package org.csstudio.trends.databrowser3.ui.waveformoverlapview.sample;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Mingtao Li
 * China Spallation Neutron Sources
 */
public interface SampleAlgorithm {
    LinkedHashMap<Instant, double[]> applySampling(Map<Instant, double[]> arrayData,
                                                   int showRepresentiveWaveformNumber);
}
