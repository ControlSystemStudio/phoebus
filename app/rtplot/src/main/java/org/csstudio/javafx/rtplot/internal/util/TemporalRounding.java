/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal.util;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;

/** Tools for rounding {@link ZonedDateTime} and {@link Instant}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TemporalRounding
{
    /** Units by which a date/time can be rounded */
    final public static ChronoUnit[] SUPPORTED_UNITS =
    {
        ChronoUnit.YEARS,
        ChronoUnit.MONTHS,
        ChronoUnit.DAYS,
        ChronoUnit.HOURS,
        ChronoUnit.MINUTES,
        ChronoUnit.SECONDS,
        ChronoUnit.MILLIS
    };

    /** Round value 'up'.
     *  @param value Value to round
     *  @param amount Rounding quantum
     *  @return Rounded number
     */
    public static int roundUp(final int value, final int amount)
    {
        final int round = roundUpOrSame(value, amount);
        if (round != value)
            return round;
        return value + amount;
   }

    /** Round value 'up'.
     *  If value is already a multiple of the rounding quantum,
     *  it remains unchanged, otherwise rounding 'up'
     *  @param value Value to round
     *  @param amount Rounding quantum. Zero to not round at all.
     *  @return Rounded number
     */
    public static int roundUpOrSame(final int value, final int amount)
    {
        if (amount <= 0)
            return value;
        // return (int) (Math.ceil((double)value / amount) * amount);
        return -Math.floorDiv(-value, amount) * amount;
    }

    /** {@link TemporalAdjuster} for next midnight.
     *
     *  <p>If time is already at midnight, no change.
     */
    public static TemporalAdjuster nextOrSameMidnight = (temporal) ->
    {
        final ZonedDateTime date_time = (ZonedDateTime) temporal;
        final ZonedDateTime result = ZonedDateTime.of(date_time.getYear(), date_time.getMonthValue(), date_time.getDayOfMonth(),
                0, 0, 0, 0, date_time.getZone());
        return result.isBefore(date_time)
                ? result.plusDays(1)
                : result;
    };

    /** {@link TemporalAdjuster} for rounding a {@link Instant}.
     *
     *  @param unit Unit by which to round, must be one of the {@link #SUPPORTED_UNITS}
     *  @param amount Rounding quantum
     *  @return Adjusted {@link Instant}
     *
     *  @see #zonedDateTimerRoundedToNextOrSame(ChronoUnit, int)
     */
    public static TemporalAdjuster instanceRoundedToNextOrSame(final ChronoUnit unit, final int amount)
    {
        final TemporalAdjuster handleZonedDateTime = zonedDateTimerRoundedToNextOrSame(unit, amount);
        return (temporal) ->
        {
            final ZonedDateTime date_time = ZonedDateTime.ofInstant((Instant)temporal, ZoneId.systemDefault());
            final ZonedDateTime adjusted = (ZonedDateTime) handleZonedDateTime.adjustInto(date_time);
            return adjusted.toInstant();
        };
    }

    /** {@link TemporalAdjuster} for rounding a {@link ZonedDateTime}
     *  to the next multiple of a rounding quantum
     *  (or same if it's already a multiple).
     *
     *  <p>For a <code>unit</code> of {@link ChronoUnit#DAYS},
     *  rounding by 7 days is treated as rounding to the next week, i.e. Monday.
     *
     *  <p>Rounding with a quantum of 0 truncates the date/time
     *  at the unit, usually rounding _down_. This is a side effect,
     *  caller should not depend on it.
     *
     *  @param unit Unit by which to round, must be one of the {@link #SUPPORTED_UNITS}
     *  @param amount Rounding quantum
     *  @return Adjusted {@link ZonedDateTime}
     */
    public static TemporalAdjuster zonedDateTimerRoundedToNextOrSame(final ChronoUnit unit, final int amount)
    {
        if (unit == ChronoUnit.YEARS)
            return (temporal) ->
            {
                final ZonedDateTime date_time = (ZonedDateTime)temporal;
                final int rounded_year = date_time.getMonthValue() > 1  ||  date_time.getDayOfMonth() > 1  ||
                        date_time.getHour() > 0  ||  date_time.getMinute() > 0  ||  date_time.getSecond() > 0  ||
                        date_time.getNano() > 0
                        ? roundUp(date_time.getYear(), amount)
                        : roundUpOrSame(date_time.getYear(), amount);
                return ZonedDateTime.of(rounded_year, 1, 1, 0, 0, 0, 0, date_time.getZone());
            };
        if (unit == ChronoUnit.MONTHS)
            return (temporal) ->
            {
                final ZonedDateTime date_time = (ZonedDateTime)temporal;
                // Month counted from 1; round from 0
                final int month = date_time.getMonthValue();
                final int rounded_month = date_time.getDayOfMonth() > 1  ||
                        date_time.getHour() > 0  ||  date_time.getMinute() > 0  ||  date_time.getSecond() > 0  ||
                        date_time.getNano() > 0
                        ? roundUp(month-1, amount) + 1
                        : roundUpOrSame(month-1, amount) + 1;
                final ZonedDateTime result = ZonedDateTime.of(date_time.getYear(), month, 1,
                                                              0, 0, 0, 0, date_time.getZone());
                return result.plusMonths(rounded_month - month);
            };
        if (unit == ChronoUnit.DAYS)
            return (temporal) ->
            {
                final ZonedDateTime date_time = (ZonedDateTime)temporal;
                if (amount == 7)
                {   // Locate next or same midnight, then the next or same Monday
                    return date_time.with(nextOrSameMidnight).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
                }
                // Days counted from 1; round from 0
                final int day = date_time.getDayOfMonth();
                final int rounded_day = date_time.getHour() > 0    ||  date_time.getMinute() > 0  ||
                                        date_time.getSecond() > 0  ||  date_time.getNano() > 0
                        ? roundUp(day-1, amount) + 1 : roundUpOrSame(day-1, amount) + 1;
                final ZonedDateTime result = ZonedDateTime.of(date_time.getYear(), date_time.getMonthValue(), day,
                                                              0, 0, 0, 0, date_time.getZone());
                return result.plusDays(rounded_day - day);
            };
        if (unit == ChronoUnit.HOURS)
            return (temporal) ->
            {
                final ZonedDateTime date_time = (ZonedDateTime)temporal;
                final int hour = date_time.getHour();
                final int rounded_hour = date_time.getMinute() > 0  ||  date_time.getSecond() > 0  ||  date_time.getNano() > 0
                        ? roundUp(hour, amount) : roundUpOrSame(hour, amount);
                final ZonedDateTime result = ZonedDateTime.of(date_time.getYear(), date_time.getMonthValue(), date_time.getDayOfMonth(),
                                                              hour, 0, 0, 0, date_time.getZone());
                return result.plusHours(rounded_hour - hour);
            };
        if (unit == ChronoUnit.MINUTES)
            return (temporal) ->
            {
                final ZonedDateTime date_time = (ZonedDateTime)temporal;
                final int minute = date_time.getMinute();
                final int rounded_minute = date_time.getSecond() > 0  ||  date_time.getNano() > 0
                        ? roundUp(minute, amount) : roundUpOrSame(minute, amount);
                final ZonedDateTime result = ZonedDateTime.of(date_time.getYear(), date_time.getMonthValue(), date_time.getDayOfMonth(),
                                                              date_time.getHour(), minute, 0, 0, date_time.getZone());
                return result.plusMinutes(rounded_minute - minute);
            };
        if (unit == ChronoUnit.SECONDS)
            return (temporal) ->
            {
                final ZonedDateTime date_time = (ZonedDateTime)temporal;
                final int sec = date_time.getSecond();
                final int rounded_sec = date_time.getNano() > 0
                        ? roundUp(sec, amount) : roundUpOrSame(sec, amount);
                final ZonedDateTime result = ZonedDateTime.of(date_time.getYear(), date_time.getMonthValue(), date_time.getDayOfMonth(),
                                                              date_time.getHour(), date_time.getMinute(), sec, 0, date_time.getZone());
                return result.plusSeconds(rounded_sec - sec);
            };
        if (unit == ChronoUnit.MILLIS)
            return (temporal) ->
            {
                final ZonedDateTime date_time = (ZonedDateTime)temporal;
                final int nano_for_milli = Duration.ofMillis(amount).getNano();
                final int nano = date_time.getNano();
                final int rounded_nano = roundUpOrSame(nano, nano_for_milli);
                return date_time.plusNanos(rounded_nano - nano);
            };

        throw new RuntimeException("Cannot round by " + unit);
    }
}
