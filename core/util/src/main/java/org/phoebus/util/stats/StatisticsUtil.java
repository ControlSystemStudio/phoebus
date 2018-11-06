/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.stats;

import java.util.Iterator;
import java.util.List;

import org.epics.util.array.CollectionNumber;
import org.epics.util.array.IteratorNumber;

/**
 * Utility class to calculate statistical information.
 *
 * @author carcassi
 */
public class StatisticsUtil {

    private static class StatisticsImpl extends Statistics {

        private final int count;
        private final Range range;
        private final double average;
        private final double stdDev;

        public StatisticsImpl(Range range, int count, double average, double stdDev) {
            this.count = count;
            this.range = range;
            this.average = average;
            this.stdDev = stdDev;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public Range getRange() {
            return range;
        }

        @Override
        public double getAverage() {
            return average;
        }

        @Override
        public double getStdDev() {
            return stdDev;
        }

    }

    /**
     * Calculates data statistics, excluding NaN values.
     *
     * @param data the data
     * @return the calculated statistics
     */
    public static Statistics statisticsOf(CollectionNumber data) {
        IteratorNumber iterator = data.iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        int count = 0;
        double min = iterator.nextDouble();
        while (Double.isNaN(min)) {
            if (!iterator.hasNext()) {
                return null;
            } else {
                min = iterator.nextDouble();
            }
        }
        double max = min;
        double total = min;
        double totalSquare = min*min;
        count++;

        while (iterator.hasNext()) {
            double value = iterator.nextDouble();
            if (!Double.isNaN(value)) {
                if (value > max)
                    max = value;
                if (value < min)
                    min = value;
                total += value;
                totalSquare += value*value;
                count++;
            }
        }

        double average = total/count;
        double stdDev = Math.sqrt(totalSquare / count - average * average);

        return new StatisticsImpl(Ranges.range(min, max), count, average, stdDev);
    }

    /**
     * Aggregates statistical information.
     *
     * @param data a list of statistical information
     * @return the aggregate of all
     */
    public static Statistics statisticsOf(List<Statistics> data) {
        if (data.isEmpty())
            return null;

        Iterator<Statistics> iterator = data.iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        Statistics first = null;
        while (first == null && iterator.hasNext()) {
            first = iterator.next();
        }
        if (first == null)
            return null;

        int count = first.getCount();
        Range range = first.getRange();
        double total = first.getAverage() * first.getCount();
        double totalSquare = (first.getStdDev() * first.getStdDev() + first.getAverage() * first.getAverage()) * first.getCount();

        while (iterator.hasNext()) {
            Statistics stats = iterator.next();
            range = range.combine(stats.getRange());
            total += stats.getAverage() * stats.getCount();
            totalSquare += ( stats.getStdDev() * stats.getStdDev() + stats.getAverage() * stats.getAverage() ) * stats.getCount();
            count += stats.getCount();
        }

        double average = total/count;
        double stdDev = Math.sqrt(totalSquare / count - average * average);

        return new StatisticsImpl(range, count, average, stdDev);
    }

    /**
     * Creates the statistics, excluding NaN values, but the values
     * are actually calculated when requested.
     *
     * @param data the data
     * @return the calculated statistics
     */
    public static Statistics lazyStatisticsOf(CollectionNumber data) {
        return new LazyStatistics(data);
    }

    private static class LazyStatistics extends Statistics {

        private Statistics stats;
        private CollectionNumber data;

        public LazyStatistics(CollectionNumber data) {
            this.data = data;
        }

        private void calculateStats() {
            if (stats == null) {
                stats = statisticsOf(data);
                data = null;
            }
        }

        @Override
        public int getCount() {
            calculateStats();
            return stats.getCount();
        }

        @Override
        public double getAverage() {
            calculateStats();
            return stats.getAverage();
        }

        @Override
        public double getStdDev() {
            calculateStats();
            return stats.getStdDev();
        }

        @Override
        public Range getRange() {
            calculateStats();
            return stats.getRange();
        }

    }
}
