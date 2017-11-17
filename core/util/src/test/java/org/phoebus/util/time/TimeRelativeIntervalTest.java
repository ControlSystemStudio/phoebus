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
 * @author carcassi
 */
public class TimeRelativeIntervalTest {

    public TimeRelativeIntervalTest() {
    }

    @Test
    public void interval1() {
        TimeRelativeInterval interval = TimeRelativeInterval.of(Instant.ofEpochSecond(0, 0),
                Instant.ofEpochSecond(3600, 0));
        assertThat(interval.toAbsoluteInterval(Instant.now()),
                equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0))));
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

    @Test
    public void relativeIntervalinDays() {
        // Create an interval for January
        TimeRelativeInterval interval = 
                TimeRelativeInterval.of(
                        LocalDateTime.parse("2011-01-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC),
                        ofMonths(1));
        // Check jan it is 31 days
        TimeInterval jan = interval.toAbsoluteInterval();
        assertEquals(31L, Duration.between(jan.getStart(), jan.getEnd()).toDays());
        interval = 
                TimeRelativeInterval.of(
                        LocalDateTime.parse("2011-02-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC),
                        ofMonths(1));
        // Check February is 28 days
        TimeInterval feb = interval.toAbsoluteInterval();
        assertEquals(28L, Duration.between(feb.getStart(), feb.getEnd()).toDays());
        interval = 
                TimeRelativeInterval.of(
                        LocalDateTime.parse("2012-02-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC),
                        ofMonths(1));
        // Check February is 29 days because it is a leap year
        TimeInterval leapFeb = interval.toAbsoluteInterval();
        assertEquals(29L, Duration.between(leapFeb.getStart(), leapFeb.getEnd()).toDays());
    }

}