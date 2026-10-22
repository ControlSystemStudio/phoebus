package org.csstudio.trends.databrowser3.ui.waveformview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WaveformHistoryChartTest {
    @Test
    void rangeIgnoresNonFiniteValues() {
        double[] range = WaveformHistoryChart.rangeOf(List.of(
                new double[]{Double.NaN, -5.0, Double.POSITIVE_INFINITY},
                new double[]{7.0, Double.NEGATIVE_INFINITY, 2.0}));
        assertEquals(-5.0, range[0]);
        assertEquals(7.0, range[1]);
    }

    @Test
    void emptyHistoryHasNoFiniteBounds() {
        double[] range = WaveformHistoryChart.rangeOf(List.of());
        assertTrue(Double.isInfinite(range[0]) && range[0] > 0);
        assertTrue(Double.isInfinite(range[1]) && range[1] < 0);
    }

    @Test
    void constantWaveformHasEqualBounds() {
        double[] range = WaveformHistoryChart.rangeOf(List.of(new double[]{3, 3, 3}));
        assertEquals(3, range[0]);
        assertEquals(3, range[1]);
    }
}
