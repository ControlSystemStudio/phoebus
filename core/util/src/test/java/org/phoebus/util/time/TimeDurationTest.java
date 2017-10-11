/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;


import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.phoebus.util.time.TimeDuration;
import static org.phoebus.util.time.TimeDuration.*;

import java.time.Duration;

import org.junit.Test;

/**
 *
 * @author carcassi
 */
public class TimeDurationTest {

    public TimeDurationTest() {
    }

    // Test factory methods

    @Test
    public void nanos1() {
        Duration duration = Duration.ofNanos(100L);
        assertThat(duration.getNano(), equalTo(100));
        assertThat(duration.getSeconds(), equalTo(0L));
    }

    @Test
    public void nanos2() {
        Duration duration = Duration.ofNanos(1234567890L);
        assertThat(duration.getNano(), equalTo(234567890));
        assertThat(duration.getSeconds(), equalTo(1L));
    }

    @Test
    public void nanos3() {
        Duration duration = Duration.ofNanos(123456789012L);
        assertThat(duration.getNano(), equalTo(456789012));
        assertThat(duration.getSeconds(), equalTo(123L));
    }

    @Test
    public void nanos4() {
        Duration duration = Duration.ofNanos(-1234567890L);
        assertThat(duration.getNano(), equalTo(765432110));
        assertThat(duration.getSeconds(), equalTo(-2L));
    }

    @Test
    public void ms1() {
        Duration duration = Duration.ofMillis(100);
        assertThat(duration.getNano(), equalTo(100000000));
        assertThat(duration.getSeconds(), equalTo(0L));
    }

    @Test
    public void ms2() {
        Duration duration = Duration.ofMillis(12345);
        assertThat(duration.getNano(), equalTo(345000000));
        assertThat(duration.getSeconds(), equalTo(12L));
    }

    @Test
    public void ms3() {
        Duration duration = Duration.ofMillis(-12345);
        assertThat(duration.getNano(), equalTo(655000000));
        assertThat(duration.getSeconds(), equalTo(-13L));
    }

    @Test
    public void sec1() {
        Duration duration = TimeDuration.ofSeconds(1.0);
        assertThat(duration.getNano(), equalTo(0));
        assertThat(duration.getSeconds(), equalTo(1L));
    }

    @Test
    public void sec2() {
        Duration duration = TimeDuration.ofSeconds(0.123456789);
        assertThat(duration.getNano(), equalTo(123456789));
        assertThat(duration.getSeconds(), equalTo(0L));
    }

    @Test
    public void sec3() {
        Duration duration = TimeDuration.ofSeconds(-1.23456789);
        assertThat(duration.getNano(), equalTo(765432110));
        assertThat(duration.getSeconds(), equalTo(-2L));
    }

    @Test
    public void min1() {
        Duration duration = TimeDuration.ofMinutes(1.0);
        assertThat(duration.getNano(), equalTo(0));
        assertThat(duration.getSeconds(), equalTo(60L));
    }

    @Test
    public void min2() {
        Duration duration = TimeDuration.ofMinutes(0.123456789);
        assertThat(duration.getNano(), equalTo(407407340));
        assertThat(duration.getSeconds(), equalTo(7L));
    }

    @Test
    public void min3() {
        Duration duration = TimeDuration.ofMinutes(-1.23456789);
        assertThat(duration.getNano(), equalTo(925926601));
        assertThat(duration.getSeconds(), equalTo(-75L));
    }

    @Test
    public void hour1() {
        Duration duration = TimeDuration.ofHours(1.0);
        assertThat(duration.getNano(), equalTo(0));
        assertThat(duration.getSeconds(), equalTo(3600L));
    }

    @Test
    public void hour2() {
        Duration duration = TimeDuration.ofHours(0.123456789);
        assertThat(duration.getNano(), equalTo(444440399));
        assertThat(duration.getSeconds(), equalTo(444L));
    }

    @Test
    public void hour3() {
        Duration duration = TimeDuration.ofHours(-1.23456789);
        assertThat(duration.getNano(), equalTo(555596001));
        assertThat(duration.getSeconds(), equalTo(-4445L));
    }

