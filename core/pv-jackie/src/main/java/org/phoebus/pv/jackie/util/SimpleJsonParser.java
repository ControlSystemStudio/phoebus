/*******************************************************************************
 * Copyright (c) 2017-2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.pv.jackie.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PrimitiveIterator;

/**
 * <p>
 * Simple JSON parser. This parser is optimized for simplicity, not
 * performance, so code that wants to parse large or complex JSON documents
 * should use a different JSON parser.
 * </p>
 * 
 * <p>
 * This parser has specifically been written in order to minimize the
 * dependencies needed for parsing JSON document. It only uses the Java 17 SE
 * API and the Apache Commons Lang 3 library.
 * </p>
 * 
 * <p>
 * This parser is able to parse any document that complies with the JSON
 * (ECMA-404) standard. Compared to many other parsers, this parser is very
 * strict about compliance and will typically refuse any input that is not
 * strictly compliant.
 * </p>
 * 
 * <p>
 * This parser converts JSON objects to Java objects using the following rules:
 * </p>
 * 
 * <ul>
 * <li>A JSON object is converted to a {@link Map Map&lt;String, Object&gt;}.
 * The order of the members is preserved in the map. The parser does not allow
 * duplicate member keys in objects. If a member using the same key as an
 * earlier member is found, the parser throws an exception.</li>
 * <li>A JSON array is converted to a {@link List List&lt;Object&gt;}.</li>
 * <li>A JSON string is converted to a {@link String}.</li>
 * <li>A JSON number is converted to a {@link Number}. The actual type of the
 * {@code Number} depends on the number's value and should be regarded as an
 * implementation detail that might change in the future.</li>
 * <li>A JSON boolean value is converted to a {@link Boolean}.</li>
 * <li>A JSON value of <code>null</code> is converted to
 * <code>null</code>.</li>
 * </ul>
 */
public class SimpleJsonParser {

    /**
     * Parses the specified string into a Java object. Please refer to the
     * {@linkplain SimpleJsonParser class description} for details.
     * 
     * @param json_string
     *            string that represents a valid JSON document.
     * @return object that is the result of converting the string from JSON
     *         into a Java object. <code>null</code> if and only if the
     *         <code>json_string</code> is the literal string "null".
     * @throws IllegalArgumentException
     *             if the <code>json_string</code> cannot be parsed because it
     *             is either invalid, or there is an object with duplicate
     *             member keys.
     */
    public static Object parse(String json_string) {
        // If json_string is null, fail early.
        if (json_string == null) {
            throw new NullPointerException();
        }
        return new SimpleJsonParser(json_string).parse();
    }

    private static String escapeString(String s) {
        return s.codePoints().collect(
                StringBuilder::new,
                (sb, code_point) -> {
                    switch (code_point) {
                        case 8: // \b
                        case 9: // \t
                        case 10: // \n
                        case 12: // \f
                        case 13: // \r
                        case 34: // \"
                        case 92: // \\
                            sb.append('\\');
                    }
                    sb.appendCodePoint(code_point);
                },
                StringBuilder::append).toString();
    }

    private final String parsed_string;
    private int position;

    private SimpleJsonParser(String json_string) {
        this.parsed_string = json_string;
        this.position = 0;
    }

    private boolean accept(int code_point) {
        if (isNext(code_point)) {
            consumeCodePoint();
            return true;
        } else {
            return false;
        }
    }

    private boolean accept(String accepted_string) {
        if (parsed_string.startsWith(accepted_string, position)) {
            position += accepted_string.length();
            return true;
        } else {
            return false;
        }
    }

    private Optional<Integer> acceptAnyOf(String options) {
        if (exhausted()) {
            return Optional.empty();
        }
        int actual_code_point = peek();
        int index = 0;
        while (index < options.length()) {
            int expected_code_point = options.codePointAt(index);
            index += Character.charCount(expected_code_point);
            if (actual_code_point == expected_code_point) {
                return Optional.of(consumeCodePoint());
            }
        }
        return Optional.empty();
    }

