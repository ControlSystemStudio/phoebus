/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.util;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.csstudio.javafx.rtplot.internal.util.TemporalRounding;
import org.junit.Test;

import static org.csstudio.javafx.rtplot.internal.util.TemporalRounding.SUPPORTED_UNITS;
import static org.csstudio.javafx.rtplot.internal.util.TemporalRounding.instanceRoundedToNextOrSame;
import static org.csstudio.javafx.rtplot.internal.util.TemporalRounding.nextOrSameMidnight;
import static org.csstudio.javafx.rtplot.internal.util.TemporalRounding.roundUp;
import static org.csstudio.javafx.rtplot.internal.util.TemporalRounding.roundUpOrSame;
import static org.csstudio.javafx.rtplot.internal.util.TemporalRounding.zonedDateTimerRoundedToNextOrSame;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/** JUnit test of {@link TemporalRounding}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TemporalRoundingTest
{

    public TemporalRoundingTest() {
        Locale.setDefault(Locale.US);
    }

    @Test
    public void testRounding()
    {
        assertThat(roundUpOrSame(0, 10), equalTo(0));
        assertThat(roundUp(0, 10), equalTo(10));

        assertThat(roundUpOrSame(1, 10), equalTo(10));
        assertThat(roundUp(1, 10), equalTo(10));

        assertThat(roundUpOrSame(9, 10), equalTo(10));
        assertThat(roundUp(9, 10), equalTo(10));

        assertThat(roundUpOrSame(10, 10), equalTo(10));
        assertThat(roundUp(10, 10), equalTo(20));

        assertThat(roundUpOrSame(1, 5), equalTo(5));
        assertThat(roundUp(1, 5), equalTo(5));

        assertThat(roundUpOrSame(10, 5), equalTo(10));
        assertThat(roundUp(10, 5), equalTo(15));

        assertThat(roundUpOrSame(11, 5), equalTo(15));
        assertThat(roundUp(11, 5), equalTo(15));

        // Rounding by 0 returns original value
        assertThat(roundUpOrSame(11, 0), equalTo(11));
        assertThat(roundUp(11, 0), equalTo(11));
    }

    @Test
    public void testTimeRounding()
    {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        final ZoneId zone = ZoneId.of("America/New_York");

        ZonedDateTime time = ZonedDateTime.of(2014, 10, 25, 13, 40, 1, Duration.ofMillis(42).getNano(), zone);
        assertThat(formatter.format(time), equalTo("2014-10-25 13:40:01.042"));

        assertThat(formatter.format(time.with(nextOrSameMidnight)),
                equalTo("2014-10-26 00:00:00.000"));

        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.MILLIS, 1))),
                equalTo("2014-10-25 13:40:01.042"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.MILLIS, 50))),
                equalTo("2014-10-25 13:40:01.050"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.MILLIS, 500))),
                equalTo("2014-10-25 13:40:01.500"));

        // Truncating side-effect of rounding by 0
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.SECONDS, 0))),
                equalTo("2014-10-25 13:40:01.000"));

        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.SECONDS, 1))),
                equalTo("2014-10-25 13:40:02.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.SECONDS, 5))),
                equalTo("2014-10-25 13:40:05.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.SECONDS, 8))),
                equalTo("2014-10-25 13:40:08.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.SECONDS, 30))),
                equalTo("2014-10-25 13:40:30.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.SECONDS, 60))),
                equalTo("2014-10-25 13:41:00.000"));

        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.MINUTES, 1))),
                equalTo("2014-10-25 13:41:00.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.MINUTES, 5))),
                equalTo("2014-10-25 13:45:00.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.MINUTES, 30))),
                equalTo("2014-10-25 14:00:00.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.MINUTES, 60))),
                equalTo("2014-10-25 14:00:00.000"));

        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 1))),
                equalTo("2014-10-25 14:00:00.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2))),
                equalTo("2014-10-25 14:00:00.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 3))),
                equalTo("2014-10-25 15:00:00.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 5))),
                equalTo("2014-10-25 15:00:00.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 6))),
                equalTo("2014-10-25 18:00:00.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 12))),
                equalTo("2014-10-26 00:00:00.000"));

        // Truncating side-effect of rounding by 0
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.DAYS, 0))),
                equalTo("2014-10-25 00:00:00.000"));

        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.DAYS, 1))),
                equalTo("2014-10-26 00:00:00.000"));
        // Every 'other' day counts from 1, i.e. 1, 3, .., 27
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.DAYS, 2))),
                equalTo("2014-10-27 00:00:00.000"));
        // Treat "7 days" as "next Monday"
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.DAYS, 7))),
                equalTo("2014-10-27 00:00:00.000"));
        assertThat(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.DAYS, 7)).getDayOfWeek(),
                equalTo(DayOfWeek.MONDAY));

        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.MONTHS, 1))),
                equalTo("2014-11-01 00:00:00.000"));
        // Months counted from 1, so rounding by 2 gives 1, 3, ..9, 11
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.MONTHS, 2))),
                equalTo("2014-11-01 00:00:00.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.MONTHS, 6))),
                equalTo("2015-01-01 00:00:00.000"));

        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.YEARS, 1))),
                equalTo("2015-01-01 00:00:00.000"));
        assertThat(formatter.format(time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.YEARS, 2))),
                equalTo("2016-01-01 00:00:00.000"));
    }

    @Test
    public void testRandomRounding()
    {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (z, 'UTC' Z)");
        final ZonedDateTime start = ZonedDateTime.now();
        for (ChronoUnit unit : SUPPORTED_UNITS)
        {
            final int amount = (int)(Math.random()*60);
            final ZonedDateTime rd = start.with(zonedDateTimerRoundedToNextOrSame(unit, amount));
            System.out.println(formatter.format(start) + " rounded by " + amount + " " + unit + " -> " +
                               formatter.format(rd));
        }
    }

    @Test
    public void testRandomRoundingWithInstant()
    {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (z, 'UTC' Z)");
        final ZoneId zone = ZoneId.systemDefault();
        final Instant start = Instant.now();
        for (ChronoUnit unit : SUPPORTED_UNITS)
        {
            final int amount = (int)(Math.random()*60);
            final Instant rd = start.with(instanceRoundedToNextOrSame(unit, amount));
            System.out.println(formatter.format(ZonedDateTime.ofInstant(start, zone )) +
                               " rounded by " + amount + " " + unit + " -> " +
                               formatter.format(ZonedDateTime.ofInstant(rd, zone)));
        }
    }

    @Test
    public void testDST()
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (z, 'UTC' Z)");
        final ZoneId zone = ZoneId.of("America/New_York");

        // Switch into DST
        ZonedDateTime start = ZonedDateTime.of(2014, 3, 8, 23, 0, 0, 0, zone);
        ZonedDateTime time = start;

        assertThat(formatter.format(time), equalTo("2014-03-08 23:00:00 (EST, UTC -0500)"));
        ZonedDateTime rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-03-09 00:00:00 (EST, UTC -0500)"));

        time = time.plusHours(1);
        assertThat(formatter.format(time), equalTo("2014-03-09 00:00:00 (EST, UTC -0500)"));
        rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-03-09 00:00:00 (EST, UTC -0500)"));

        time = time.plusHours(1);
        assertThat(formatter.format(time), equalTo("2014-03-09 01:00:00 (EST, UTC -0500)"));
        rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-03-09 03:00:00 (EDT, UTC -0400)"));

        time = time.plusHours(1);
        assertThat(formatter.format(time), equalTo("2014-03-09 03:00:00 (EDT, UTC -0400)"));
        rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-03-09 04:00:00 (EDT, UTC -0400)"));

        time = time.plusHours(1);
        assertThat(formatter.format(time), equalTo("2014-03-09 04:00:00 (EDT, UTC -0400)"));
        rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-03-09 04:00:00 (EDT, UTC -0400)"));

        time = time.plusHours(1);
        assertThat(formatter.format(time), equalTo("2014-03-09 05:00:00 (EDT, UTC -0400)"));
        rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-03-09 06:00:00 (EDT, UTC -0400)"));

        // Leave DST
        start = ZonedDateTime.of(2014, 11, 1, 23, 0, 0, 0, zone);
        time = start;

        assertThat(formatter.format(time), equalTo("2014-11-01 23:00:00 (EDT, UTC -0400)"));
        rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-11-02 00:00:00 (EDT, UTC -0400)"));

        time = time.plusHours(1);
        assertThat(formatter.format(time), equalTo("2014-11-02 00:00:00 (EDT, UTC -0400)"));
        rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-11-02 00:00:00 (EDT, UTC -0400)"));

        time = time.plusHours(1);
        assertThat(formatter.format(time), equalTo("2014-11-02 01:00:00 (EDT, UTC -0400)"));
        rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-11-02 01:00:00 (EST, UTC -0500)"));

        time = time.plusHours(1);
        assertThat(formatter.format(time), equalTo("2014-11-02 01:00:00 (EST, UTC -0500)"));
        rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-11-02 01:00:00 (EST, UTC -0500)"));

        time = time.plusHours(1);
        assertThat(formatter.format(time), equalTo("2014-11-02 02:00:00 (EST, UTC -0500)"));
        rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-11-02 02:00:00 (EST, UTC -0500)"));

        time = time.plusHours(1);
        assertThat(formatter.format(time), equalTo("2014-11-02 03:00:00 (EST, UTC -0500)"));
        rd = time.with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.HOURS, 2));
        assertThat(formatter.format(rd), equalTo("2014-11-02 04:00:00 (EST, UTC -0500)"));
    }

    @Test
    public void testUnsupportedRounding()
    {
        try
        {
            ZonedDateTime.now().with(zonedDateTimerRoundedToNextOrSame(ChronoUnit.WEEKS, 2));
            fail("!?");
        }
        catch (RuntimeException ex)
        {
            // NOP   System.out.println("Detected: " + ex.getMessage());
        }
    }
}
