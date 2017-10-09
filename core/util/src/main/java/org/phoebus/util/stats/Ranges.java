/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.stats;

/**
 * Utility classes to compute ranges.
 *
 * @author carcassi
 */
public class Ranges {

    /**
     * Returns the range of the absolute values within the range.
     * <p>
     * If the range is all positive, it returns the same range.
     *
     * @param range a range
     * @return the range of the absolute values
     */
    public static Range absRange(Range range) {
        if (range.getMinimum() >= 0 && range.getMaximum() >= 0) {
            return range;
        } else if (range.getMinimum() < 0 && range.getMaximum() < 0) {
            return range(- range.getMaximum(), - range.getMinimum());
        } else {
            return range(0, Math.max(range.getMinimum(), range.getMaximum()));
        }
    }

    /**
     * Range from given min and max.
     *
     * @param minValue minimum value
     * @param maxValue maximum value
     * @return the range
     */
    public static Range range(final double minValue, final double maxValue) {
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue should be less then or equal to maxValue (" + minValue+ ", " + maxValue + ")");
        }
        return new Range(minValue, maxValue, false);
    }

    /**
     * Determines the range that can contain both ranges. If one of the
     * ranges in contained in the other, the bigger range is returned.
     *
     * @param range1 a range
     * @param range2 another range
     * @return the bigger range
     */
    public static Range sum(Range range1, Range range2) {
        if (range1.getMinimum() <= range2.getMinimum()) {
            if (range1.getMaximum() >= range2.getMaximum()) {
                return range1;
            } else {
                return range(range1.getMinimum(), range2.getMaximum());
            }
        } else {
            if (range1.getMaximum() >= range2.getMaximum()) {
                return range(range2.getMinimum(), range1.getMaximum());
            } else {
                return range2;
            }
        }

    }

    /**
     * Increases the given aggregated range with the new data range.
     * <p>
     * TODO: maybe this should be re-thought: it's the same as sum with
     * different null handling. Maybe a RangeAggregator utility class
     * that also handles numbers?
     *
     * @param dataRange the new data range; can't be null
     * @param aggregatedRange the old aggregated range; can be null
     * @return a range big enough to contain both ranges
     */
    public static Range aggregateRange(Range dataRange, Range aggregatedRange) {
        if (aggregatedRange == null) {
            return dataRange;
        } else {
            return Ranges.sum(dataRange, aggregatedRange);
        }
    }

    /**
     * Percentage, from 0 to 1, of the first range that is contained by
     * the second range.
     *
     * @param range the range to be contained by the second
     * @param otherRange the range that has to contain the first
     * @return from 0 (if there is no intersection) to 1 (if the ranges are the same)
     */
    public static double overlap(Range range, Range otherRange) {
        double minOverlap = Math.max(range.getMinimum(), otherRange.getMinimum());
        double maxOverlap = Math.min(range.getMaximum(), otherRange.getMaximum());
        double overlapWidth = maxOverlap - minOverlap;
        double rangeWidth = range.getMaximum() - range.getMinimum();
        double fraction = Math.max(0.0, overlapWidth / rangeWidth);
        return fraction;
    }
}
