/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.internal.util.TemporalRounding;

/** Helper for creating tick marks.
 *  <p>
 *  Computes tick positions, formats tick labels.
 *  Doesn't perform the actual drawing.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimeTicks extends Ticks<Instant>
{
    /** How many percent of the available space should be used for labels? */
    protected static final int FILL_PERCENTAGE = 75;

    /** Supported time range */
    final private static Duration min = Duration.ofMillis(10), max = Duration.ofDays(50*365);

    /** Description of a tick configuration */
    private static class TickConfig
    {
        /** A computed distance between ticks above this threshold will select this configuration */
        final private Duration threshold;

        /** Rounding to turn 'start' into initial tick */
        final private TemporalAdjuster rounding;

        /** Distance between tick mark value. */
        final private TemporalAmount distance;

        /** Number of minor ticks */
        final private int minor_ticks;

        /** Formatter for the initial start time stamp */
        final private DateTimeFormatter start_formatter;

        /** Formatter for remaining ticks */
        final private DateTimeFormatter formatter;

        TickConfig(final Duration threshold,
                final ChronoUnit unit, final int amount,
                final TemporalAmount distance,
                final int minor_ticks,
                final String start_format_pattern, final String format_pattern)
        {
            this.threshold = threshold;
            this.rounding = TemporalRounding.instanceRoundedToNextOrSame(unit, amount);
            this.distance = distance;
            this.minor_ticks = minor_ticks;
            this.start_formatter = DateTimeFormatter.ofPattern(start_format_pattern);
            this.formatter = DateTimeFormatter.ofPattern(format_pattern);
        }
    }

    /** Possible configurations, largest(!) distance between ticks first */
    final private static TickConfig[] tick_configs = new TickConfig[]
    {
        new TickConfig(Duration.ofDays(365*5), ChronoUnit.YEARS, 5, Period.ofYears(5), 5,
                "yyyy", "yyyy"),
        new TickConfig(Duration.ofDays(365*2), ChronoUnit.YEARS, 2, Period.ofYears(2), 6,
                "yyyy", "yyyy"),
        new TickConfig(Duration.ofDays(365/2), ChronoUnit.YEARS, 1, Period.ofYears(1), 6,
                "yyyy", "yyyy"),
        new TickConfig(Duration.ofDays(90), ChronoUnit.MONTHS, 6, Period.ofMonths(6), 6,
                "yyyy-MM", "yyyy-MM"),
        new TickConfig(Duration.ofDays(60), ChronoUnit.MONTHS, 3, Period.ofMonths(3), 3,
                "yyyy-MM", "yyyy-MM"),
        new TickConfig(Duration.ofDays(30), ChronoUnit.MONTHS, 2, Period.ofMonths(2), 4,
                "yyyy-MM", "yyyy-MM"),
        new TickConfig(Duration.ofDays(20), ChronoUnit.MONTHS, 1, Period.ofMonths(1), 0,
                "yyyy-MM-dd", "MM-dd"),
        new TickConfig(Duration.ofDays(3), ChronoUnit.DAYS, 7, Period.ofDays(7), 7,
                "yyyy-MM-dd", "MM-dd"),
        new TickConfig(Duration.ofDays(1), ChronoUnit.DAYS, 2, Period.ofDays(2), 2,
                "yyyy-MM-dd", "MM-dd"),
        new TickConfig(Duration.ofHours(13), ChronoUnit.DAYS, 1, Period.ofDays(1), 2,
                "yyyy-MM-dd", "MM-dd"),
        new TickConfig(Duration.ofHours(7), ChronoUnit.HOURS, 12, Duration.ofHours(12), 6,
                "HH:mm\nyyyy-MM-dd", "HH:mm\nMM-dd"),
        new TickConfig(Duration.ofHours(2), ChronoUnit.HOURS, 6, Duration.ofHours(6), 6,
                "HH:mm\nyyyy-MM-dd", "HH:mm\nMM-dd"),
        new TickConfig(Duration.ofHours(1), ChronoUnit.HOURS, 2, Duration.ofHours(2), 6,
                "HH:mm\nyyyy-MM-dd", "HH:mm"),
        new TickConfig(Duration.ofMinutes(40), ChronoUnit.HOURS, 1, Duration.ofHours(1), 6,
                "HH:mm\nyyyy-MM-dd", "HH:mm"),
        new TickConfig(Duration.ofMinutes(15), ChronoUnit.MINUTES, 30, Duration.ofMinutes(30), 3,
                "HH:mm\nyyyy-MM-dd", "HH:mm"),
        new TickConfig(Duration.ofMinutes(7), ChronoUnit.MINUTES, 10, Duration.ofMinutes(10), 5,
                "HH:mm\nyyyy-MM-dd", "HH:mm"),
        new TickConfig(Duration.ofMinutes(3), ChronoUnit.MINUTES, 5, Duration.ofMinutes(5), 5,
                "HH:mm\nyyyy-MM-dd", "HH:mm"),
        new TickConfig(Duration.ofMinutes(1), ChronoUnit.MINUTES, 2, Duration.ofMinutes(2), 6,
                "HH:mm\nyyyy-MM-dd", "HH:mm"),
        new TickConfig(Duration.ofSeconds(40), ChronoUnit.MINUTES, 1, Duration.ofMinutes(1), 6,
                "HH:mm\nyyyy-MM-dd", "HH:mm"),
        new TickConfig(Duration.ofSeconds(15), ChronoUnit.SECONDS, 30, Duration.ofSeconds(30), 3,
                "HH:mm:ss\nyyyy-MM-dd", "HH:mm:ss"),
        new TickConfig(Duration.ofSeconds(7), ChronoUnit.SECONDS, 10, Duration.ofSeconds(10), 5,
                "HH:mm:ss\nyyyy-MM-dd", "HH:mm:ss"),
        new TickConfig(Duration.ofSeconds(3), ChronoUnit.SECONDS, 5, Duration.ofSeconds(5), 5,
                "HH:mm:ss\nyyyy-MM-dd", "HH:mm:ss"),
        new TickConfig(Duration.ofSeconds(1), ChronoUnit.SECONDS, 2, Duration.ofSeconds(2), 2,
                "HH:mm:ss\nyyyy-MM-dd", "HH:mm:ss"),
        new TickConfig(Duration.ofMillis(600), ChronoUnit.SECONDS, 1, Duration.ofSeconds(1), 5,
                "HH:mm:ss\nyyyy-MM-dd", "HH:mm:ss"),
        new TickConfig(Duration.ofMillis(300), ChronoUnit.MILLIS, 500, Duration.ofMillis(500), 5,
                "HH:mm:ss.SSS\nyyyy-MM-dd", "mm:ss.SSS"),
        new TickConfig(Duration.ofMillis(120), ChronoUnit.MILLIS, 200, Duration.ofMillis(200), 2,
                "HH:mm:ss.SSS\nyyyy-MM-dd", "mm:ss.SSS"),
        new TickConfig(Duration.ofMillis(60), ChronoUnit.MILLIS, 100, Duration.ofMillis(100), 5,
                "HH:mm:ss.SSS\nyyyy-MM-dd", "mm:ss.SSS"),
        new TickConfig(Duration.ofMillis(25), ChronoUnit.MILLIS, 50, Duration.ofMillis(50), 5,
                "HH:mm:ss.SSS\nyyyy-MM-dd", "mm:ss.SSS"),
        new TickConfig(Duration.ofMillis(12), ChronoUnit.MILLIS, 20, Duration.ofMillis(20), 2,
                "HH:mm:ss.SSS\nyyyy-MM-dd", "mm:ss.SSS"),
        new TickConfig(Duration.ofMillis(7), ChronoUnit.MILLIS, 10, Duration.ofMillis(10), 5,
                "HH:mm:ss.SSS\nyyyy-MM-dd", "mm:ss.SSS"),
        new TickConfig(Duration.ofMillis(3), ChronoUnit.MILLIS, 5, Duration.ofMillis(5), 5,
                "HH:mm:ss.SSS\nyyyy-MM-dd", "mm:ss.SSS"),
        new TickConfig(Duration.ofMillis(1), ChronoUnit.MILLIS, 2, Duration.ofMillis(2), 2,
                "HH:mm:ss.SSS\nyyyy-MM-dd", "mm:ss.SSS"),
        new TickConfig(Duration.ZERO, ChronoUnit.MILLIS, 1, Duration.ofMillis(1), 5,
                "HH:mm:ss.SSS\nyyyy-MM-dd", "mm:ss.SSS"),
    };

    /** First tick mark value. */
    private Instant start;

    /** Tick mark configuration */
    private TickConfig config, detailed_config;

    /** {@inheritDoc} */
    @Override
    public boolean isSupportedRange(final Instant low, final Instant high)
    {
        final Duration range = Duration.between(low, high);
        return low.compareTo(high) < 0  &&
               range.compareTo(min) >= 0  &&  range.compareTo(max) <= 0;
    }

    /** {@inheritDoc} */
    @Override
    public void compute(final Instant low, final Instant high, final Graphics2D gc, final int screen_width)
    {
        final Duration range = Duration.between(low, high);
        if (range.isNegative())
            throw new Error("Tick range is not ordered, " + low + " > " + high);

        // Estimate number of labels that fits on screen
        final int label_width = gc.getFontMetrics().stringWidth("yyyy-MM-dd");
        final int num_that_fits = Math.max(1,  screen_width/label_width*FILL_PERCENTAGE/100);

        final Duration distance = range.dividedBy(num_that_fits);

        // Which of the available formats suits the visible time range?
        for (int i=0; i<tick_configs.length; ++i)
        {
            final TickConfig option = tick_configs[i];
            if (distance.compareTo(option.threshold) >= 0)
            {
                config = option;
                detailed_config = i < tick_configs.length-1 ? tick_configs[i+1] : tick_configs[i];
                break;
            }
        }
        start = low.with(config.rounding);

        logger.log(Level.FINE, "Compute time ticks for {0}, {1} pixels: Tick distance {2}",
                               new Object[] { range, screen_width, config.distance });

        final List<MajorTick<Instant>> major_ticks = new ArrayList<>();
        final List<MinorTick<Instant>> minor_ticks = new ArrayList<>();

        long prev_ms = getPrevious(start).toEpochMilli();
        final Instant end = getNext(high);
        for (Instant value = start;  value.isBefore(end);  value = getNext(value))
        {
            if (value.compareTo(low) >= 0  &&  value.compareTo(high) <= 0)
                major_ticks.add(new MajorTick<>(value, format(value)));

            final long ms = value.toEpochMilli();
            for (int i=1; i<config.minor_ticks; ++i)
            {
                final long min_ms = prev_ms + ((ms - prev_ms)*i)/config.minor_ticks;
                final Instant min_val = Instant.ofEpochMilli(min_ms);
                if (min_val.isAfter(low)  &&  min_val.isBefore(high))
                    minor_ticks.add(new MinorTick<>(min_val));
            }
            prev_ms = ms;
        }

        if (major_ticks.size() < 2)
        {   // If the best-laid plans of mice and men fail
            // and we end up with just one or no tick,
            // add the low and high markers.
            // Use full format for the low marker.
            final ZonedDateTime local = ZonedDateTime.ofInstant(low, ZoneId.systemDefault());
            major_ticks.add(0, new MajorTick<>(low, config.start_formatter.format(local)));
            major_ticks.add(new MajorTick<>(high, format(high)));
        }
        this.major_ticks = major_ticks;
        this.minor_ticks = minor_ticks;
    }

    private Instant getPrevious(final Instant tick)
    {
        if (config.distance instanceof Period)
        {   // Date-based distance must be computed in local time,
            // because "1 day" is not always 24 hours,
            // and "1 month" varies as well
            final ZonedDateTime local = ZonedDateTime.ofInstant(tick, ZoneId.systemDefault());
            return local.minus(config.distance).toInstant();
        }
        else // Plain Duration, can be computed on Instant
            return tick.minus(config.distance);
    }

    private Instant getNext(final Instant tick)
    {
        if (config.distance instanceof Period)
        {   // Date-based distance must be computed in local time,
            // because "1 day" is not always 24 hours,
            // and "1 month" varies as well
            final ZonedDateTime local = ZonedDateTime.ofInstant(tick, ZoneId.systemDefault());
            return local.plus(config.distance).toInstant();
        }
        else // Plain Duration, can be computed on Instant
            return tick.plus(config.distance);
    }

    /** {@inheritDoc} */
    @Override
    public String format(final Instant tick)
    {
        final ZonedDateTime local = ZonedDateTime.ofInstant(tick, ZoneId.systemDefault());
        // Use full format for the start tick
        if (tick.compareTo(start) <= 0)
            return config.start_formatter.format(local);
        return config.formatter.format(local);
    }

    /** {@inheritDoc} */
    @Override
    public String formatDetailed(final Instant tick)
    {
        final ZonedDateTime local = ZonedDateTime.ofInstant(tick, ZoneId.systemDefault());
        return detailed_config.formatter.format(local);
    }
}
