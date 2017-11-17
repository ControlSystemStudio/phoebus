/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAmount;

/**
 * A period of time where each end can either be an absolute moment in time
 * (e.g. 5/16/2012 11:30 AM) or a relative moment from a reference (e.g. 30
 * seconds before) which typically is going to be "now".
 * <p>
 * This class stores a reference for start and a reference for end. Each
 * reference can either be absolute, in which case it's a Instant, or relative,
 * in which case it's a TimeDuration. The {@link Instant} can be used to
 * transform the relative interval into an absolute one calculated from the
 * reference. This allows to keep the relative interval, and then to convert
 * multiple time to an absolute interval every time that one needs to calculate.
 * For example, one can keep the range of a plot from 1 minute ago to now, and
 * then get a specific moment the absolute range of that plot.
 *
 * @author carcassi
 */
public class TimeRelativeInterval {

    public static final String NOW = "Now";

    private final Object start;
    private final Object end;

    private TimeRelativeInterval(Object start, Object end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Create a {@link TimeRelativeInterval} with an absolute start time and an
     * absolute end time.
     *
     * @param start
     *            the absolute start time of the this time interval
     * @param end
     *            the absolute end time of this time interval
     * @return a {@link TimeRelativeInterval} object starting at Instance start
     *         and ending at Instance end
     */
    public static TimeRelativeInterval of(Instant start, Instant end) {
        return new TimeRelativeInterval(start, end);
    }

    /**
     * Create a {@link TimeRelativeInterval} with a relative start and end
     * described as either a {@link Duration} or {@link Period}
     *
     * e.g. TimeRelativeInterval.of(TimeParser.parse("last 5 days"),
     * TimeParser.parse("2 days ago"))
     * 
     * @param start
     *            the relative start
     * @param end
     *            the relative end
     * @return a
     */
    public static TimeRelativeInterval of(TemporalAmount start, TemporalAmount end) {
        return new TimeRelativeInterval(start, end);
    }

    /**
     * Create a {@link TimeRelativeInterval} with a relative start described as
     * either a {@link Duration} or {@link Period} and an absolute end
     * represented by an {@link Instant}
     * 
     * e.g. TimeRelativeInterval.of(TimeParser.parse("last 5 days"),
     * Instant.now())
     * 
     * @param start
     *            the relative start
     * @param end
     *            the absolute end
     * @return
     */
    public static TimeRelativeInterval of(TemporalAmount start, Instant end) {
        return new TimeRelativeInterval(start, end);
    }

    /**
     * Create a {@link TimeRelativeInterval} with an absolute start represented
     * by an {@link Instant} and an absolute end described as either a
     * {@link Duration} or {@link Period}
     * 
     * e.g. TimeRelativeInterval.of(TimeParser.parse("2017/01/17 13:45"),
     * TimeParser.parse("+2 days"))
     * 
     * @param start
     *            the relative start
     * @param end
     *            the absolute end
     * @return
     */
    public static TimeRelativeInterval of(Instant start, TemporalAmount end) {
        return new TimeRelativeInterval(start, end);
    }

    public static TimeRelativeInterval startsAt(Instant start) {
        return new TimeRelativeInterval(start, NOW);
    }

    public static TimeRelativeInterval startsAt(TemporalAmount start) {
        return new TimeRelativeInterval(start, NOW);
    }

    public static TimeRelativeInterval endsAt(Instant end) {
        return new TimeRelativeInterval(NOW, end);
    }

    public static TimeRelativeInterval endsAt(TemporalAmount end) {
        return new TimeRelativeInterval(NOW, end);
    }

    private boolean isStartAbsolute() {
        return start == null || start instanceof Instant || NOW.equals(start);
    }

    private boolean isEndAbsolute() {
        return end == null || end instanceof Instant || NOW.equals(end);
    }

    private Instant getAbsoluteStart() {
        if(NOW.equals(start)) {
            return Instant.now();
        }
        return (Instant) start;
    }

    private Instant getAbsoluteEnd() {
        if(NOW.equals(end)) {
            return Instant.now();
        }
        return (Instant) end;
    }

    private TemporalAmount getRelativeStart() {
        return (TemporalAmount) start;
    }

    private TemporalAmount getRelativeEnd() {
        return (TemporalAmount) end;
    }

    public TimeInterval toAbsoluteInterval(Instant reference) {
        Instant absoluteStart;
        if (isStartAbsolute()) {
            absoluteStart = getAbsoluteStart();
        } else {
            if (getRelativeStart() instanceof Duration) {
                absoluteStart = reference.minus(getRelativeStart());
            } else {
                absoluteStart = LocalDateTime
                        .ofInstant(reference, ZoneOffset.UTC)
                        .minus(getRelativeStart())
                        .toInstant(ZoneOffset.UTC);
            }
        }
        Instant absoluteEnd;
        if (isEndAbsolute()) {
            absoluteEnd = getAbsoluteEnd();
        } else {
            if (getRelativeEnd() instanceof Duration) {
                absoluteEnd = absoluteStart.plus(getRelativeEnd());
            } else {
                absoluteEnd = LocalDateTime
                        .ofInstant(absoluteStart, ZoneOffset.UTC)
                        .plus(getRelativeEnd())
                        .toInstant(ZoneOffset.UTC);
            }
        }
        return TimeInterval.between(absoluteStart, absoluteEnd);
    }

    public TimeInterval toAbsoluteInterval() {
        return toAbsoluteInterval(Instant.now());
    }

}