    @Test
    public void hz1() {
        Duration duration = TimeDuration.ofHertz(1.0);
        assertThat(duration.getNano(), equalTo(0));
        assertThat(duration.getSeconds(), equalTo(1L));
    }

    @Test
    public void hz2() {
        Duration duration = TimeDuration.ofHertz(100.0);
        assertThat(duration.getNano(), equalTo(10000000));
        assertThat(duration.getSeconds(), equalTo(0L));
    }

    @Test
    public void hz3() {
        Duration duration = TimeDuration.ofHertz(0.123456789);
        assertThat(duration.getNano(), equalTo(100000073));
        assertThat(duration.getSeconds(), equalTo(8L));
    }

    // Test equality

    @Test
    public void equals1() {
        Duration duration = Duration.ofNanos(1000000);
        assertThat(duration, equalTo(Duration.ofMillis(1)));
        assertThat(duration, equalTo(TimeDuration.ofSeconds(0.001)));
        assertThat(duration, equalTo(TimeDuration.ofMinutes(0.0000166666666667)));
        assertThat(duration, equalTo(TimeDuration.ofHours(0.0000002777777778)));
        assertThat(duration, not(equalTo(Duration.ofMillis(0))));
    }

    @Test
    public void equals2() {
        Duration duration = Duration.ofNanos(1000000000);
        assertThat(duration, equalTo(Duration.ofMillis(1000)));
        assertThat(duration, equalTo(Duration.ofSeconds(1)));
        assertThat(duration, equalTo(TimeDuration.ofMinutes(0.0166666666667)));
        assertThat(duration, equalTo(TimeDuration.ofHours(0.0002777777778)));
        assertThat(duration, not(equalTo(Duration.ofMillis(0))));
    }

    @Test
    public void equals3() {
        Duration duration = Duration.ofNanos(60000000000L);
        assertThat(duration, equalTo(Duration.ofMillis(60000)));
        assertThat(duration, equalTo(Duration.ofSeconds(60)));
        assertThat(duration, equalTo(Duration.ofMinutes(1)));
        assertThat(duration, equalTo(TimeDuration.ofHours(0.0166666666667)));
    }

    @Test
    public void equals4() {
        Duration duration = Duration.ofNanos(3600000000000L);
        assertThat(duration, equalTo(Duration.ofMillis(3600000)));
        assertThat(duration, equalTo(Duration.ofSeconds(3600)));
        assertThat(duration, equalTo(Duration.ofMinutes(60)));
        assertThat(duration, equalTo(Duration.ofHours(1)));
    }

    // Test operations

    @Test
    public void plus1() {
        Duration duration = Duration.ofMillis(800);
        assertThat(duration.plus(Duration.ofMillis(300)), equalTo(TimeDuration.ofSeconds(1.1)));
    }

    @Test
    public void plus2() {
        Duration duration = Duration.ofMillis(-100);
        assertThat(duration.plus(Duration.ofMillis(300)), equalTo(TimeDuration.ofSeconds(0.2)));
    }

    @Test
    public void plus3() {
        Duration duration = Duration.ofMillis(100);
        assertThat(duration.plus(Duration.ofMillis(-200)), equalTo(TimeDuration.ofSeconds(-0.1)));
    }

    @Test
    public void plus4() {
        Duration duration = TimeDuration.ofSeconds(1.250);
        assertThat(duration.plus(TimeDuration.ofSeconds(1.250)), equalTo(TimeDuration.ofSeconds(2.5)));
    }

    @Test
    public void plus5() {
        Duration duration = TimeDuration.ofSeconds(10.250);
        assertThat(duration.plus(TimeDuration.ofSeconds(-1.750)), equalTo(TimeDuration.ofSeconds(8.5)));
    }

    @Test
    public void minus1() {
        Duration duration = Duration.ofMillis(800);
        assertThat(duration.minus(Duration.ofMillis(300)), equalTo(TimeDuration.ofSeconds(0.5)));
    }

    @Test
    public void minus2() {
        Duration duration = Duration.ofMillis(800);
        assertThat(duration.minus(Duration.ofMillis(-300)), equalTo(TimeDuration.ofSeconds(1.1)));
    }

