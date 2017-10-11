/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.phoebus.util.time.TimeDuration;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.junit.Test;

/**
 *
 * @author carcassi
 */
public class TimestampTest {

    public TimestampTest() {
    }

    @Test
    public void time1() {
        Instant time = Instant.ofEpochSecond(100, 10000000);
        assertThat(time.getEpochSecond(), equalTo(100L));
        assertThat(time.getNano(), equalTo(10000000));
    }

    @Test
    public void ofDate1() {
        Date date = new Date(123456789);
        Instant time = Instant.ofEpochMilli(date.getTime());
        assertThat(time.getEpochSecond(), equalTo(123456L));
        assertThat(time.getNano(), equalTo(789000000));
    }

    @Test
    public void ofDate2() {
        Date date = new Date(-123456789);
        Instant time = Instant.ofEpochMilli(date.getTime());
        assertThat(new Date(time.toEpochMilli()), equalTo(date));
        assertThat(time.getEpochSecond(), equalTo(-123457L));
        assertThat(time.getNano(), equalTo(211000000));
    }

    @Test
    public void toDate1() {
        Instant time = Instant.ofEpochSecond(123456, 789000000);
        assertThat(new Date(time.toEpochMilli()), equalTo(new Date(123456789)));
    }

    @Test
    public void plus1() {
        Instant time = Instant.ofEpochSecond(0, 0);
        Instant newTime = time.plus(Duration.ofMillis(100));
        assertThat(newTime, equalTo(Instant.ofEpochSecond(0, 100000000)));
    }

    @Test
    public void plus2() {
        Instant time = Instant.ofEpochSecond(100, 100000000);
        Instant newTime = time.plus(Duration.ofNanos(999000000));
        assertThat(newTime, equalTo(Instant.ofEpochSecond(101, 99000000)));
    }

    @Test
    public void plus3() {
        Instant time = Instant.ofEpochSecond(100, 750000000);
        Instant newTime = time.plus(TimeDuration.ofSeconds(5.750));
        assertThat(newTime, equalTo(Instant.ofEpochSecond(106, 500000000)));
    }

    @Test
    public void plus4() {
        Instant time = Instant.ofEpochSecond(100, 750000000);
        Instant newTime = time.plus(TimeDuration.ofSeconds(-5.750));
        assertThat(newTime, equalTo(Instant.ofEpochSecond(95, 000000000)));
    }

    @Test
    public void minus1() {
        Instant time = Instant.ofEpochSecond(0, 0);
        Instant newTime = time.minus(Duration.ofMillis(100));
        assertThat(newTime, equalTo(Instant.ofEpochSecond(-1, 900000000)));
    }

    @Test
    public void minus2() {
        Instant time = Instant.ofEpochSecond(0, 0);
        Instant newTime = time.minus(Duration.ofMillis(100));
        assertThat(newTime, equalTo(Instant.ofEpochSecond(-1, 900000000)));
    }

    @Test
    public void minus3() {
        Instant time = Instant.ofEpochSecond(0, 0);
        Instant newTime = time.minus(Duration.ofMillis(100));
        assertThat(newTime, equalTo(Instant.ofEpochSecond(-1, 900000000)));
    }

    @Test
    public void durationFrom1() {
        Instant reference = Instant.now();
        assertThat(Duration.between(reference, reference.plus(Duration.ofNanos(10))), equalTo(Duration.ofNanos(10)));
    }

    @Test
    public void durationFrom2() {
        Instant reference = Instant.now();
        assertThat(Duration.between(reference, reference.minus(Duration.ofNanos(10))), equalTo(Duration.ofNanos(-10)));
    }

    @Test
    public void durationFrom3() {
        Instant reference = Instant.ofEpochSecond(10, 500000000);
        assertThat(Duration.between(reference, reference.plus(Duration.ofMillis(600))), equalTo(Duration.ofMillis(600)));
    }

    @Test
    public void durationFrom4() {
        Instant reference = Instant.ofEpochSecond(10, 500000000);
        assertThat(Duration.between(reference, reference.minus(Duration.ofMillis(600))), equalTo(Duration.ofMillis(-600)));
    }

    @Test
    public void durationBetween1() {
        Instant reference = Instant.now();
        assertThat(Duration.between(reference, reference.plus(Duration.ofNanos(10))), equalTo(Duration.ofNanos(10)));
    }

    @Test
    public void durationBetween2() {
        Instant reference = Instant.now();
        assertThat(Duration.between(reference.minus(Duration.ofNanos(10)), reference), equalTo(Duration.ofNanos(10)));
    }

    @Test
    public void durationBetween3() {
        Instant reference = Instant.ofEpochSecond(10, 500000000);
        assertThat(Duration.between(reference, reference.plus(Duration.ofMillis(600))), equalTo(Duration.ofMillis(600)));
    }

    @Test
    public void durationBetween4() {
        Instant reference = Instant.ofEpochSecond(10, 500000000);
        assertThat(Duration.between(reference.minus(Duration.ofMillis(600)), reference), equalTo(Duration.ofMillis(600)));
    }

//    Removing this test since I am not really sure how useful the string representation of a time instant and a double is
//    If needed the functionality can be added via a helper class
//
//    @Test
//    public void toString1() {
//        Instant timestamp = Instant.ofEpochSecond(0, 10000000);
//        assertThat(TimeInstant.doubleSeconds(timestamp), equalTo("0.010000000"));
//    }
//
//    @Test
//    public void toString2() {
//        Instant timestamp = Instant.ofEpochSecond(1, 234500000);
//        assertThat(timestamp.toString(), equalTo("1.234500000"));
//    }
//
//    @Test
//    public void toString3() {
//        Instant timestamp = Instant.ofEpochSecond(1234, 567890000);
//        assertThat(timestamp.toString(), equalTo("1234.567890000"));
//    }
//
//    @Test
//    public void toString4() {
//        Instant timestamp = Instant.ofEpochSecond(1234, 100);
//        assertThat(timestamp.toString(), equalTo("1234.000000100"));
//    }

}