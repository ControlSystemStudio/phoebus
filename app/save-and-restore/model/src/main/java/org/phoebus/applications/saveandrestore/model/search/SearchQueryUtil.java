/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.applications.saveandrestore.model.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SearchQueryUtil {

    // Ordered search keys
    public enum Keys {
        NAME("name"),
        DESC("desc"),
        USER("user"),
        TAGS("tags"),
        TYPE("type"),
        STARTTIME("start"),
        ENDTIME("end"),
        FROM("from"),
        SIZE("size"),
        GOLDEN("golden");

        private final String name;

        Keys(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return getName();
        }

        protected static final Map<String, Keys> lookupTable = new HashMap<>();

        static {
            lookupTable.put("name", Keys.NAME);
            lookupTable.put("desc", Keys.DESC);
            lookupTable.put("tags", Keys.TAGS);
            lookupTable.put("start", Keys.STARTTIME);
            lookupTable.put("end", Keys.ENDTIME);
            lookupTable.put("user", Keys.USER);
            lookupTable.put("type", Keys.TYPE);
            lookupTable.put("golden", Keys.GOLDEN);
        }

        public static Keys findKey(String keyName) {
            return lookupTable.get(keyName);
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
        if (query == null || query.isEmpty()) {
            return new HashMap<>();
        } else {
            Map<String, String> searchParams = Arrays.stream(query.split("&"))
                    .collect(Collectors.toMap(new KeyParser(), new SimpleValueParser(), (key1, key2) -> key1));
            searchParams.entrySet().removeIf(e -> Keys.findKey(e.getKey().trim().toLowerCase()) == null);
            return searchParams;
        }
    }

    /**
     * Converts a {@link Map} of search query key/value pairs to URL pattern.
     * @param queryParams {@link Map} of query parameters where values may be <code>null</code> or empty.
     * @return A URL like string, e.g. a=b&amp;c=d.
     */
    public static String toQueryString(Map<String, String> queryParams){
        if(queryParams == null){
            return "";
        }
        List<String> params = new ArrayList<>();
        queryParams.keySet().forEach(key -> {
            if(Keys.lookupTable.containsKey(key)){
                params.add(key + "=" + (queryParams.get(key) == null ? "" : formatSearchTerm(queryParams.get(key))));
            }
        });
        return params.stream().collect(Collectors.joining("&"));
    }

    /**
     * @param searchTerm A search term that may contain comma separated list of values.
     * @return A formatted string with trimmed values, e.g. "a,b" rather than " a , b".
     */
    private static String formatSearchTerm(String searchTerm){
        if(searchTerm == null || searchTerm.isEmpty()){
            return "";
        }
        return Arrays.asList(searchTerm.split(",")).stream().map(i -> i.trim()).collect(Collectors.joining(","));
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

    private static class SimpleValueParser implements Function<String, String> {
        @Override
        public String apply(String t) {

            if (t.contains("=")) {
                String[] split = t.split("=");
                if (split.length < 2) {
                    return "";
                } else {
                    return formatSearchTerm(t.split("=")[1]);
                }
            } else {
                return "*";
            }
        }
    }
}
