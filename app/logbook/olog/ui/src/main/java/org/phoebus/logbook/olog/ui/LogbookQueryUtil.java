package org.phoebus.logbook.olog.ui;

import com.google.common.base.Strings;
import org.phoebus.util.time.TimeParser;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;

public class LogbookQueryUtil {

    // Ordered search keys
    public static enum Keys {
        SEARCH("desc"),
        LOGBOOKS("logbooks"),
        TAGS("tag"),
        STARTTIME("start"),
        ENDTIME("end"),
        AUTHOR("owner"),
        TITLE("title"),
        LEVEL("level"),
        PROPERTIES("properties"),
        SORT("sort");

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
            lookupTable.put("sort", Keys.SORT);
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

    }
    /**
     * This method parses a logbook query URI and returns a map of search keys and their assocaited search patterns as
     * values
     * @param query the logbook query URI
     * @return a map consisting of search keys and patterns
     */
    public static Map<String, String> parseQueryURI(URI query) {
        if (Strings.isNullOrEmpty(query.getQuery())) {
            return new HashMap<>();
        } else {
            return Arrays.asList(query.getQuery().split("&")).stream()
                    .collect(Collectors.toMap(new KeyParser(), new ValueParser()));
        }
    }

    /**
     * This method parses a logbook query string and returns a map of search keys and their associated search patterns as
     * values.
     * The use of temporal descriptors like "1 day" etc are resolved to Unix time.
     * @param query the logbook query string
     * @return a map consisting of search keys and patterns
     */
    public static Map<String, String> parseQueryString(String query) {
        if (Strings.isNullOrEmpty(query)) {
            return new HashMap<>();
        } else {
            return Arrays.asList(query.split("&")).stream()
                    .collect(Collectors.toMap(new KeyParser(), new ValueParser()));
        }
    }

    /**
     * This method parses a logbook query string and returns a map of search keys and their associated search patterns as
     * values.
     * Temporal descriptors like "1 day" etc are not converted to unix time.
     * This method is primarily intended as a helper for UI controls.
     * @param query the logbook query string
     * @return a map consisting of search keys and patterns
     */
    public static Map<String, String> parseHumanReadableQueryString(String query) {
        if (Strings.isNullOrEmpty(query)) {
            return new HashMap<>();
        } else {
            return Arrays.asList(query.split("&")).stream()
                    .collect(Collectors.toMap(new KeyParser(), new SimpleValueParser()));
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
                if(split.length < 2){
                    return "";
                }
                String key = split[0];
                String value = split[1];
                if (key.equals(Keys.STARTTIME.getName()) || key.equals(Keys.ENDTIME.getName())) {
                    Object time = TimeParser.parseInstantOrTemporalAmount(value);
                    if (time instanceof Instant) {
                        return MILLI_FORMAT.format((Instant)time);
                    } else if (time instanceof TemporalAmount) {
                        return MILLI_FORMAT.format(Instant.now().minus((TemporalAmount)time));
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
                if(split.length < 2){
                    return "";
                }
                else{
                    return t.split("=")[1];
                }
            } else {
                return "*";
            }
        }
    }

    /**
     * In case <code>query</code> contains the "sort" query parameter (e.g. added manually by user), it will
     * be removed. Then - if <code>sortAscending=true</code> - "&sort=ASC" is appended last.
     * @param query
     * @param sortAscending
     * @return A query string that will determine sort order.
     */
    public static String addSortOrder(String query, boolean sortAscending){
        Map<String, String> queryParams = LogbookQueryUtil.parseHumanReadableQueryString(query);
        if(queryParams.containsKey("sort")){ // In case user did add sort=...
            queryParams.remove("sort");
        }
        if(sortAscending){
            queryParams.put("sort", "up");
        }
        else{
            queryParams.put("sort", "down");
        }
        StringBuilder stringBuilder = new StringBuilder();
        queryParams.keySet().stream().forEach(key -> stringBuilder.append(key + "=" + queryParams.get(key) + "&"));
        String queryString = stringBuilder.toString();
        return queryString.substring(0, queryString.length() - 1); // Remove trailing '&' char
    }
}
