package org.phoebus.applications.alarm;

import org.phoebus.util.time.TimeParser;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalAmount;
import java.util.Map;


/**
 * Represents a disable duration that the user can select when disabling an alarm.
 *
 * Presets can be relative (e.g. 5 hours, 10 days) or occurrences (e.g. 17:00, Monday 3:50).
 * Occurrences simply select the next occurrence of the given time or day.
 *
 * Occurrences can be minutes, hours:minutes, or weekday hours:minutes.
 */
public sealed interface DisableDurationPreset permits DisableDurationPreset.Relative, DisableDurationPreset.Occurrence {

    record Relative(TemporalAmount amount) implements DisableDurationPreset {

        public Relative(final TemporalAmount amount) {
            this.amount = amount;
        }

        @Override
            public LocalDateTime getNextDisableDateTime(final LocalDateTime now) {
                return now.plus(amount);
            }
        }

    record Occurrence(DayOfWeek day, Integer hour, int minute) implements DisableDurationPreset {

        public Occurrence(final DayOfWeek day, final Integer hour, final int minute) {
            this.day = day;
            this.hour = hour;
            this.minute = minute;

            if (hour == null && day != null) {
                throw new IllegalArgumentException("A day and minute was provided, but no hour");
            }

            if (hour != null && hour > 23) {
                throw new IllegalArgumentException("Invalid hour " + hour);
            }

            if (minute > 59) {
                throw new IllegalArgumentException("Invalid minute " + minute);
            }
        }

        @Override
        public LocalDateTime getNextDisableDateTime(final LocalDateTime now) {
            LocalDateTime nextDisableTime = now.withMinute(minute).truncatedTo(ChronoUnit.MINUTES);

            if (hour == null) {
                // only minute
                if (nextDisableTime.isBefore(now)) {
                    nextDisableTime = nextDisableTime.plusHours(1);
                }
                return nextDisableTime;
            }

            nextDisableTime = nextDisableTime.withHour(hour);
            if (day == null) {
                // hour and minute
                if (nextDisableTime.isBefore(now)) {
                    nextDisableTime = nextDisableTime.plusDays(1);
                }
                return nextDisableTime;
            }

            // day, hour, and minute
            nextDisableTime = nextDisableTime.with(TemporalAdjusters.nextOrSame(day));
            if (nextDisableTime.isBefore(now)) {
                nextDisableTime = nextDisableTime.plusWeeks(1);
            }
            return nextDisableTime;
        }
    }

    LocalDateTime getNextDisableDateTime(final LocalDateTime now);

    static DisableDurationPreset parse(final String string) {
        final String stripped = string.strip();

        final Map<String, DayOfWeek> weekdaysMap = Map.of(
                "Mon", DayOfWeek.MONDAY,
                "Tue", DayOfWeek.TUESDAY,
                "Wed", DayOfWeek.WEDNESDAY,
                "Thu", DayOfWeek.THURSDAY,
                "Fri", DayOfWeek.FRIDAY,
                "Sat", DayOfWeek.SATURDAY,
                "Sun", DayOfWeek.SUNDAY
        );
        final String weekdaysRegex = "(" + String.join("|", weekdaysMap.keySet()) + ")";

        if (stripped.matches("xx:\\d\\d"))
        {
            return new DisableDurationPreset.Occurrence(
                    null,
                    null,
                    Integer.parseInt(stripped.substring(3))
            );
        } else if (stripped.matches("\\d\\d:\\d\\d"))
        {
            return new Occurrence(
                    null,
                    Integer.parseInt(stripped.substring(0, 2)),
                    Integer.parseInt(stripped.substring(3))
            );
        } else if (stripped.matches(weekdaysRegex + " \\d\\d:\\d\\d")) {
            return new Occurrence(
                    weekdaysMap.get(stripped.substring(0, 3)),
                    Integer.parseInt(stripped.substring(4, 6)),
                    Integer.parseInt(stripped.substring(7))
            );
        }

        final TemporalAmount amount = TimeParser.parseTemporalAmount(stripped);
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime end = now.plus(amount);
        if (!end.isAfter(now))
            throw new RuntimeException("Invalid 'disable_duration_presets' value '" + string + "'");
        return new DisableDurationPreset.Relative(amount);
    }
}
