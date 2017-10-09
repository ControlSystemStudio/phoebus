/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import java.time.Duration;
import java.time.Instant;

/**
 * A period of time that spans two instances (included) at the nanosecond
 * precision.
 *
 * @author carcassi
 */
public class TimeInterval {

    private final Instant start;
    private final Instant end;

    private TimeInterval(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }

    /**
     * True if the given time stamp is inside the interval.
     *
     * @param instant a time stamp
     * @return true if inside the interval
     */
    public boolean contains(Instant instant) {
        return (start == null || start.compareTo(instant) <= 0) && (end == null || end.compareTo(instant) >= 0);
    }

    /**
     * Returns the interval between the given timestamps.
     *
     * @param start the beginning of the interval
     * @param end the end of the interval
     * @return a new interval
     */
    public static TimeInterval between(Instant start, Instant end) {
        return new TimeInterval(start, end);
    }

    /**
     * Returns a new interval shifted backward in time by the given duration.
     *
     * @param duration a time duration
     * @return the new shifted interval
     */
    public TimeInterval minus(Duration duration) {
        return between(start.minus(duration), end.minus(duration));
    }

    /**
     * Returns a time interval that lasts this duration and is centered
     * around the given instant reference.
     *
     * @param duration the duration
     * @param reference a instant
     * @return a new time interval
     */
    public static TimeInterval around(Duration duration, Instant reference) {
        Duration half = duration.dividedBy(2);
        return TimeInterval.between(reference.minus(half), reference.plus(half));
    }

    /**
     * Returns a time interval that lasts this duration and starts from the
     * given instant.
     *
     * @param duration the duration
     * @param reference a instant
     * @return a new time interval
     */
    public static TimeInterval after(Duration duration, Instant reference) {
        return TimeInterval.between(reference, reference.plus(duration));
    }

    /**
     * Returns a time interval that lasts this duration and ends at the
     * given instant.
     *
     * @param duration the duration
     * @param reference a instant
     * @return a new time interval
     */
    public static TimeInterval before(Duration duration, Instant reference) {
        return TimeInterval.between(reference.minus(duration), reference);
    }

    /**
     * Initial value of the interval.
     *
     * @return the initial instant
     */
    public Instant getStart() {
        return start;
    }

    /**
     * Final value of the interval.
     *
     * @return the final instant
     */
    public Instant getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return String.valueOf(start) + " - " + String.valueOf(end);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TimeInterval) {
            TimeInterval other = (TimeInterval) obj;
            boolean startEqual = (getStart() == other.getStart()) || (getStart() != null && getStart().equals(other.getStart()));
            boolean endEqual = (getEnd() == other.getEnd()) || (getEnd() != null && getEnd().equals(other.getEnd()));
            return startEqual && endEqual;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (this.start != null ? this.start.hashCode() : 0);
        hash = 29 * hash + (this.end != null ? this.end.hashCode() : 0);
        return hash;
    }

}