    @Test
    public void minus3() {
        Duration duration = Duration.ofMillis(1300);
        assertThat(duration.minus(Duration.ofMillis(800)), equalTo(TimeDuration.ofSeconds(0.5)));
    }

    @Test
    public void minus4() {
        Duration duration = Duration.ofMillis(800);
        assertThat(duration.minus(Duration.ofMillis(1300)), equalTo(TimeDuration.ofSeconds(-0.5)));
    }

    @Test
    public void minus5() {
        Duration duration = TimeDuration.ofSeconds(10.250);
        assertThat(duration.minus(Duration.ofMillis(1750)), equalTo(TimeDuration.ofSeconds(8.5)));
    }

    @Test
    public void multipliedBy1() {
        Duration duration = Duration.ofMillis(300);
        assertThat(duration.multipliedBy(5), equalTo(TimeDuration.ofSeconds(1.5)));
    }

    @Test
    public void multipliedBy2() {
        Duration duration = TimeDuration.ofSeconds(10.500);
        assertThat(duration.multipliedBy(5), equalTo(TimeDuration.ofSeconds(52.5)));
    }

    @Test
    public void multipliedBy3() {
        Duration duration = TimeDuration.ofSeconds(10.500);
        assertThat(duration.multipliedBy(-5), equalTo(TimeDuration.ofSeconds(-52.5)));
    }

    @Test
    public void dividedBy1() {
        Duration duration = Duration.ofMillis(600);
        assertThat(duration.dividedBy(3), equalTo(TimeDuration.ofSeconds(0.2)));
    }

    @Test
    public void dividedBy2() {
        Duration duration = Duration.ofMillis(1200);
        assertThat(duration.dividedBy(3), equalTo(TimeDuration.ofSeconds(0.4)));
    }

    @Test
    public void dividedBy3() {
        Duration duration = Duration.ofMillis(1200);
        assertThat(duration.dividedBy(-3), equalTo(TimeDuration.ofSeconds(-0.4)));
    }

    @Test
    public void dividedBy4() {
        Duration duration = TimeDuration.ofSeconds(10.4);
        assertThat(duration.dividedBy(4), equalTo(TimeDuration.ofSeconds(2.6)));
    }

    @Test
    public void dividedBy5() {
        Duration duration1 = TimeDuration.ofSeconds(10.4);
        Duration duration2 = Duration.ofMillis(100);
        assertThat(TimeDuration.dividedBy(duration1, duration2), equalTo(104));
    }

    @Test
    public void dividedBy6() {
        Duration duration1 = TimeDuration.ofSeconds(10.4);
        Duration duration2 = TimeDuration.ofSeconds(2.5);
        assertThat(TimeDuration.dividedBy(duration1, duration2), equalTo(4));
    }

    @Test
    public void toString1() {
        Duration duration = Duration.ofMillis(10);
        assertThat(TimeDuration.toSecondString(duration), equalTo("0.010000000"));
    }

    @Test
    public void toString2() {
        Duration duration = TimeDuration.ofSeconds(1.2345);
        assertThat(TimeDuration.toSecondString(duration), equalTo("1.234500000"));
    }

    @Test
    public void toString3() {
        Duration duration = TimeDuration.ofSeconds(1234.56789);
        assertThat(TimeDuration.toSecondString(duration), equalTo("1234.567890000"));
    }

    @Test
    public void toNanosLong1() {
        Duration duration = TimeDuration.ofSeconds(1.5);
        assertThat(duration.toNanos(), equalTo(1500000000L));
    }

//    @Test(expected=ArithmeticException.class)
//    public void toNanosLong2() {
//        Duration duration = TimeDuration.ofSeconds(9223372036.9);
//        assertThat(duration.toNanos(), equalTo(1500000000L));
//    }

    @Test
    public void toSeconds1() {
        Duration duration = TimeDuration.ofSeconds(1.5);
        assertThat(toSecondsDouble(duration), equalTo(1.5));
    }

    public void toSeconds2() {
        Duration duration = Duration.ofNanos(1234567890123L);
        assertThat(toSecondsDouble(duration), equalTo(1234.567890123));
    }

