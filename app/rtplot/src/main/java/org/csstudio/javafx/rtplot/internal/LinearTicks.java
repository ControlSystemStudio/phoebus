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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import javafx.util.Pair;
import org.csstudio.javafx.rtplot.internal.util.Log10;

/** Helper for creating tick marks.
 *  <p>
 *  Computes tick positions, formats tick labels.
 *  Doesn't perform the actual drawing.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LinearTicks extends Ticks<Double>
{
    /** Neutral locale */
    private static final Locale LOCALE = Locale.ROOT;

    /** Use 'E' for exponential notation, not 'x10' */
    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(LOCALE);

    /** Numbers smaller than this are considered "0"
     *
     *  <p>Avoids labels "-0.0" or "0.0000000001" for "0"
     */
    protected double zero_threshold = 0.000001;

    /** Format helper for the number. */
    protected NumberFormat num_fmt = createDecimalFormat(1);

    /** Format helper for the detailed display of number. */
    protected NumberFormat detailed_num_fmt = createDecimalFormat(2);

    /** Threshold for order-of-magnitude to use exponential notation */
    private long exponential_threshold = 4;

    /** @param order_of_magnitude determines when to use exponential notation */
    public void setExponentialThreshold(long order_of_magnitude)
    {
        exponential_threshold = order_of_magnitude;
    }

    /** {@inheritDoc} */
    @Override
    public Pair<Double, Double> adjustRange(Double low, Double high)
    {
        if (!Double.isFinite(low)) {
            low = Double.MIN_VALUE;
        }
        if (!Double.isFinite(high)) {
            high = Double.MAX_VALUE;
        }

        // Avoid degraded axes like
        // 1000.00000000000001 .. 1000.00000000000002
        // where low + (high - low) == low,
        // i.e. tick computations will fail because
        // they reach the granularity of the Double type.
        if (Math.abs(high - low) < 3*Math.ulp(low)) {
            high = low + 3*Math.ulp(low);
        }
        return new Pair<>(low, high);
    }

    /** {@inheritDoc} */
    @Override
    public void compute(Double low, Double high, final Graphics2D gc, final int screen_width)
    {
        Pair<Double, Double> adjustedRange = adjustRange(low, high);
        double newLow = adjustedRange.getKey();
        double newHigh = adjustedRange.getValue();

        if (newLow != low || newHigh != high) {
            logger.log(Level.WARNING, "Invalid value range for a linear scale {0,number,#.###############E0} ... {1,number,#.###############E0}. Adjusting the range to {2,number,#.###############E0} ... {3,number,#.###############E0}.",
                    new Object[] {low, high, newLow, newHigh });
            high = newHigh;
            low = newLow;
        }

        logger.log(Level.FINE, "Compute linear ticks, width {0}, for {1} - {2}",
                               new Object[] { screen_width, low, high });

        if (low.equals(high))
        {
            low = high - 1;
            high += 1;
        }
        final boolean normal = low < high;
        final double range = Math.abs(high-low);

        // Determine initial precision for displaying numbers in this range.
        // Precision must be set to format test entries, which
        // are then used to compute ticks.
        final boolean use_exp_notation = shouldUseExpNotation(low, high);
        int precision;
        if (use_exp_notation)
        {
            precision = 3;
            num_fmt = createExponentialFormat(precision);
            detailed_num_fmt = createExponentialFormat(precision + 1);
        }
        else
        {
            precision = determinePrecision(range/2);
            num_fmt = createDecimalFormat(precision);
            detailed_num_fmt = createDecimalFormat(precision + 1);
        }

        // Determine minimum label distance on the screen, using some
        // percentage of the available screen space.
        // Guess the label width, using the two extremes.
        final String low_label = format(low);
        final String high_label = format(high);
        final FontMetrics metrics = gc.getFontMetrics();
        final int label_width = Math.max(metrics.stringWidth(low_label), metrics.stringWidth(high_label));
        final int num_that_fits = Math.max(1,  screen_width/label_width*FILL_PERCENTAGE/100);
        final double min_distance = range / num_that_fits;

        // Round up to the precision used to display values
        double distance = selectNiceStep(min_distance);
        if (distance == 0.0)
            throw new Error("Broken tickmark computation");
        
        // Update num_fmt based on distance between major tick labels.
        // For example, an axis with range 0 .. 10 would ordinarily use precision 0
        // and axis markers like 0, 2, 4, 6, 8, 10.
        // If the screen width is very large, it can however end with markers for
        // 0, 0.5, 1. 1.0, 1.5, .. and then needs precision 1.
        if (! use_exp_notation)
        {   // Update precision based on `distance`
            precision = determinePrecision(distance) - 1;
            num_fmt = createDecimalFormat(precision);
            detailed_num_fmt = createDecimalFormat(precision + 1);
        }

        zero_threshold = Math.abs(distance/100000);

        final List<MajorTick<Double>> major_ticks = new ArrayList<>();
        final List<MinorTick<Double>> minor_ticks = new ArrayList<>();

        // Start at 'low' adjusted to a multiple of the tick distance
        final int minor = 5;
        if (normal)
        {
            double start = Math.ceil(low / distance) * distance;

        	// Set prev to one before the start
        	// and loop until high + distance
        	// to get minor marks before and after the major tick mark range
        	double prev = start - distance;
        	for (double value = start; value < high + distance; value += distance)
        	{
        	    // Compute major tick marks
        	    if (value >= low  &&  value <= high)
        	        major_ticks.add(new MajorTick<>(value, format(value)));

        	    // Fill major tick marks with minor ticks
        	    for (int i=1; i<minor; ++i)
        	    {
        	        final double min_val = prev + ((value - prev)*i)/minor;
        	        if (min_val <= low || min_val >= high)
        	            continue;
        	        minor_ticks.add(new MinorTick<>(min_val));
        	    }
        	    prev = value;
        	}
        }
        else
        {
        	distance = -distance;
        	double start = Math.floor(low / distance) * distance;

            double prev = start - distance;
            for (double value = start; value > high + distance; value += distance)
            {
                // Compute major tick marks
                if (value <= low  &&  value >= high)
                    major_ticks.add(new MajorTick<>(value, format(value)));

                // Fill major tick marks with minor ticks
                for (int i=1; i<minor; ++i)
                {
                    final double min_val = prev + ((value - prev)*i)/minor;
                    if (min_val >= low || min_val <= high)
                        continue;
                    minor_ticks.add(new MinorTick<>(min_val));
                }
                prev = value;
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

    protected boolean shouldUseExpNotation(Double low, Double high) {
        boolean isLargeOrderOfMagnitude = Log10.log10(Math.abs(low)) >= exponential_threshold + 1 || Log10.log10(Math.abs(high)) >= exponential_threshold + 1;
        boolean isSmallOrderOfMagnitude = Log10.log10(Math.abs(low)) < -exponential_threshold && Log10.log10(Math.abs(high)) < -exponential_threshold;

        return isLargeOrderOfMagnitude || isSmallOrderOfMagnitude;
    }

    /** @param number A number
     *  @return Suggested precision, i.e. floating point digits to display
     */
    public static int determinePrecision(final double number)
    {
        // Log gymnastics:
        // Number  Ceil(Log10)     Show as     Precision
        // 10.0        1             "5.0"        1
        //  5.0        1             "5.0"        1
        //  0.5        0             "0.50"       2
        //  0.05      -1             "0.05"       3
        final double log = Math.log10(number);
        final int rounded_log = (int)Math.ceil(log);
        // Precision: 0 or more trailing digits
        if (number > 10.0)
            return 0;
        return 2 - rounded_log;
    }

    /** Nice looking steps for the distance between tick,
     *  and the threshold for using them.
     *  In general, the computed steps "fill" the axis.
     *  The nice looking steps should be wider apart,
     *  because tighter steps would result in overlapping label.
     *  The thresholds thus favor the larger steps:
     *  A computed distance of 6.1 turns into 10.0, not 5.0.
     *  @see #selectNiceStep(double)
     */
    final private static double[] NICE_STEPS = { 1.0, 2.0, 5.0, 10.0 };

    /** To a human viewer, tick distances of 5.0 are easier to see
     *  than for example 7.
     *
     *  <p>This method tries to adjust a computed tick distance
     *  to one that is hopefully 'nicer'
     *
     *  @param min_distance Original step distance
     *  @return
     */
    public static double selectNiceStep(final double min_distance)
    {
        final double log = Math.log10(min_distance);
        final double order_of_magnitude = Math.pow(10, Math.floor(log));
        for (int i=0; i<NICE_STEPS.length; ++i)
            if (NICE_STEPS[i] * order_of_magnitude >= min_distance)
                return NICE_STEPS[i] * order_of_magnitude;
        return min_distance;
    }

    /** Create decimal format
     *  @param precision
     *  @return NumberFormat
     */
    protected static NumberFormat createDecimalFormat(final int precision)
    {
        final NumberFormat fmt = NumberFormat.getNumberInstance(LOCALE);
        fmt.setGroupingUsed(false);
        fmt.setMinimumFractionDigits(precision);
        fmt.setMaximumFractionDigits(precision);
        return fmt;
    }

    private final static ConcurrentHashMap<Integer, NumberFormat> exponential_formats = new ConcurrentHashMap<>();

    /** Create exponential format
     *  @param mantissa_precision
     *  @return NumberFormat
     */
    protected static NumberFormat createExponentialFormat(final Integer mantissa_precision)
    {
        return exponential_formats.computeIfAbsent(mantissa_precision, prec ->
        {
            // DecimalFormat needs pattern for exponential notation,
            // there are no factory or configuration methods
            final StringBuilder pattern = new StringBuilder("0");
            if (prec > 0)
                pattern.append('.');
            for (int i=0; i<prec; ++i)
                pattern.append('0');
            pattern.append("E0");
            return new DecimalFormat(pattern.toString(), SYMBOLS);
        });
    }

    /** {@inheritDoc} */
    @Override
    public String format(final Double num)
    {
        if (num.isNaN())
            return "NaN";
        if (num.isInfinite())
            return "Inf";
        // Patch numbers that are "very close to zero"
        // to avoid "-0.00" or "0.0e-22"
        if (Math.abs(num) < zero_threshold)
            return num_fmt.format(0.0);
        return num_fmt.format(num);
    }

    /** {@inheritDoc} */
    @Override
    public String formatDetailed(final Double num)
    {
        if (num.isNaN())
            return "NaN";
        if (num.isInfinite())
            return "Inf";
        // Do NOT patch numbers "very close to zero"
        // in detailed format because that could
        // hide what user wants to inspect
        return detailed_num_fmt.format(num);
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return "Ticks: " + getMajorTicks();
    }
}
