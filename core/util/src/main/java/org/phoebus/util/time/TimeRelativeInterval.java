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
import java.util.Optional;

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
 * @author carcassi, shroffk
 */
@SuppressWarnings("nls")
public class TimeRelativeInterval {

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
     * either a {@link Duration} or {@link Period} or "now" and an absolute end
     * represented by an {@link Instant}
     *
     * e.g. TimeRelativeInterval.of(TimeParser.parse("5 days"), Instant.now())
     *
     * @param start
     *            the relative start
     * @param end
     *            the absolute end
     * @return {@link TimeRelativeInterval}
     */
    public static TimeRelativeInterval of(TemporalAmount start, Instant end) {
        return new TimeRelativeInterval(start, end);
    }

    /**
     * Create a {@link TimeRelativeInterval} with an absolute start represented
     * by an {@link Instant} and a relative end described as either a
     * {@link Duration} or {@link Period} or "now"
     *
     * e.g. TimeRelativeInterval.of(TimeParser.parse("2017/01/17 13:45"),
     * TimeParser.parse("2 days"))
     *
     * @param start
     *            the start instant
     * @param end
     *            the relative end time
     * @return {@link TimeRelativeInterval}
     */
    public static TimeRelativeInterval of(Instant start, TemporalAmount end) {
        return new TimeRelativeInterval(start, end);
    }

    /**
     * Create a {@link TimeRelativeInterval} which starts at the absolute
     * instance "start" and ends at "now"
     *
     * @param start
     *            the absolute start instant
     * @return {@link TimeRelativeInterval}
     */
    public static TimeRelativeInterval startsAt(Instant start) {
        return new TimeRelativeInterval(start, Duration.ZERO);
    }

    /**
     * Create a {@link TimeRelativeInterval} with a relative start and a
     * relative end "now"
     *
     * @param start
     *            the relative start time defined as a {@link Period} or
     *            {@link Duration}
     * @return {@link TimeRelativeInterval}
     */
    public static TimeRelativeInterval startsAt(TemporalAmount start) {
        return new TimeRelativeInterval(start, Duration.ZERO);
    }

    /**
     * Create a {@link TimeRelativeInterval} with an absolute end time and a
     * relative start time which is "now"
     *
     * @param end
     *            the absolute end instant
     * @return {@link TimeRelativeInterval}
     */
    public static TimeRelativeInterval endsAt(Instant end) {
        return new TimeRelativeInterval(Duration.ZERO, end);
    }

    /**
     * Create a {@link TimeRelativeInterval} with a relative end time defined as
     * a {@link Period} or {@link Duration} adn a relative start "now"
     *
     * @param end
     *            the relative end time
     * @return {@link TimeRelativeInterval}
     */
    public static TimeRelativeInterval endsAt(TemporalAmount end) {
        return new TimeRelativeInterval(Duration.ZERO, end);
    }

    /**
     * Check if the start is absolute
     *
     * @return true if the start is defined as an absolute value
     */
    public boolean isStartAbsolute() {
        return start instanceof Instant;
    }

    /**
     * Check if the end of the {@link TimeRelativeInterval} is absolute
     *
     * @return true if the end is defined as an absolute value
     */
    public boolean isEndAbsolute() {
        return end instanceof Instant;
    }

