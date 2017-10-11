/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeRelativeInterval;

import java.time.Instant;

import org.junit.Test;

/**
 *
 * @author carcassi
 */
public class TimeRelativeIntervalTest {

    public TimeRelativeIntervalTest() {
    }

    // Trasform to absolute/relative?
    // create aa/ar/ra/rr
    @Test
    public void interval1() {
        TimeRelativeInterval interval = TimeRelativeInterval.of(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0));
        assertThat(interval.isIntervalAbsolute(), equalTo(true));
        assertThat(interval.getAbsoluteStart(), equalTo(Instant.ofEpochSecond(0, 0)));
        assertThat(interval.getAbsoluteEnd(), equalTo(Instant.ofEpochSecond(3600, 0)));
        assertThat(interval.toAbsoluteInterval(Instant.now()), equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0))));
    }

    @Test
    public void interval2() {
        TimeRelativeInterval interval = TimeRelativeInterval.of(Instant.ofEpochSecond(0, 0), null);
        assertThat(interval.isIntervalAbsolute(), equalTo(true));
        assertThat(interval.getAbsoluteStart(), equalTo(Instant.ofEpochSecond(0, 0)));
        assertThat(interval.getAbsoluteEnd(), nullValue());
        assertThat(interval.toAbsoluteInterval(Instant.now()), equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), null)));
    }

    @Test
    public void interval3() {
        TimeRelativeInterval interval = TimeRelativeInterval.of(null, Instant.ofEpochSecond(0, 0));
        assertThat(interval.isIntervalAbsolute(), equalTo(true));
        assertThat(interval.getAbsoluteStart(), nullValue());
        assertThat(interval.getAbsoluteEnd(), equalTo(Instant.ofEpochSecond(0, 0)));
        assertThat(interval.toAbsoluteInterval(Instant.now()), equalTo(TimeInterval.between(null, Instant.ofEpochSecond(0, 0))));
    }
}