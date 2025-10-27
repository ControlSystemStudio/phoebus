/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.csstudio.javafx.rtplot.data;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlotDataSearchTest {

    private PlotDataProvider<Double> simpleData = new SimpleDataProvider();
    private PlotDataSearch<Double> plotDataSearch = new PlotDataSearch<>();

    @Test
    public void testFindClosestSample() {

        int index = plotDataSearch.findClosestSample(simpleData, 0.0);
        assertEquals(50, index);

        index = plotDataSearch.findClosestSample(simpleData, -50.0);
        assertEquals(0, index);

        index = plotDataSearch.findClosestSample(simpleData, 0.1);
        assertEquals(50, index);

        index = plotDataSearch.findClosestSample(simpleData, 0.9);
        assertEquals(51, index);

        index = plotDataSearch.findClosestSample(simpleData, 100.0);
        assertEquals(99, index);

        index = plotDataSearch.findClosestSample(simpleData, -100.0);
        assertEquals(0, index);

    }

    @Test
    public void testWrongType() {
        PlotDataSearch plotDataSearch1 = new PlotDataSearch();

        int index = plotDataSearch1.findClosestSample(simpleData, new UnsupportedType());
        assertEquals(-1, index);
    }

    @Test
    public void testFindSampleGreaterOrEqual() {

        int index = plotDataSearch.findSampleGreaterOrEqual(simpleData, 0.0);
        assertEquals(50, index);

        index = plotDataSearch.findSampleGreaterOrEqual(simpleData, 0.1);
        assertEquals(51, index);

        index = plotDataSearch.findSampleGreaterOrEqual(simpleData, 1.0);
        assertEquals(51, index);

        index = plotDataSearch.findSampleGreaterOrEqual(simpleData, 100.0);
        assertEquals(-1, index);

        index = plotDataSearch.findSampleGreaterOrEqual(simpleData, 48.5);
        assertEquals(99, index);

        index = plotDataSearch.findSampleGreaterOrEqual(simpleData, -100.0);
        assertEquals(0, index);

    }

    @Test
    public void testFindSampleLessOrEqual() {

        int index = plotDataSearch.findSampleLessOrEqual(simpleData, 0.0);
        assertEquals(50, index);

        index = plotDataSearch.findSampleLessOrEqual(simpleData, 0.1);
        assertEquals(50, index);

        index = plotDataSearch.findSampleLessOrEqual(simpleData, -0.1);
        assertEquals(49, index);

        index = plotDataSearch.findSampleLessOrEqual(simpleData, 0.9);
        assertEquals(50, index);

        index = plotDataSearch.findSampleLessOrEqual(simpleData, 100.0);
        assertEquals(99, index);

        index = plotDataSearch.findSampleLessOrEqual(simpleData, -100.0);
        assertEquals(-1, index);

    }

    @Test
    public void testFindSampleLessThan() {
        int index = plotDataSearch.findSampleLessThan(simpleData, 0.0);
        assertEquals(49, index);

        index = plotDataSearch.findSampleLessThan(simpleData, 0.0001);
        assertEquals(50, index);

        index = plotDataSearch.findSampleLessThan(simpleData, 100.0);
        assertEquals(99, index);

        index = plotDataSearch.findSampleLessThan(simpleData, -100.0);
        assertEquals(-1, index);
    }

    @Test
    public void testFindSampleGreaterThan() {
        int index = plotDataSearch.findSampleGreaterThan(simpleData, 0.0);
        assertEquals(51, index);

        index = plotDataSearch.findSampleGreaterThan(simpleData, 0.9999);
        assertEquals(51, index);

        index = plotDataSearch.findSampleGreaterThan(simpleData, 100.0);
        assertEquals(-1, index);

        index = plotDataSearch.findSampleGreaterThan(simpleData, -100.0);
        assertEquals(0, index);
    }

    private static class SimpleDataProvider implements PlotDataProvider<Double> {

        private List<PlotDataItem<Double>> data = new ArrayList<>();
        int elementCount = 100;

        public SimpleDataProvider() {
            for (int i = 0; i < elementCount; i++) {
                PlotDataItem<Double> item = new SimpleDataItem<>(1.0 * (i - 50), 1.0 * (i - 50));
                data.add(i, item);
            }
        }

        @Override
        public Lock getLock() {
            return null;
        }

        @Override
        public int size() {
            return elementCount;
        }

        @Override
        public PlotDataItem<Double> get(int index) {
            return data.get(index);
        }
    }

    private static class UnsupportedType implements Comparable {

        @Override
        public int compareTo(Object o) {
            return 0;
        }
    }

}
