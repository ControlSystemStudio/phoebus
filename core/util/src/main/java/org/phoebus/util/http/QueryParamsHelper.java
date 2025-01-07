/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.util.http;

import javax.ws.rs.core.MultivaluedMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class QueryParamsHelper {

    public static String mapToQueryParams(MultivaluedMap<String, String> map) {
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
        return stringBuilder.toString();
    }
}
