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
import java.util.List;
import java.util.logging.Level;

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
        if (! isSupportedRange(low, high)  ||   high <= low)
            throw new Error("Unsupported range " + low + " .. " + high);

        // Determine range of values on axis
        final double low_exp = (int) Math.floor(Log10.log10(low));
        final double high_exp = (int) Math.floor(Log10.log10(high));

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

        // Try major tick distance between __exponents__
        double exp_dist = (high_exp - low_exp) / num_that_fits;

        if (exp_dist < 1.0)
        {
            // All values have the same exponent, can't create a useful log scale.
            // Pick a format that shows significant detail for mantissa.
            final double low_power = Log10.pow10(low_exp);
            final double low_mantissa = low / low_power;
            final double high_power = Log10.pow10(high_exp);
            final double high_mantissa = high / high_power;

            final double distance = Math.abs(high_mantissa - low_mantissa);
            precision = determinePrecision(distance) + 1;
            num_fmt = createExponentialFormat(precision);
            detailed_num_fmt = createExponentialFormat(precision+1);
            zero_threshold = low / 100.0;

            // System.out.println("\nDegraded dist: " + exp_dist + " for range "+ num_fmt.format(low) + " .. " + num_fmt.format(high));
            // Use 4 major ticks, no minors
            for (int i=0; i<=4; ++i)
            {
                double value = low + (high - low) * i / 4;
                major_ticks.add(new MajorTick<>(value, format(value)));
            }
        }
        else
        {
            // System.out.println("\nExp dist: " + exp_dist + " for range "+ num_fmt.format(low) + " .. " + num_fmt.format(high));
            // Round up to a 'nice' step size
            exp_dist = selectNiceStep(exp_dist);
            // System.out.println("-> Nice Exp dist: " + exp_dist);

            int minor_count;
            if (exp_dist < 1.0)
            {   // Range isn't really large enough for a useful log scale,
                // ticks share the same exponent
                // -> Remove the minor ticks
                minor_count = 0;
                // Keep precision = 2
            }
            else if (exp_dist == 1)
            {
                // Example: 1e2, 1e3, 1e4 with dist==1 between exponents
                precision = 0;
                minor_count = 10;
            }
            else
            {
                // Example: 1e2, 1e4, 1e6 with dist==2 between exponents
                precision = 0;
                minor_count = 10;
            }
            num_fmt = createExponentialFormat(precision);
            detailed_num_fmt = createExponentialFormat(precision+1);

            // Compute major tick marks
            final double start = Log10.pow10(Math.ceil(Log10.log10(low) / exp_dist) * exp_dist);
            final double major_factor = Log10.pow10(exp_dist);
            double value = start;
            double prev = start / major_factor;
            zero_threshold = start - prev;
            while (value <= high*major_factor)
            {
                if (value >= low  &&  value <= high)
                    major_ticks.add(new MajorTick<>(value, format(value)));

                if (minor_count > 0)
                {   // Fill major tick marks with minor ticks
                    // Minor ticks use 1/N of the _linear range.
                    // Example:
                    // Major ticks 0,   10: Minors at  1,  2,  3,  4, ..,  9
                    // Major ticks 0,  100: Minors at 10, 20, 30, 40, .., 90
                    final double minor_step = value  / minor_count;
                    for (int i=1; i<minor_count; ++i)
                    {
                        final double min_val = prev + i * minor_step;
                        if (min_val <= low || min_val >= high)
                            continue;
                        minor_ticks.add(new MinorTick<>(min_val));
                    }
                }
                prev = value;

                value *= major_factor;
                // Rounding errors can result in a situation where
                // we don't make any progress...
                if (value <= prev)
                    break;
            }
        }

        if (major_ticks.size() < 2)
        {   // If the best-laid plans of mice and men fail
            // and we end up with just one or no tick,
            // add the low and high markers
            major_ticks.add(0, new MajorTick<>(low, format(low)));
            major_ticks.add(new MajorTick<>(high, format(high)));
        }
        this.major_ticks = major_ticks;
        this.minor_ticks = minor_ticks;
    }
}
