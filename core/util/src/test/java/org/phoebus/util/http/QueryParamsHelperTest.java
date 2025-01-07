/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.util.http;

import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QueryParamsHelperTest {

    @Test
    public void testBasic(){
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.put("key1", List.of("value1", "value2"));
        map.put("key2", List.of("value 3"));
        map.put("key3", List.of("value:4"));

        String queryParamString = QueryParamsHelper.mapToQueryParams(map);

        assertEquals("key1=value1,value2&key2=value+3&key3=value%3A4", queryParamString);

        queryParamString = QueryParamsHelper.mapToQueryParams(new MultivaluedHashMap<>());
        assertEquals("", queryParamString);

        queryParamString = QueryParamsHelper.mapToQueryParams(null);
        assertEquals("", queryParamString);

        map = new MultivaluedHashMap<>();
        map.put("key1", null);
        map.put("key2", List.of("value3"));

        queryParamString = QueryParamsHelper.mapToQueryParams(map);
        assertEquals("key2=value3", queryParamString);

        map = new MultivaluedHashMap<>();
        map.put("key1", null);
        map.put("key2", Collections.emptyList());

        queryParamString = QueryParamsHelper.mapToQueryParams(map);
        assertEquals("", queryParamString);

    }
}