    private void acceptWhitespace() {
        boolean is_whitespace = true;
        while (!exhausted() && is_whitespace) {
            int code_point = peek();
            switch (code_point) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
                consumeCodePoint();
                break;
            default:
                is_whitespace = false;
                break;
            }
        }
    }

    private int consumeCodePoint() {
        // We assume that this method is only called after checking that we
        // have not reached the end of the string.
        int code_point = parsed_string.codePointAt(position);
        position += Character.charCount(code_point);
        return code_point;
    }

    private String escapeAndShorten() {
        return escapeAndShorten(parsed_string.substring(position));
    }

    private String escapeAndShorten(CharSequence cs) {
        int max_length = 12;
        if (cs.length() < max_length) {
            return escapeString(cs.toString());
        } else {
            return escapeString(cs.subSequence(0, max_length - 3) + "...");
        }
    }

    private boolean exhausted() {
        return position >= parsed_string.length();
    }

    private void expect(int expected_code_point) {
        if (exhausted()) {
            throw new IllegalArgumentException("Expected '"
                    + new String(Character.toChars(expected_code_point))
                    + "', but found end-of-string.");
        }
        int actual_code_point = consumeCodePoint();
        if (actual_code_point != expected_code_point) {
            throw new IllegalArgumentException("Expected '"
                    + new String(Character.toChars(expected_code_point))
                    + "', but found '"
                    + new String(Character.toChars(actual_code_point)) + "'.");
        }
    }

    private int expectAny(String description) {
        if (exhausted()) {
            throw new IllegalArgumentException(
                    "Expected " + description + ", but found end-of-string.");
        }
        int code_point = peek();
        if (!Character.isValidCodePoint(code_point)) {
            throw new IllegalArgumentException(
                    "Expected " + description
                            + ", but found invalid code point \\u"
                            + StringUtils.leftPad(Integer.toString(
                                    code_point, 16), 4)
                            + ".");
        }
        consumeCodePoint();
        return code_point;
    }

    private int expectAnyOf(String options, String description) {
        if (exhausted()) {
            throw new IllegalArgumentException(
                    "Expected " + description + ", but found end-of-string.");
        }
        int actual_code_point = peek();
        int index = 0;
        while (index < options.length()) {
            int expected_code_point = options.codePointAt(index);
            index += Character.charCount(expected_code_point);
            if (actual_code_point == expected_code_point) {
                return consumeCodePoint();
            }
        }
        throw new IllegalArgumentException("Expected " + description
                + ", but found '"
                + new String(Character.toChars(actual_code_point)) + "'.");
    }

    private int expectDecimalDigit() {
        return expectAnyOf("0123456789",
                "'0', '1', '2', '3', '4', '5', '6', '7', or '9'");
    }

    private int fourHexDigits() {
        StringBuilder four_digits = new StringBuilder(4);
        while (four_digits.length() < 4) {
            four_digits.appendCodePoint(
                    expectAnyOf(
                            "0123456789ABCDEFabcdef",
                            "hexadecimal digit"));
        }
        return Integer.valueOf(four_digits.toString(), 16);
    }

    private boolean isNext(int code_point) {
        return !exhausted() && peek() == code_point;
    }

    private boolean isNextAnyOf(String options) {
        if (exhausted()) {
            return false;
        }
        int actual_code_point = peek();
        int index = 0;
        while (index < options.length()) {
            int expected_code_point = options.codePointAt(index);
            index += Character.charCount(expected_code_point);
            if (actual_code_point == expected_code_point) {
                return true;
            }
        }
        return false;
    }

    private List<Object> jsonArray() {
        // We use an ArrayList because in general, it performs better than a
        // LinkedList.
        expect('[');
        acceptWhitespace();
        if (accept(']')) {
            return Collections.emptyList();
        }
        ArrayList<Object> members = new ArrayList<>();
        boolean array_closed = false;
        while (!array_closed) {
            members.add(jsonValue());
            acceptWhitespace();
            if (accept(']')) {
                array_closed = true;
            } else {
                expect(',');
                acceptWhitespace();
            }
        }
        return members;
    }

    private Number jsonNumber() {
        // First, we copy the number into a string builder. This way, we know
        // that we have a valid number, and we know where it ends.
        StringBuilder sb = new StringBuilder();
        sb.append(jsonNumberIntPart());
        if (accept('.')) {
            sb.appendCodePoint('.');
            sb.append(jsonNumberDigitsPart(false));
        }
        Optional<Integer> e_code_point = acceptAnyOf("eE");
        if (e_code_point.isPresent()) {
            sb.appendCodePoint(e_code_point.get());
            if (accept('+')) {
                sb.appendCodePoint('+');
            } else if (accept('-')) {
                sb.appendCodePoint('-');
            }
            sb.append(jsonNumberDigitsPart(false));
        }
        BigDecimal number = new BigDecimal(sb.toString());
        try {
            return number.byteValueExact();
        } catch (ArithmeticException e) {
            // Ignore any exception that might occur here, we simply continue
            // with other conversions.
        }
        try {
            return number.shortValueExact();
        } catch (ArithmeticException e) {
            // Ignore any exception that might occur here, we simply continue
            // with other conversions.
        }
        try {
            return number.intValueExact();
        } catch (ArithmeticException e) {
            // Ignore any exception that might occur here, we simply continue
            // with other conversions.
        }
        try {
            return number.longValueExact();
        } catch (ArithmeticException e) {
            // Ignore any exception that might occur here, we simply continue
            // with other conversions.
        }
        float number_as_float = number.floatValue();
        if (Float.isFinite(number_as_float)
                && BigDecimal.valueOf(number_as_float).equals(number)) {
            return number_as_float;
        }
        double number_as_double = number.doubleValue();
        if (Double.isFinite(number_as_double)
                && BigDecimal.valueOf(number_as_double).equals(number)) {
            return number_as_double;
        }
        try {
            return number.toBigIntegerExact();
        } catch (ArithmeticException e) {
            // Ignore any exception that might occur here, we simply return the
            // BigDecimal.
        }
        return number;
    }

    private CharSequence jsonNumberDigitsPart(boolean optional) {
        StringBuilder sb = new StringBuilder();
        if (!optional) {
            int digitCodePoint = expectDecimalDigit();
            sb.appendCodePoint(digitCodePoint);
        }
        Optional<Integer> next_digit_code_point = acceptAnyOf("0123456789");
        while (next_digit_code_point.isPresent()) {
            sb.appendCodePoint(next_digit_code_point.get());
            next_digit_code_point = acceptAnyOf("0123456789");
        }
        return sb;
    }

    private CharSequence jsonNumberIntPart() {
        StringBuilder sb = new StringBuilder();
        if (accept('-')) {
            sb.appendCodePoint('-');
        }
        int digit_code_point = expectDecimalDigit();
        sb.appendCodePoint(digit_code_point);
        if (digit_code_point == '0') {
            return sb;
        }
        sb.append(jsonNumberDigitsPart(true));
        return sb;
    }

    private Map<String, Object> jsonObject() {
        expect('{');
        acceptWhitespace();
        if (accept('}')) {
            return Collections.emptyMap();
        }
        // We use a linked hash-map so that the order of members is
        // preserved.
        LinkedHashMap<String, Object> members = new LinkedHashMap<>();
        boolean object_closed = false;
        while (!object_closed) {
            Pair<String, Object> member = jsonObjectMember();
            // This is a SIMPLE parser, so we do not support duplicate keys
            // (even though the JSON specification basically allows them).
            if (members.put(member.getLeft(), member.getRight()) != null) {
                throw new IllegalArgumentException(
                        "Found duplicate key \""
                                + escapeAndShorten(member.getLeft())
                                + "\" in object.");
            }
            acceptWhitespace();
            if (accept('}')) {
                object_closed = true;
            } else {
                expect(',');
                acceptWhitespace();
            }
        }
        return members;
    }

    private Pair<String, Object> jsonObjectMember() {
        String key = jsonString();
        acceptWhitespace();
        expect(':');
        acceptWhitespace();
        Object value = jsonValue();
        return Pair.of(key, value);
    }

    private String jsonString() {
        expect('"');
        StringBuilder content = new StringBuilder();
        boolean string_closed = false;
        while (!string_closed) {
            if (accept('"')) {
                string_closed = true;
            } else if (accept('\\')) {
                int codePoint = expectAnyOf(
                        "\"\\/bfnrtu",
                        "any of '\"', '\\', '/', 'b', 'f', 'n', 'r', 't', or 'u'");
                switch (codePoint) {
                case '"':
                case '\\':
                case '/':
                    content.appendCodePoint(codePoint);
                    break;
                case 'b':
                    content.appendCodePoint('\b');
                    break;
                case 'f':
                    content.appendCodePoint('\f');
                    break;
                case 'n':
                    content.appendCodePoint('\n');
                    break;
                case 'r':
                    content.appendCodePoint('\r');
                    break;
                case 't':
                    content.appendCodePoint('\t');
                    break;
                case 'u':
                    // Unicode sequence.
                    int hex_code_point = fourHexDigits();
                    if (!Character.isValidCodePoint(hex_code_point)) {
                        String hex_code_point_as_string = StringUtils.leftPad(
                                Integer.toString(hex_code_point, 16),
                                4);
                        throw new IllegalArgumentException(
                                "Illegal code point specified in unicode "
                                        + "sequence \\u"
                                        + hex_code_point_as_string
                                        + ".");
                    }
                    content.appendCodePoint(hex_code_point);
                    break;
                default:
                    // We matched all characters that we passed to the expect
                    // method, so we really should not find any other ones.
                    throw new RuntimeException("Internal logic error.");
                }
            } else {
                int code_point = expectAny("valid string content");
                if (code_point > 0 && code_point < 0x20) {
                    String code_point_as_string = StringUtils.leftPad(
                            Integer.toString(code_point), 2, '0');
                    throw new IllegalArgumentException(
                            "Expected valid string content, but found invalid "
                                    + "control character 0x"
                                    + code_point_as_string
                                    + ".");
                }
                content.appendCodePoint(code_point);
            }
        }
        return content.toString();
    }

    private Object jsonValue() {
        if (isNext('"')) {
            return jsonString();
        } else if (isNext('{')) {
            return jsonObject();
        } else if (isNext('[')) {
            return jsonArray();
        } else if (accept("true")) {
            return Boolean.TRUE;
        } else if (accept("false")) {
            return Boolean.FALSE;
        } else if (accept("null")) {
            return null;
        } else if (isNextAnyOf("-0123456789")) {
            return jsonNumber();
        } else {
            throw new IllegalArgumentException(
                    "Expected JSON value, but found \"" + escapeAndShorten()
                            + "\".");
        }
    }

    private Object parse() {
        // We always throw an IllegalArgumentException, so we can specifically
        // catch it.
        String error_message;
        try {
            Object obj = jsonValue();
            if (position < parsed_string.length()) {
                throw new IllegalArgumentException(
                        "Expected end-of-string, but found \""
                                + escapeAndShorten() + "\".");
            }
            return obj;
        } catch (IllegalArgumentException e) {
            error_message = e.getMessage();
        }
        // We use the position information to determine where the problem
        // happened. This can help the user to find the problem in the document.
        int line = 0;
        int column = 0;
        boolean last_char_was_cr = false;

        PrimitiveIterator.OfInt code_point_iterator = (
                parsed_string.codePoints().iterator());
        while (code_point_iterator.hasNext()) {
            int code_point = code_point_iterator.nextInt();
            // We ignore a newline directly after a carriage return, if we did
            // not, we would mess up our line count for documents using CR LF as
            // the end-of-line sequence.
            if (last_char_was_cr && code_point == '\n') {
                last_char_was_cr = false;
                continue;
            }
            last_char_was_cr = false;
            if (code_point == '\r') {
                last_char_was_cr = true;
                ++line;
                column = 0;
            } else if (code_point == '\n') {
                ++line;
                column = 0;
            } else {
                ++column;
            }
        }
        // Most users expect one-based line and column numbers, so we add one
        // when including them in the error message.
        throw new IllegalArgumentException("Error at line " + (line + 1)
                + " column " + (column + 1) + ": " + error_message);
    }

    private int peek() {
        // We assume that this method is only called after checking that we have
        // not reached the end of the string.
        return parsed_string.codePointAt(position);
    }
}
