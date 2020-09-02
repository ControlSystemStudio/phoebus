/*
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.csstudio.apputil.formula.array;


import org.epics.util.array.ArrayInteger;
import org.epics.util.array.IteratorNumber;
import org.epics.util.stats.Range;
import org.epics.util.stats.Ranges;
import org.epics.util.stats.Statistics;
import org.epics.util.stats.StatisticsUtil;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.List;

/**
 * Computes a histogram from the input array.
 */
public class HistogramOfFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "histogramOf";
    }

    @Override
    public String getDescription() {
        return "Constructs a histogram of the array data, using the specified number of bins (default 100)";
    }

    @Override
    public List<String> getArguments() {
        return List.of("array", "bin count");
    }

    @Override
    public boolean isVarArgs() {
        return true;
    }

    /**
     * Computes a histogram from the input array. User may optionally specify the number of bins,
     * which - if not specified - defaults to 100. The display field will contain information
     * on the value of the bin holding the highest count.
     *
     * @param args Arguments, count will match <code>getArgumentCount()</code>
     * @return A {@link VNumberArray} where each element holds the count for each bin. If the input
     * array is numeric, {@link BaseArrayFunction#DEFAULT_NAN_DOUBLE_ARRAY} is returned.
     */
    @Override
    public VType compute(VType... args) {
        if (VTypeHelper.isNumericArray(args[0])) {
            VNumberArray numberArray = (VNumberArray) args[0];
            if (numberArray == null) {
                return null;
            }

            VNumberArray previousValue = null;
            VNumberArray previousResult = null;
            Range previousXRange = null;

            // If no change, return previous
            if (previousValue == numberArray) {
                return previousResult;
            }

            Statistics stats = StatisticsUtil.statisticsOf(numberArray.getData());
            int nBins = args.length == 1 ? 100 : ((VNumber) args[1]).getValue().intValue();
            Range aggregatedRange = aggregateRange(stats.getRange(), previousXRange);
            Range xRange;
            if (Ranges.overlap(aggregatedRange, stats.getRange()) >= 0.75) {
                xRange = aggregatedRange;
            } else {
                xRange = stats.getRange();
            }

            IteratorNumber newValues = numberArray.getData().iterator();
            double previousMaxCount = Double.MIN_VALUE;

            int[] binData = new int[nBins];
            double maxCount = 0;
            while (newValues.hasNext()) {
                double value = newValues.nextDouble();
                // Check value in range
                if (xRange.contains(value)) {

                    int bin = (int) Math.floor(xRange.normalize(value) * nBins);
                    if (bin == nBins) {
                        bin--;
                    }

                    binData[bin]++;
                    if (binData[bin] > maxCount) {
                        maxCount = binData[bin];
                    }
                }
            }

            if (previousMaxCount > maxCount && previousMaxCount < maxCount * 2.0) {
                maxCount = previousMaxCount;
            }

            Display display = Display.of(Range.of(0.0, maxCount), Range.of(0.0, maxCount), Range.of(0.0, maxCount),
                    Range.of(0.0, maxCount), "count", NumberFormats.precisionFormat(0));

            return VNumberArray.of(ArrayInteger.of(binData), Alarm.none(), Time.now(), display);
        } else {
            return BaseArrayFunction.DEFAULT_NAN_DOUBLE_ARRAY;
        }
    }

    private Range aggregateRange(Range dataRange, Range aggregatedRange) {
        if (aggregatedRange == null) {
            return dataRange;
        } else {
            return sum(dataRange, aggregatedRange);
        }
    }

    private Range sum(Range range1, Range range2) {
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

    private Range range(final double minValue, final double maxValue) {
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue should be less then or equal to maxValue (" + minValue + ", " + maxValue + ")");
        }
        return Range.of(minValue, maxValue);
    }
}
