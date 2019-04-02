package org.phoebus.applications.alarm.logging.ui;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.phoebus.util.time.TimeParser;

public class AlarmLogTableQueryUtil {

    // Ordered search keys
    public static enum Keys {
        PV("pv"), SEVERITY("severity"), MESSAGE("message"), CURRENTSEVERITY("current_severity"), CURRENTMESSAGE("current_message"), USER("user"), HOST("host"), COMMAND("command"), STARTTIME("start"), ENDTIME("end");
        private final String name;

        private Keys(String name) {
            this.name = name;
        };

        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.toString();
        }
    }

    private static class KeyParser implements Function<String, String> {

        @Override
        public String apply(String t) {
            if (t.contains("=")) {
                return t.split("=")[0];
            } else {
                return t;
            }
        }

    }

    private static class ValueParser implements Function<String, String> {

        @Override
        public String apply(String t) {

            if (t.contains("=")) {
                String key = t.split("=")[0];
                String value = t.split("=")[1];
                if (key.equals(Keys.STARTTIME.getName()) || key.equals(Keys.ENDTIME.getName())) {
                    Object time = TimeParser.parseInstantOrTemporalAmount(value);
                    if (time instanceof Instant) {
                        return String.valueOf(((Instant)time).toEpochMilli()/1000);
                    } else if (time instanceof TemporalAmount) {
                        return String.valueOf(Instant.now().minus((TemporalAmount)time).toEpochMilli()/1000);
                    }
                }
                return value;
            } else {
                return "*";
            }
        }

    }
}
