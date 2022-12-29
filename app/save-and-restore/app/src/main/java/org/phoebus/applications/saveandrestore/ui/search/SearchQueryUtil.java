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

package org.phoebus.applications.saveandrestore.ui.search;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.HashMap;
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
        SIZE("size");

        private final String name;

        Keys(String name) {
            this.name = name;
        };

        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return getName();
        }

        private static Map<String, Keys> lookupTable = new HashMap<String, Keys>();

        static {
            lookupTable.put("desc", Keys.DESC);
            lookupTable.put("tags", Keys.TAGS);
            lookupTable.put("start", Keys.STARTTIME);
            lookupTable.put("end", Keys.ENDTIME);
            lookupTable.put("user", Keys.USER);
            lookupTable.put("type", Keys.TYPE);
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