    /**
     * Get the absolute start value of this {@link TimeRelativeInterval}. The
     * optional is empty if the start value is relative.
     *
     * @return {@link Optional} {@link Instant}
     */
    public Optional<Instant> getAbsoluteStart() {
        if (isStartAbsolute()) {
            return Optional.of((Instant) start);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get the absolute end value of this {@link TimeRelativeInterval}. The
     * returned Optional is empty is the end value is relative.
     *
     * @return {@link Optional} {@link Instant}
     */
    public Optional<Instant> getAbsoluteEnd() {
        if (isEndAbsolute()) {
            return Optional.of((Instant) end);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get the relative start value of this {@link TimeRelativeInterval}. The
     * returned Optional is empty is the end value is absolute.
     *
     * @return {@link Optional} {@link TemporalAmount}
     */
    public Optional<TemporalAmount> getRelativeStart() {
        if (!isStartAbsolute()) {
            return Optional.of((TemporalAmount) start);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get the relative start value of this {@link TimeRelativeInterval}. The
     * returned Optional is empty is the end value is absolute.
     *
     * @return {@link Optional} {@link TemporalAmount}
     */
    public Optional<TemporalAmount> getRelativeEnd() {
        if (!isEndAbsolute()) {
            return Optional.of((TemporalAmount) end);
        } else {
            return Optional.empty();
        }
    }

    public TimeInterval toAbsoluteInterval(Instant reference) {
        Instant absoluteStart;
        Instant absoluteEnd;

        if (isStartAbsolute() && isEndAbsolute()) {
            // Both start and end are absolute
            absoluteStart = getAbsoluteStart().get();
            absoluteEnd = getAbsoluteEnd().get();
        } else if (isStartAbsolute() && !isEndAbsolute()) {
            // Start is absolute and end is relative
            absoluteStart = getAbsoluteStart().get();
            if (getRelativeEnd().get() instanceof Duration) {
                if (Duration.ZERO.equals(getRelativeEnd().get())) {
                    absoluteEnd = Instant.now();
                } else {
                    absoluteEnd = getAbsoluteStart().get().plus(getRelativeEnd().get());
                }
            } else {
                absoluteEnd = LocalDateTime.ofInstant(getAbsoluteStart().get(), ZoneOffset.UTC)
                        .plus(getRelativeEnd().get()).toInstant(ZoneOffset.UTC);
            }
        } else if (!isStartAbsolute() && isEndAbsolute()) {
            // Start is relative and the end is absolute
            absoluteEnd = getAbsoluteEnd().get();
            if (getRelativeStart().get() instanceof Duration) {
                if (Duration.ZERO.equals(getRelativeStart().get())) {
                    absoluteStart = Instant.now();
                } else {
                    absoluteStart = absoluteEnd.minus(getRelativeStart().get());
                }
            } else {
                absoluteStart = LocalDateTime.ofInstant(absoluteEnd, ZoneOffset.UTC).minus(getRelativeStart().get())
                        .toInstant(ZoneOffset.UTC);
            }
        } else {
            // Both start and end are relative to the reference
            if (getRelativeStart().get() instanceof Duration) {
                if (Duration.ZERO.equals(getRelativeStart().get())) {
                    absoluteStart = Instant.now();
                } else {
                    absoluteStart = reference.minus(getRelativeStart().get());
                }
            } else {
                absoluteStart = LocalDateTime.ofInstant(reference, ZoneOffset.UTC).minus(getRelativeStart().get())
                        .toInstant(ZoneOffset.UTC);
            }
            if (getRelativeEnd().get() instanceof Duration) {
                if (Duration.ZERO.equals(getRelativeEnd().get())) {
                    absoluteEnd = Instant.now();
                } else {
                    absoluteEnd = reference.minus(getRelativeEnd().get());
                }
            } else {
                absoluteEnd = LocalDateTime.ofInstant(reference, ZoneOffset.UTC).minus(getRelativeEnd().get()).toInstant(ZoneOffset.UTC);
            }
        }
        return TimeInterval.between(absoluteStart, absoluteEnd);
    }

    public TimeInterval toAbsoluteInterval() {
        return toAbsoluteInterval(Instant.now());
    }

    /** @return Human-readable representation */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();

        if (isStartAbsolute())
            buf.append(TimestampFormats.SECONDS_FORMAT.format((Instant) start));
        else
            buf.append(TimeParser.format((TemporalAmount)start));

        buf.append(" - ");

        if (isEndAbsolute())
            buf.append(TimestampFormats.SECONDS_FORMAT.format((Instant) end));
        else
            buf.append(TimeParser.format((TemporalAmount)end));

        return buf.toString();
    }
}
