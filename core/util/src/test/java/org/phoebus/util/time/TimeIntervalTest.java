/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.phoebus.util.time.TimeInterval;

import java.time.Instant;

import org.junit.Test;

/**
 *
 * @author carcassi
 */
public class TimeIntervalTest {

    public TimeIntervalTest() {
    }

    @Test
    public void interval1() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0));
        assertThat(interval.getStart(), equalTo(Instant.ofEpochSecond(0, 0)));
        assertThat(interval.getEnd(), equalTo(Instant.ofEpochSecond(3600, 0)));
    }

    @Test
    public void interval2() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(3600, 0), Instant.ofEpochSecond(7200, 0));
        assertThat(interval.getStart(), equalTo(Instant.ofEpochSecond(3600, 0)));
        assertThat(interval.getEnd(), equalTo(Instant.ofEpochSecond(7200, 0)));
    }

    @Test
    public void interval3() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), null);
        assertThat(interval.getStart(), equalTo(Instant.ofEpochSecond(0, 0)));
        assertThat(interval.getEnd(), nullValue());
    }

    @Test
    public void interval4() {
        TimeInterval interval = TimeInterval.between(null, Instant.ofEpochSecond(0, 0));
        assertThat(interval.getStart(), nullValue());
        assertThat(interval.getEnd(), equalTo(Instant.ofEpochSecond(0, 0)));
    }

    @Test
    public void equals1() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0));
        assertThat(interval, equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0))));
    }

    @Test
    public void equals2() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 1), Instant.ofEpochSecond(3600, 0));
        assertThat(interval, not(equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0)))));
    }

    @Test
    public void equals3() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 1));
        assertThat(interval, not(equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0)))));
    }

    @Test
    public void equals4() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), null);
        assertThat(interval, equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), null)));
    }

    @Test
    public void equals5() {
        TimeInterval interval = TimeInterval.between(null, Instant.ofEpochSecond(0, 0));
        assertThat(interval, equalTo(TimeInterval.between(null, Instant.ofEpochSecond(0, 0))));
    }

    @Test
    public void contains1() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 1));
        assertThat(interval.contains(Instant.ofEpochSecond(3,0)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(0,110)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(3600,0)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(-1,110)), is(false));
        assertThat(interval.contains(Instant.ofEpochSecond(3600,2)), is(false));
    }

    @Test
    public void contains2() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), null);
        assertThat(interval.contains(Instant.ofEpochSecond(-3600,2)), is(false));
        assertThat(interval.contains(Instant.ofEpochSecond(-1,110)), is(false));
        assertThat(interval.contains(Instant.ofEpochSecond(0,110)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(3,0)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(3600,0)), is(true));
    }

    @Test
    public void contains3() {
        TimeInterval interval = TimeInterval.between(null, Instant.ofEpochSecond(0, 0));
        assertThat(interval.contains(Instant.ofEpochSecond(-3600,2)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(-1,110)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(0,110)), is(false));
        assertThat(interval.contains(Instant.ofEpochSecond(3,0)), is(false));
        assertThat(interval.contains(Instant.ofEpochSecond(3600,0)), is(false));
    }
}