/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;

import org.junit.Test;
/**
 * TODO additional tests are needed to verify all the chrono types are properly handled.
 * @author shroffk
 */
public class TimeParserTest {

    @Test
    public void getNow() {
        Instant ts = TimeParser.getInstant("now");
        assertTrue("Failed to obtain Timestamp corresponding to now ",
                ts != null && ts instanceof Instant);
    }

    /**
     * Test the times Duration ← relative
     */
    @Test
    public void getDuration() {
        // "last min", "last hour", "last day", "last week"
        Duration lastMin = TimeParser.parseDuration("1 min");
        assertEquals("Failed to get Duration for last min", Duration.ofSeconds(60),
                lastMin);
        Duration lastHour = TimeParser.parseDuration("1 hour");
        assertEquals("Failed to get Duration for last hour",
                Duration.ofHours(1), lastHour);
        // "last 5 mins", "last 5 hours", "last 5 days"
        TemporalAmount last5Min = TimeParser.parseDuration(" 5 mins");
        assertEquals("Failed to get Duration for last 5 mins",
                Duration.ofMinutes(5), last5Min);
        TemporalAmount last5Hour = TimeParser.parseDuration(" 5 hours");
        assertEquals("Failed to get Duration for last 5 hours",
                Duration.ofHours(5), last5Hour);
        Duration last5Day = TimeParser.parseDuration(" 5 days");
        assertEquals("Failed to get Duration for last 5 days",
                60 * 60 * 24 * 5, last5Day.getSeconds());
    }

    @Test
    public void parse() {
        Instant now = Instant.now();
        // create time interval using string to define relative start and end
        // times
        TimeRelativeInterval interval = TimeRelativeInterval.of(TimeParser.parseTemporalAmount("1 min"), now);
        assertEquals(now.minusSeconds(60), interval.toAbsoluteInterval().getStart());
        assertEquals(now, interval.toAbsoluteInterval().getEnd());
    }

    /**
     * Test time strings i.e. 4 hours 3 mins 2 secs ago
     */
    @Test
    public void parseCompositeTimeString() {
        TemporalAmount last5Mins30Secs = TimeParser.parseDuration("5 mins 30 secs");
        assertEquals("Failed to get Duration for last 5 mins",
                Duration.ofMinutes(5).plusSeconds(30), last5Mins30Secs);
        TemporalAmount last3Hours5Mins30Secs = TimeParser.parseDuration("3 hours 5 mins 30 secs");
        assertEquals("Failed to get Duration for last 5 mins",
                Duration.ofHours(3).plusMinutes(5).plusSeconds(30), last3Hours5Mins30Secs);
    }


    /**
     * Test the creation parsing of string representations of time to create {@link TimeRelativeInterval}
     *
     * The below tests create an interval which represents a single month.
     *
     */
    @Test
    public void parseRelativeInterval() {
        // Create an interval for January
        TimeRelativeInterval interval = TimeRelativeInterval.of(TimeParser.parseTemporalAmount("1 month"), LocalDateTime
                .parse("2011-02-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC));

        // Check jan it is 31 days
        TimeInterval jan = interval.toAbsoluteInterval();
        assertEquals(31L, Duration.between(jan.getStart(), jan.getEnd()).toDays());

        // Check February is 28 days
        TimeInterval feb = TimeRelativeInterval
                .of(TimeParser.parseTemporalAmount("1 month"), LocalDateTime
                        .parse("2011-03-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC))
                .toAbsoluteInterval();
        assertEquals(28L, Duration.between(feb.getStart(), feb.getEnd()).toDays());

        // Check February is 29 days because it is a leap year
        TimeInterval leapFeb = TimeRelativeInterval
                .of(TimeParser.parseTemporalAmount("1 month"), LocalDateTime
                        .parse("2012-03-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC))
                .toAbsoluteInterval();
        assertEquals(29L, Duration.between(leapFeb.getStart(), leapFeb.getEnd()).toDays());
    }


    @Test
    public void testParseDuration()
    {
        TemporalAmount amount = TimeParser.parseDuration("3 days");
        final long seconds = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).plus(amount).toEpochSecond(ZoneOffset.UTC);
        assertEquals(3*24*60*60, seconds);
    }

    @Test
    public void testParseTemporalAmount()
    {
        // 3 days are parsed in P1Y, 1 year
        TemporalAmount amount = TimeParser.parseTemporalAmount("3 days");
        System.out.println(amount);

        final long seconds = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).plus(amount).toEpochSecond(ZoneOffset.UTC);
        assertEquals(3*24*60*60, seconds);
    }
}
