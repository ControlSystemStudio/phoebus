package org.phoebus.applications.alarm;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.phoebus.util.time.TimeDuration;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class DisableDurationPresetTest {

    @Test
    public void testParse() {
        assertThat(DisableDurationPreset.parse("5 min"), equalTo(new DisableDurationPreset.Relative(TimeDuration.ofMinutes(5))));
        assertThat(DisableDurationPreset.parse("xx:16"), equalTo(new DisableDurationPreset.Occurrence(null, null, 16)));
        assertThat(DisableDurationPreset.parse("12:16"), equalTo(new DisableDurationPreset.Occurrence(null, 12, 16)));
        assertThat(DisableDurationPreset.parse("Mon 18:16"), equalTo(new DisableDurationPreset.Occurrence(DayOfWeek.MONDAY, 18, 16)));
        assertThrows(Exception.class, () -> DisableDurationPreset.parse("25:00"));
        assertThrows(Exception.class, () -> DisableDurationPreset.parse("xx:70"));
    }

    @Test
    public void testGetNextDisableDateTime() {
        final LocalDateTime now = LocalDateTime.of(2050, 12, 25, 6, 9);

        assertThat(
                new DisableDurationPreset.Relative(Duration.of(5, ChronoUnit.DAYS)).getNextDisableDateTime(now),
                equalTo(now.plusDays(5))
        );

        assertThat(
                new DisableDurationPreset.Occurrence(null, null, 50).getNextDisableDateTime(now),
                equalTo(now.withMinute(50))
        );
        assertThat(
                new DisableDurationPreset.Occurrence(null, null, 5).getNextDisableDateTime(now),
                equalTo(now.withMinute(5).plusHours(1))
        );

        assertThat(
                new DisableDurationPreset.Occurrence(null, 12, 5).getNextDisableDateTime(now),
                equalTo(now.withHour(12).withMinute(5))
        );
        assertThat(
                new DisableDurationPreset.Occurrence(null, 3, 5).getNextDisableDateTime(now),
                equalTo(now.withHour(3).withMinute(5).plusDays(1))
        );

        assertThat(
                new DisableDurationPreset.Occurrence(now.getDayOfWeek(), 12, 5).getNextDisableDateTime(now),
                equalTo(now.withHour(12).withMinute(5))
        );
        assertThat(
                new DisableDurationPreset.Occurrence(now.getDayOfWeek(), 3, 5).getNextDisableDateTime(now),
                equalTo(now.withHour(3).withMinute(5).plusWeeks(1))
        );
    }
}
