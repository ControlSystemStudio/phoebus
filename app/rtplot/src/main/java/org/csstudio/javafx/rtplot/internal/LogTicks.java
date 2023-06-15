/*******************************************************************************
 * Copyright (c) 2014-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.IntStream;

import org.csstudio.javafx.rtplot.internal.util.Log10;

/** Helper for creating tick marks.
 *  <p>
 *  Computes tick positions, formats tick labels.
 *  Doesn't perform the actual drawing.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LogTicks extends LinearTicks
{
    public LogTicks()
    {
        num_fmt = createExponentialFormat(2);
        detailed_num_fmt = createExponentialFormat(3);
    }

    /** {@inheritDoc} */
    @Override
    public void compute(final Double low, final Double high, final Graphics2D gc, final int screen_width)
    {
        logger.log(Level.FINE, "Compute log ticks, width {0}, for {1} - {2}",
                               new Object[] { screen_width, low, high });

        // Only support 'normal' order, low < high
        if (!(low > 0.0) || !isSupportedRange(low, high) || high <= low)
            throw new Error("Unsupported range " + low + " .. " + high);

        double low_exp_exact = Log10.log10(low);
        double high_exp_exact = Log10.log10(high);

        // Test format
        int precision = 2;
        num_fmt = createExponentialFormat(precision);

        // Determine minimum label distance on the screen, using some
        // percentage of the available screen space.
        // Guess the label width, using the two extremes.
        final String low_label = format(low);
        final String high_label = format(high);
        final FontMetrics metrics = gc.getFontMetrics();
        final int label_width = Math.max(metrics.stringWidth(low_label), metrics.stringWidth(high_label));
        final int num_that_fits = Math.max(1, screen_width/label_width*FILL_PERCENTAGE/100);
        final List<MajorTick<Double>> major_ticks = new ArrayList<>();
        final List<MinorTick<Double>> minor_ticks = new ArrayList<>();

        List<Integer> exponentsOfPowersOfTenInInterval = new ArrayList<>();
        List<Double> minorTickValuesInInterval = new LinkedList<>();
        for(int exponent : IntStream.rangeClosed((int) Math.floor(low_exp_exact), (int) Math.ceil(high_exp_exact)).toArray()) {
            double powerOfTen = Log10.pow10(exponent);
            if (powerOfTen >= low && powerOfTen <= high) {
                exponentsOfPowersOfTenInInterval.add(exponent);
            }

            for (int i = 1; i < 10; i++) {
                double minorTickValue = i * powerOfTen;
                if (minorTickValue >= low && minorTickValue <= high) {
                    minorTickValuesInInterval.add(minorTickValue);
                }
            }
        }

        zero_threshold = 0.0;
        if (exponentsOfPowersOfTenInInterval.size() >= 2 && exponentsOfPowersOfTenInInterval.size() > num_that_fits) {
            precision = 0;
            num_fmt = createExponentialFormat(precision);
            while (exponentsOfPowersOfTenInInterval.size() > num_that_fits) {
                List<Integer> newExponentsOfPowersOfTenInInterval = new LinkedList<>();
                for (int i=0; i < exponentsOfPowersOfTenInInterval.size()/2; i++) {
                    newExponentsOfPowersOfTenInInterval.add(i, exponentsOfPowersOfTenInInterval.get(2*i));
                }
                if (exponentsOfPowersOfTenInInterval.size() % 2 == 1) {
                    newExponentsOfPowersOfTenInInterval.add(exponentsOfPowersOfTenInInterval.get(exponentsOfPowersOfTenInInterval.size() - 1));
                }
                exponentsOfPowersOfTenInInterval = newExponentsOfPowersOfTenInInterval;
            }
            for (int exponent : exponentsOfPowersOfTenInInterval) {
                double majorTickValueInInterval = Log10.pow10(exponent);
                major_ticks.add(new MajorTick<>(majorTickValueInInterval, format(majorTickValueInInterval)));
            }
        }
        else if (exponentsOfPowersOfTenInInterval.size() >= 2 && exponentsOfPowersOfTenInInterval.size() <= num_that_fits) {
            precision = 0;
            num_fmt = createExponentialFormat(precision);
            for (int exponent : exponentsOfPowersOfTenInInterval) {
                double majorTickValueInInterval = Log10.pow10(exponent);
                major_ticks.add(new MajorTick<>(majorTickValueInInterval, format(majorTickValueInInterval)));
            }

            for (double minorTickValueInInterval : minorTickValuesInInterval) {
                minor_ticks.add(new MinorTick<>(minorTickValueInInterval));
            }
        }
        else {
            // Compute scale with linearly spaced values:
            int logOfSignificantDecimal = (int) Math.floor(Math.log10(high - low));
            double significantDecimal = Log10.pow10(logOfSignificantDecimal);
            Set<Double> tickValues = new TreeSet<>();
            double stepSize = significantDecimal;
            int steps = 0;
            do  {
                for (double tickValue = low - (low % significantDecimal) - stepSize; tickValue <= high; tickValue += stepSize) {
                    if (tickValue >= low && tickValue <= high) {
                        tickValues.add(tickValue);
                    }
                }
                steps++;
                stepSize /= 2;
            } while (tickValues.size() < num_that_fits / 2);

            for (double tickValue : tickValues) {
                precision = (int) Math.floor(Math.log10(tickValue)) + 1 + Math.max(0, -logOfSignificantDecimal) + (steps - 1);
                num_fmt = createExponentialFormat(precision-1);
                detailed_num_fmt = createExponentialFormat(precision+1);
                major_ticks.add(new MajorTick<>(tickValue, format(tickValue)));
            }
        }

        this.major_ticks = major_ticks;
        this.minor_ticks = minor_ticks;
    }
}
