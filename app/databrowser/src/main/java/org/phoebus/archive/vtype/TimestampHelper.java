/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public class TimestampHelper
{
    /** Round time to next multiple of given duration
     *  @param time Original time stamp
     *  @param duration Duration to use for rounding
     *  @return Time stamp rounded up to next multiple of duration
     */
    public static Instant roundUp(final Instant time, final Duration duration)
    {
        return roundUp(time, duration.getSeconds());
    }

    final public static long SECS_PER_HOUR = TimeUnit.HOURS.toSeconds(1);
    final public static long SECS_PER_MINUTE = TimeUnit.MINUTES.toSeconds(1);
    final public static long SECS_PER_DAY = TimeUnit.DAYS.toSeconds(1);

    /** Round time to next multiple of given seconds
     *  @param time Original time stamp
     *  @param seconds Seconds to use for rounding
     *  @return Time stamp rounded up to next multiple of seconds
     */
    public static Instant roundUp(final Instant time, final long seconds)
    {
        if (seconds <= 0)
            return time;

        // Directly round seconds within an hour
        if (seconds <= SECS_PER_HOUR)
        {
            long secs = time.getEpochSecond();
            if (time.getNano() > 0)
                ++secs;
            final long periods = secs / seconds;
            secs = (periods + 1) * seconds;
            return Instant.ofEpochSecond(secs, 0);
        }

        // When rounding "2012/01/19 12:23:14" by 2 hours,
        // the user likely expects "2012/01/19 14:00:00"
        // because 12.xx rounded up by 2 is 14.
        //
        // In other words, rounding by 2 should result in an even hour,
        // but this is in the user's time zone.
        // When rounding based on the epoch seconds, which could differ
        // by an odd number of hours from the local time zone, rounding by
        // 2 hours could result in odd-numbered hours in local time.
        //
        // The addition of leap seconds can further confuse matters,
        // so perform computations that go beyond an hour in local time,
        // relative to midnight of the given time stamp.
        final ZonedDateTime local = ZonedDateTime.ofInstant(time, ZoneId.systemDefault());
        final ZonedDateTime midnight = ZonedDateTime.of(local.getYear(), local.getMonthValue(), local.getDayOfMonth(),
                                                        0, 0, 0, 0, local.getZone());

        // Round the HH:MM within the day
        long secs = local.getHour()* SECS_PER_HOUR +
                    local.getMinute() * SECS_PER_MINUTE;
        final long periods = secs / seconds;
        secs = (periods + 1) * seconds;

        // Create time for rounded HH:MM
        return midnight.toInstant().plus(Duration.ofSeconds(secs));
    }

    public static Timestamp toSQLTimestamp(Instant start) {
        return java.sql.Timestamp.from(start);
    }

    public static Instant fromSQLTimestamp(Timestamp timestamp) {
        return timestamp.toInstant();
    }
}
