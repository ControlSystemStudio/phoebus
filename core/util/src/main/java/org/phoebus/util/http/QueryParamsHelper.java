/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.util.http;

import javax.ws.rs.core.MultivaluedMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class QueryParamsHelper {

    /**
     * Converts a {@link MultivaluedMap<String, String>} to a URL encoded query parameter {@link String}. Attempts to
     * be fault-tolerant: <code>null</code> or empty {@link List}s values are ignored and not appended to the
     * returned query parameter {@link String}. See also unit test class.
     * @param map Map of key/value pairs where each value is expected to be a {@link List} of {@link String}s.
     * @return A URL encoded {@link String} that can be appended in a http URL, e.g. <code>key1=value1,value2&key2=value3</code>.
     */
    public static String mapToQueryParams(MultivaluedMap<String, String> map) {
        if(map == null || map.isEmpty()){
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        map.keySet().forEach(k -> {
            List<String> value = map.get(k);
            if (value != null && !value.isEmpty()) {
                stringBuilder.append(k).append("=");
                stringBuilder.append(String.join(",",
                        value.stream().map(v -> URLEncoder.encode(v, StandardCharsets.UTF_8)).toList()));
                stringBuilder.append("&");
            }
        });
        String queryParams = stringBuilder.toString();
        if("".equals(queryParams)){
            return "";
        }
        return queryParams.substring(0, queryParams.length() - 1);
    }
}
