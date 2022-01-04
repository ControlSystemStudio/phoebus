package org.phoebus.logbook.olog.ui;

import com.google.common.base.Strings;
import org.phoebus.util.time.TimeParser;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;

public class LogbookQueryUtil {

    // Ordered search keys
    public static enum Keys {
        SEARCH("desc"),
        LOGBOOKS("logbooks"),
        TAGS("tags"),
        STARTTIME("start"),
        ENDTIME("end"),
        AUTHOR("owner"),
        TITLE("title"),
        LEVEL("level"),
        PROPERTIES("properties");

        // The human readable name of the query key
        private final String name;
        // A lookuptable for finding the Keys constant that matches the human readable query key
        private static Map<String, Keys> lookupTable = new HashMap<String, Keys>();

        static {
            lookupTable.put("desc", Keys.SEARCH);
            lookupTable.put("logbooks", Keys.LOGBOOKS);
            lookupTable.put("tags", Keys.TAGS);
            lookupTable.put("start", Keys.STARTTIME);
            lookupTable.put("end", Keys.ENDTIME);
            lookupTable.put("owner", Keys.AUTHOR);
            lookupTable.put("title", Keys.TITLE);
            lookupTable.put("level", Keys.LEVEL);
            lookupTable.put("properties", Keys.PROPERTIES);
        }

        Keys(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static Keys findKey(String keyName) {
            return lookupTable.get(keyName);
        }

        /**
         * These keys are defined as "hidden", i.e. if specified by user in the search field, they
         * will be removed from the query string as presented to user. They are however used
         * "internally" to define behavior, i.e. for sort order and pagination.
         */
        public static List<String> HIDDEN_KEYS = Arrays.asList("from", "size", "sort", "limit");

    }

    /**
     * This method parses a logbook query URI and returns a map of search keys and their assocaited search patterns as
     * values
     *
     * @param query the logbook query URI
     * @return a map consisting of search keys and patterns
     */
    public static Map<String, String> parseQueryURI(URI query) {
        if (Strings.isNullOrEmpty(query.getQuery())) {
            return new HashMap<>();
        } else {
            Map<String, String> searchParams = Arrays.asList(query.getQuery().split("&")).stream()
                    .collect(Collectors.toMap(new KeyParser(), new ValueParser(), (key1, key2) -> key1));
            searchParams.entrySet().removeIf(e -> Keys.findKey(e.getKey().trim().toLowerCase()) == null);
            return searchParams;
        }
    }

    /**
     * This method parses a logbook query string and returns a map of search keys and their associated search patterns as
     * values.
     * The use of temporal descriptors like "1 day" etc are resolved to Unix time.
     *
     * @param query the logbook query string
     * @return a map consisting of search keys and patterns
     */
    public static Map<String, String> parseQueryString(String query) {
        if (Strings.isNullOrEmpty(query)) {
            return new HashMap<>();
        } else {
            Map<String, String> searchParams = Arrays.asList(query.split("&")).stream()
                    .collect(Collectors.toMap(new KeyParser(), new ValueParser(), (key1, key2) -> key1));
            // Remove keys that should not be shown in search field
            searchParams.entrySet().removeIf(e -> Keys.findKey(e.getKey().trim().toLowerCase()) == null);
            return searchParams;
        }
    }

    /**
     * This method parses a logbook query string and returns a map of search keys and their associated search patterns as
     * values.
     * Temporal descriptors like "1 day" etc are not converted to unix time.
     * This method is primarily intended as a helper for UI controls.
     *
     * @param query the logbook query string
     * @return a map consisting of search keys and patterns
     */
    public static Map<String, String> parseHumanReadableQueryString(String query) {
        if (Strings.isNullOrEmpty(query)) {
            return new HashMap<>();
        } else {
            Map<String, String> searchParams = Arrays.asList(query.split("&")).stream()
                    .collect(Collectors.toMap(new KeyParser(), new SimpleValueParser(), (key1, key2) -> key1));
            searchParams.entrySet().removeIf(e -> Keys.findKey(e.getKey().trim().toLowerCase()) == null);
            return searchParams;
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
                String[] split = t.split("=");
                if (split.length < 2) {
                    return "";
                }
                String key = split[0];
                String value = split[1];
                if (key.equals(Keys.STARTTIME.getName()) || key.equals(Keys.ENDTIME.getName())) {
                    Object time = TimeParser.parseInstantOrTemporalAmount(value);
                    if (time instanceof Instant) {
                        return MILLI_FORMAT.format((Instant) time);
                    } else if (time instanceof TemporalAmount) {
                        return MILLI_FORMAT.format(Instant.now().minus((TemporalAmount) time));
                    }
                }
                return value;
            } else {
                return "*";
            }
        }

    }

    private static class SimpleValueParser implements Function<String, String> {
        @Override
        public String apply(String t) {

            if (t.contains("=")) {
                String[] split = t.split("=");
                if (split.length < 2) {
                    return "";
                } else {
                    return t.split("=")[1];
                }
            } else {
                return "*";
            }
        }
    }
}
