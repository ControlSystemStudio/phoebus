/*******************************************************************************
 * Copyright (c) 2017-2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.pv.jackie.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link SimpleJsonParser}.
 */
public class SimpleJsonParserTest {

    private static Object parse(String json_string) {
        return SimpleJsonParser.parse(json_string);
    }

    private static void testNumber(String number) {
        assertEquals(Double.parseDouble(number),
                ((Number) parse(number)).doubleValue(), 0.00001);
    }

    /**
     * Tests that JSON arrays are parsed correctly.
     */
    @Test
    public void arrays() {
        assertEquals(Collections.emptyList(), parse("[]"));
        assertEquals(Collections.emptyList(), parse("[\t]"));
        assertEquals(Collections.singletonList(true), parse("[true]"));
        assertEquals(Collections.singletonList("abc"), parse("[ \"abc\"]"));
        assertEquals(Collections.singletonList(null), parse("[null ]"));
        assertEquals(Arrays.asList("abc", null, "def", false),
                parse("[ \"abc\", null,\"def\"  , false ]"));
    }

    /**
     * Test that the parsing fails if there is a comma in an empty array.
     */
    @Test
    public void commaInEmptyArrayNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> parse("[,]"));
    }

    /**
     * Test that the parsing fails if there is a comma in an empty array.
     */
    @Test
    public void commaInEmptyObjectNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> parse("{,}"));
    }

    /**
     * Tests that the JSON document "false" is parsed correctly.
     */
    @Test
    public void falseValue() {
        assertEquals(Boolean.FALSE, parse("false"));
    }

    /**
     * Test that the parsing fails if there is leading comma in an array.
     */
    @Test
    public void leadingCommaInArrayNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> {
            parse("[,\"abc\"]");
        });
    }

    /**
     * Test that the parsing fails if there is leading comma in an object.
     */
    @Test
    public void leadingCommaInObjectNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> {
            parse("{,\"a\": 5}");
        });
    }

    /**
     * Test that the parsing fails if there is leading whitespace.
     */
    @Test
    public void leadingWhitespaceNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> parse(" 5"));
    }

    /**
     * Tests that the JSON document "null" is parsed correctly.
     */
    @Test
    public void nullValue() {
        assertNull(parse("null"));
    }

    /**
     * Tests that JSON numbers are parsed correctly.
     */
    @Test
    public void numberValues() {
        testNumber("5.384");
        testNumber("-7.384");
        testNumber("2.0e-3");
        testNumber("-5e22");
        testNumber("1234567890");
        testNumber("0");
        testNumber("0.00");
        testNumber("-48");
        testNumber("1e50000");
    }

    /**
     * Tests that JSON objects are parsed correctly.
     */
    @Test
    public void objects() {
        assertEquals(Collections.emptyMap(), parse("{}"));
        assertEquals(Collections.emptyMap(), parse("{ \n}"));
        assertEquals(Collections.singletonMap("boolean", true),
                parse("{\"boolean\":true}"));
        assertEquals(Collections.singletonMap("string", "abc"),
                parse("{ \"string\"  : \"abc\" }"));
        assertEquals(Collections.singletonMap("null", null),
                parse("{\"null\": null }"));
        assertEquals(
                Collections.singletonMap("nested",
                        Collections.singletonMap("test", true)),
                parse("{\"nested\":{\"test\":true}}"));
        LinkedHashMap<String, Object> test_map = new LinkedHashMap<>();
        test_map.put("k1", "abc");
        test_map.put("k2", null);
        test_map.put("k3", "def");
        test_map.put("k4", false);
        String test_json = "{ \"k1\": \"abc\", \"k2\":null,\"k3\":  \"def\"  , \"k4\" : false }";
        @SuppressWarnings("unchecked")
        Map<String, Object> result_map = (Map<String, Object>) parse(test_json);
        // We want to be sure that the result map has the right order, so we
        // cannot simply use assertEquals().
        assertEquals(test_map.size(), result_map.size());
        Iterator<Map.Entry<String, Object>> i1 = test_map.entrySet().iterator();
        Iterator<Map.Entry<String, Object>> i2 = result_map.entrySet()
                .iterator();
        while (i1.hasNext()) {
            assertEquals(i1.next(), i2.next());
        }
    }

    /**
     * Test that the parsing fails if there is an object that has a key without
     * an associated value.
     */
    @Test
    public void objectWithKeyAndNoValueNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> parse("{\"a\"}"));
    }

    /**
     * Tests that JSON strings are parsed correctly.
     */
    @Test
    public void stringValues() {
        assertEquals("a\"b\\c\näöü", parse("\"a\\\"b\\\\c\\näöü\""));
        assertEquals("", parse("\"\""));
        assertEquals("\"", parse("\"\\\"\""));
        assertEquals(" \n@>", parse("\" \\n\\u0040\\u003e\""));
    }

    /**
     * Test that the parsing fails if there is trailing comma in an array.
     */
    @Test
    public void trailingCommaInArrayNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> parse("[5,]"));
    }

    /**
     * Test that the parsing fails if there is trailing comma in an object.
     */
    @Test
    public void trailingCommaInObjectNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> {
            parse("{\"a\": 5,}");
        });
    }

    /**
     * Test that the parsing fails if there is trailing whitespace.
     */
    @Test
    public void trailingWhitespaceNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> parse("48\t"));
    }

    /**
     * Tests that the JSON document "true" is parsed correctly.
     */
    @Test
    public void trueValue() {
        assertEquals(Boolean.TRUE, parse("true"));
    }

}
