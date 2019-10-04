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
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;

import org.junit.Test;
/**
 * TODO additional tests are needed to verify all the chrono types are properly handled.
 * @author shroffk
 */
@SuppressWarnings("nls")
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
        TemporalAmount amount = TimeParser.parseTemporalAmount("3 days");
        long seconds = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).plus(amount).toEpochSecond(ZoneOffset.UTC);
        assertEquals(3*24*60*60, seconds);

        amount = TimeParser.parseTemporalAmount("3 days 20 mins 10 sec");
        seconds = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).plus(amount).toEpochSecond(ZoneOffset.UTC);
        assertEquals(3*24*60*60 + 20*60 + 10, seconds);

        amount = TimeParser.parseTemporalAmount("now");
        seconds = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).plus(amount).toEpochSecond(ZoneOffset.UTC);
        assertEquals(0, seconds);

        // Month triggers use of period, which only keeps y/m/d
        // 1 week adds 7 days
        amount = TimeParser.parseTemporalAmount("1 month 1 weeks 3 days 20 mins 10 sec");
        assertEquals(amount, Period.of(0, 1, 10));

        amount = TimeParser.parseTemporalAmount("1 month 1 day");
        assertEquals(amount, Period.of(0, 1, 1));

        amount = TimeParser.parseTemporalAmount("1 mo");
        assertEquals(amount, Period.of(0, 1, 0));

        // Just the unit name implies "1 xxx"
        amount = TimeParser.parseTemporalAmount("month");
        assertEquals(amount, Period.of(0, 1, 0));

        // 60 days span more than a month,
        // but since "month" is not mentioned,
        // it's considered 60 exact days
        // (implying 24 hour days)
        amount = TimeParser.parseTemporalAmount("60 days");
        assertEquals(amount, Duration.ofDays(60));
    }

    @Test
    public void testParseFactionalTemporalAmount()
    {
        // Just having ".0" used to be an error before fractions were supported
        TemporalAmount amount = TimeParser.parseTemporalAmount("1.000 hour");
        System.out.println(TimeParser.format(amount));
        Instant instant = Instant.ofEpochSecond(0).plus(amount);
        assertEquals(60*60, instant.getEpochSecond());
        assertEquals(0, instant.getNano());

        amount = TimeParser.parseTemporalAmount("1.5 hour");
        System.out.println(TimeParser.format(amount));
        instant = Instant.ofEpochSecond(0).plus(amount);
        assertEquals((60+30)*60, instant.getEpochSecond());
        assertEquals(0, instant.getNano());

        amount = TimeParser.parseTemporalAmount("1.5 hour 6.5 minutes");
        System.out.println(TimeParser.format(amount));
        instant = Instant.ofEpochSecond(0).plus(amount);
        assertEquals((60+30+6)*60+30, instant.getEpochSecond());
        assertEquals(0, instant.getNano());

        amount = TimeParser.parseTemporalAmount("6.5 minutes");
        System.out.println(TimeParser.format(amount));
        instant = Instant.ofEpochSecond(0).plus(amount);
        assertEquals(6*60+30, instant.getEpochSecond());
        assertEquals(0, instant.getNano());

        amount = TimeParser.parseTemporalAmount("3.5 seconds");
        System.out.println(TimeParser.format(amount));
        instant = Instant.ofEpochSecond(0).plus(amount);
        assertEquals(3, instant.getEpochSecond());
        assertEquals(500000000, instant.getNano());

        amount = TimeParser.parseTemporalAmount("1.5 days");
        System.out.println(TimeParser.format(amount));
        instant = Instant.ofEpochSecond(0).plus(amount);
        assertEquals((24+12)*60*60, instant.getEpochSecond());
        assertEquals(0, instant.getNano());

        // As soon as the time span enters "weeks", it's handled
        // as a 'Period' which is only good down to days.
        // So 1.5 weeks = 7 + 3.5 days is rounded to 11 days, not 10.5
        amount = TimeParser.parseTemporalAmount("1.5 weeks");
        System.out.println(TimeParser.format(amount));
        assertEquals(Period.of(0,  0,  11), amount);

        // 1.5 month = 1 month, 2 weeks=14days
        amount = TimeParser.parseTemporalAmount("1.5 months");
        System.out.println(TimeParser.format(amount));
        assertEquals(Period.of(0,  1,  14), amount);

        // 1.5 years = 1 year, 6 months (not divving up further into days)
        amount = TimeParser.parseTemporalAmount("1.5 years");
        System.out.println(TimeParser.format(amount));
        assertEquals(Period.of(1,  6,  0), amount);
    }

    @Test
    public void testFormatTemporalAmount()
    {
        String text = TimeParser.format(Duration.ofHours(2));
        assertEquals("2 hours", text);

        text = TimeParser.format(Period.of(1, 2, 3));
        assertEquals("1 year 2 months 3 days", text);

        text = TimeParser.format(Duration.ofSeconds(2*24*60*60 + 1*60*60 + 10, 123000000L));
        assertEquals("2 days 1 hour 10 seconds 123 ms", text);

        text = TimeParser.format(Duration.ofSeconds(0));
        assertEquals("now", text);

        text = TimeParser.format(Period.ZERO);
        assertEquals("now", text);
    }
}