    @Test
    public void isPositive1() {
        Duration duration = TimeDuration.ofSeconds(0);
        assertThat(!duration.isNegative(), equalTo(true));
    }

    @Test
    public void isPositive2() {
        Duration duration = TimeDuration.ofSeconds(1.3);
        assertThat(!duration.isNegative(), equalTo(true));
    }

    @Test
    public void isPositive3() {
        Duration duration = TimeDuration.ofSeconds(0.5);
        assertThat(!duration.isNegative(), equalTo(true));
    }

    @Test
    public void isPositive4() {
        Duration duration = TimeDuration.ofSeconds(5.0);
        assertThat(!duration.isNegative(), equalTo(true));
    }

    @Test
    public void isPositive5() {
        Duration duration = TimeDuration.ofSeconds(-0.5);
        assertThat(!duration.isNegative(), equalTo(false));
    }

    @Test
    public void isPositive6() {
        Duration duration = TimeDuration.ofSeconds(-5.5);
        assertThat(!duration.isNegative(), equalTo(false));
    }

    @Test
    public void isNegative1() {
        Duration duration = TimeDuration.ofSeconds(0);
        assertThat(duration.isNegative(), equalTo(false));
    }

    @Test
    public void isNegative2() {
        Duration duration = TimeDuration.ofSeconds(1.3);
        assertThat(duration.isNegative(), equalTo(false));
    }

    @Test
    public void isNegative3() {
        Duration duration = TimeDuration.ofSeconds(0.5);
        assertThat(duration.isNegative(), equalTo(false));
    }

    @Test
    public void isNegative4() {
        Duration duration = TimeDuration.ofSeconds(5.0);
        assertThat(duration.isNegative(), equalTo(false));
    }

    @Test
    public void isNegative5() {
        Duration duration = TimeDuration.ofSeconds(-0.5);
        assertThat(duration.isNegative(), equalTo(true));
    }

    @Test
    public void isNegative6() {
        Duration duration = TimeDuration.ofSeconds(-5.5);
        assertThat(duration.isNegative(), equalTo(true));
    }

    @Test
    public void compare1() {
        Duration duration1 = Duration.ofMillis(500);
        Duration duration2 = Duration.ofMillis(300);
        assertThat(duration1, greaterThan(duration2));
        assertThat(duration2, lessThan(duration1));
        assertThat(duration1, not(comparesEqualTo(duration2)));
        assertThat(duration2, not(comparesEqualTo(duration1)));
    }

    @Test
    public void compare2() {
        Duration duration1 = Duration.ofMillis(500);
        Duration duration2 = Duration.ofMillis(500);
        assertThat(duration1, not(greaterThan(duration2)));
        assertThat(duration2, not(lessThan(duration1)));
        assertThat(duration1, comparesEqualTo(duration2));
        assertThat(duration2, comparesEqualTo(duration1));
    }

    @Test
    public void compare3() {
        Duration duration1 = Duration.ofMillis(1500);
        Duration duration2 = Duration.ofMillis(500);
        assertThat(duration1, greaterThan(duration2));
        assertThat(duration2, lessThan(duration1));
        assertThat(duration1, not(comparesEqualTo(duration2)));
        assertThat(duration2, not(comparesEqualTo(duration1)));
    }

    @Test
    public void compare4() {
        Duration duration1 = Duration.ofMillis(1300);
        Duration duration2 = Duration.ofMillis(500);
        assertThat(duration1, greaterThan(duration2));
        assertThat(duration2, lessThan(duration1));
        assertThat(duration1, not(comparesEqualTo(duration2)));
        assertThat(duration2, not(comparesEqualTo(duration1)));
    }

    @Test
    public void compare5() {
        Duration duration1 = Duration.ofMillis(2500);
        Duration duration2 = Duration.ofMillis(1500);
        assertThat(duration1, greaterThan(duration2));
        assertThat(duration2, lessThan(duration1));
        assertThat(duration1, not(comparesEqualTo(duration2)));
        assertThat(duration2, not(comparesEqualTo(duration1)));
    }

}