/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static java.time.Duration.*;
import static java.time.Period.*;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeRelativeInterval;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

/**
 *
 * @author carcassi, shroffk
 */
public class TimeRelativeIntervalTest {

    public TimeRelativeIntervalTest() {
    }

    /**
     * Absolute start and absolute end
     */
    @Test
    public void interval1() {
        TimeRelativeInterval interval = TimeRelativeInterval.of(Instant.ofEpochSecond(0, 0),
                Instant.ofEpochSecond(3600, 0));
        assertThat(interval.toAbsoluteInterval(Instant.now()),
                equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0))));
    }

    /**
     * absolute start and relative end
     */
    @Test
    public void interval2() {
        // relative end of 10 ns after start
        TimeRelativeInterval interval = TimeRelativeInterval.of(Instant.ofEpochSecond(0, 0), Duration.ofNanos(10));
        assertThat(interval.toAbsoluteInterval(),
                equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(0, 10))));
        // relatice end of 10 ms after start
        interval = TimeRelativeInterval.of(Instant.ofEpochSecond(0, 0), Duration.ofMillis(10));
        assertThat(interval.toAbsoluteInterval(),
                equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(0, 10000000))));
        // relative end of 10 s after start
        interval = TimeRelativeInterval.of(Instant.ofEpochSecond(0, 0), Duration.ofSeconds(10));
        assertThat(interval.toAbsoluteInterval(),
                equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(10, 0))));
        // relative end of 10 mins after start
        interval = TimeRelativeInterval.of(Instant.ofEpochSecond(0, 0), Duration.ofMinutes(10));
        assertThat(interval.toAbsoluteInterval(Instant.now()),
                equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(10 * 60, 0))));
        // relative end of 10 hours after start
        interval = TimeRelativeInterval.of(Instant.ofEpochSecond(0, 0), Duration.ofHours(10));
        assertThat(interval.toAbsoluteInterval(Instant.now()),
                equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(10 * 60 * 60, 0))));
    }

    /**
     * relative start and absolute end
     */
    @Test
    public void interval3() {
        // relative start of 10 ns before now
        Instant now = Instant.now();
        TimeRelativeInterval interval = TimeRelativeInterval.of(Duration.ofNanos(10), now);
        assertThat(interval.toAbsoluteInterval(), equalTo(TimeInterval.between(now.minusNanos(10), now)));
        // relative start of 10 ms before now
        interval = TimeRelativeInterval.of(Duration.ofMillis(10), now);
        assertThat(interval.toAbsoluteInterval(), equalTo(TimeInterval.between(now.minusMillis(10), now)));
        // relative start of 10 s before now
        interval = TimeRelativeInterval.of(Duration.ofSeconds(10), now);
        assertThat(interval.toAbsoluteInterval(), equalTo(TimeInterval.between(now.minusSeconds(10), now)));
        // relative start of 10 mins before now
        interval = TimeRelativeInterval.of(Duration.ofMinutes(10), now);
        assertThat(interval.toAbsoluteInterval(), equalTo(TimeInterval.between(now.minusSeconds(10*60), now)));
        // relative start of 10 hours before now
        interval = TimeRelativeInterval.of(Duration.ofHours(10), now);
        assertThat(interval.toAbsoluteInterval(), equalTo(TimeInterval.between(now.minusSeconds(10*60*60), now)));
    }

    @Test
    public void relativeIntervalinMilliSecs() {
        TimeRelativeInterval interval = TimeRelativeInterval.of(ofMillis(15), ofMillis(5));
        Instant now = Instant.now();
        TimeInterval timeInterval = interval.toAbsoluteInterval(now);
        TimeInterval expectedTimeInterval = TimeInterval.between(now.minus(ofMillis(15)), now.minus(ofMillis(5)));
        assertEquals(expectedTimeInterval, timeInterval);
    }

    @Test
    public void relativeIntervalinSecs() {
        TimeRelativeInterval interval = TimeRelativeInterval.of(ofSeconds(5), ofSeconds(3));
        Instant now = Instant.now();
        TimeInterval timeInterval = interval.toAbsoluteInterval(now);
        TimeInterval expectedTimeInterval = TimeInterval.between(now.minus(ofSeconds(5)), now.minus(ofSeconds(3)));
        assertEquals(expectedTimeInterval, timeInterval);
    }

    @Test
    public void relativeIntervalinMins() {
        TimeRelativeInterval interval = TimeRelativeInterval.of(ofMinutes(5), ofMinutes(3));
        Instant now = Instant.now();
        TimeInterval timeInterval = interval.toAbsoluteInterval(now);
        TimeInterval expectedTimeInterval = TimeInterval.between(now.minus(ofMinutes(5)), now.minus(ofMinutes(3)));
        assertEquals(expectedTimeInterval, timeInterval);
    }

    @Test
    public void relativeIntervalinHours() {
        TimeRelativeInterval interval = TimeRelativeInterval.of(ofHours(5), ofHours(3));
        Instant now = Instant.now();
        TimeInterval timeInterval = interval.toAbsoluteInterval(now);
        TimeInterval expectedTimeInterval = TimeInterval.between(now.minus(ofHours(5)), now.minus(ofHours(3)));
        assertEquals(expectedTimeInterval, timeInterval);
    }

    /**
     * The {@link TimeRelativeInterval} is defined with both the start and the
     * end as relative definition. The below tests create an interval which
     * represents a single month.
     * 
     */
    @Test
    public void relativeIntervalinDays1() {
        // Create an interval for January
        TimeRelativeInterval interval = TimeRelativeInterval.of(ofMonths(1), ofMonths(0));
        // Check jan it is 31 days
        TimeInterval jan = interval.toAbsoluteInterval(LocalDateTime
                .parse("2011-02-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC));
        assertEquals(31L, Duration.between(jan.getStart(), jan.getEnd()).toDays());

        // Check February is 28 days
        TimeInterval feb = interval.toAbsoluteInterval(LocalDateTime
                .parse("2011-03-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC));
        assertEquals(28L, Duration.between(feb.getStart(), feb.getEnd()).toDays());

        // Check February is 29 days because it is a leap year
        TimeInterval leapFeb = interval.toAbsoluteInterval(LocalDateTime
                .parse("2012-03-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC));
        assertEquals(29L, Duration.between(leapFeb.getStart(), leapFeb.getEnd()).toDays());
    }

    public void relativeIntervalinDays2() {
        // Create an interval for January
        TimeRelativeInterval interval = TimeRelativeInterval.of(LocalDateTime
                .parse("2011-01-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC),
                ofMonths(1));
        // Check jan it is 31 days
        TimeInterval jan = interval.toAbsoluteInterval();
        assertEquals(31L, Duration.between(jan.getStart(), jan.getEnd()).toDays());
        interval = TimeRelativeInterval.of(LocalDateTime
                .parse("2011-02-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC),
                ofMonths(1));
        // Check February is 28 days
        TimeInterval feb = interval.toAbsoluteInterval();
        assertEquals(28L, Duration.between(feb.getStart(), feb.getEnd()).toDays());
        interval = TimeRelativeInterval.of(LocalDateTime
                .parse("2012-02-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC),
                ofMonths(1));
        // Check February is 29 days because it is a leap year
        TimeInterval leapFeb = interval.toAbsoluteInterval();
        assertEquals(29L, Duration.between(leapFeb.getStart(), leapFeb.getEnd()).toDays());
    }

}