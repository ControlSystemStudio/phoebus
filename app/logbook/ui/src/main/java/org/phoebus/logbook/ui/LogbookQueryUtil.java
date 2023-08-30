package org.phoebus.logbook.ui;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.phoebus.util.time.TimeParser;

import com.google.common.base.Strings;

public class LogbookQueryUtil {

    // Ordered search keys
    public static enum Keys {
        SEARCH("search"),
        LOGBOOKS("logbook"),
        TAGS("tag"),
        STARTTIME("start"),
        ENDTIME("end"),
        AUTHOR("owner"),
        TITLE("title"),
        LEVEL("level");
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

    public static Map<String, String> parseQueryURI(URI query) {
        if (Strings.isNullOrEmpty(query.getQuery())) {
            return Collections.emptyMap();
        } else {
            return Arrays.asList(query.getQuery().split("&")).stream()
                    .collect(Collectors.toMap(new KeyParser(), new ValueParser()));
        }
    }

    public static Map<String, String> parseQueryString(String query) {
        if (Strings.isNullOrEmpty(query)) {
            return Collections.emptyMap();
        } else {
            return Arrays.asList(query.split("&")).stream()
                    .collect(Collectors.toMap(new KeyParser(), new ValueParser()));
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
