/*******************************************************************************
 * Copyright (c) 2013-2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.archive.reader.json.internal;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.epics.util.array.ListDouble;
import org.epics.util.array.ListInteger;
import org.epics.util.array.ListLong;
import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLong;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;

import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Reads a {@link org.epics.vtype.VType} from a {@link JsonParser}.
 */
public final class JsonVTypeReader {

    private enum ValueType {
        DOUBLE("double"),
        ENUM("enum"),
        LONG("long"),
        MIN_MAX_DOUBLE("minMaxDouble"),
        STRING("string");

        public final String name;

        ValueType(String name) {
            this.name = name;
        }

    }

    private final static BigInteger ONE_BILLION = BigInteger
            .valueOf(1000000000L);

    private JsonVTypeReader() {
    }

    /**
     * Reads a {@link VType} value from a {@link JsonParser}. When calling this
     * method, the parser’s current token must be {@link JsonToken#START_OBJECT
     * START_OBJECT} and when the method returns successfully, the parser’s
     * current token is the corresponding {@link JsonToken#END_OBJECT
     * END_OBJECT}.
     *
     * @param parser
     *  JSON parser from which the tokens are read.
     * @param honor_zero_precision
     *  whether a precision of zero should result in no fractional digits being
     *  used in the number format (<code>true</code>) or a default number
     *  format should be used when the precision is zero (<code>false</code>).
     *  This only applies to floating-point values. Integer values always use
     *  a number format that does not include fractional digits.
     * @return value representing the parsed JSON object.
     * @throws IOException
     *  if the JSON data is malformed or there is an I/O problem.
     */
    public static VType readValue(
            final JsonParser parser, boolean honor_zero_precision)
            throws IOException {
        JsonToken token = parser.getCurrentToken();
        if (token != JsonToken.START_OBJECT) {
            throw new JsonParseException(
                    parser,
                    "Expected START_OBJECT but got " + token,
                    parser.getTokenLocation());
        }
        Display display = null;
        List<Double> double_value = null;
        EnumDisplay enum_display = null;
        List<Integer> enum_value = null;
        String field_name = null;
        boolean found_value = false;
        List<Long> long_value = null;
        Double maximum = null;
        Double minimum = null;
        String quality = null;
        AlarmSeverity severity = null;
        String status = null;
        Instant timestamp = null;
        ValueType type = null;
        List<String> string_value = null;
        while (true) {
            token = parser.nextToken();
            if (token == null) {
                throw new IOException("Unexpected end of stream.");
            }
            if (token == JsonToken.END_OBJECT) {
                break;
            }
            if (field_name == null) {
                if (token != JsonToken.FIELD_NAME) {
                    throw new JsonParseException(
                            parser,
                            "Expected FIELD_NAME but got " + token,
                            parser.getTokenLocation());
                }
                field_name = parser.getCurrentName();
                continue;
            }
            switch (field_name) {
                case "maximum" -> {
                    duplicateFieldIfNotNull(parser, field_name, maximum);
                    maximum = readDoubleValue(parser);
                }
                case "metaData" -> {
                    if (enum_display != null || display != null) {
                        throw new JsonParseException(
                                parser,
                                "Field \"" + field_name + "\" occurs twice.",
                                parser.getTokenLocation());
                    }
                    Object metaData = readMetaData(
                            parser, honor_zero_precision);
                    if (metaData instanceof Display) {
                        display = (Display) metaData;
                    } else if (metaData instanceof EnumDisplay) {
                        enum_display = (EnumDisplay) metaData;
                    } else {
                        throw new RuntimeException(
                                "Return value of internal method readMetaData "
                                        + "has unexpected type "
                                        + metaData.getClass().getName()
                                        + ".");
                    }
                }
                case "minimum" -> {
                    duplicateFieldIfNotNull(parser, field_name, minimum);
                    minimum = readDoubleValue(parser);
                }
                case "quality" -> {
                    // We do not use the quality field any longer (Phoebus’s
                    // VType system does not support it), but we still want to
                    // ensure that the data is well-formed.
                    duplicateFieldIfNotNull(parser, field_name, quality);
                    quality = readStringValue(parser);
                }
                case "severity" -> {
                    duplicateFieldIfNotNull(parser, field_name, severity);
                    severity = readSeverity(parser);
                }
                case "status" -> {
                    duplicateFieldIfNotNull(parser, field_name, status);
                    status = readStringValue(parser);
                }
                case "time" -> {
                    duplicateFieldIfNotNull(parser, field_name, timestamp);
                    timestamp = readInstant(parser);
                }
                case "type" -> {
                    duplicateFieldIfNotNull(parser, field_name, type);
                    final var type_name = readStringValue(parser);
                    type = switch (type_name.toLowerCase(Locale.ROOT)) {
                        case "double" -> ValueType.DOUBLE;
                        case "enum" -> ValueType.ENUM;
                        case "long" -> ValueType.LONG;
                        case "minmaxdouble" -> ValueType.MIN_MAX_DOUBLE;
                        case "string" -> ValueType.STRING;
                        default -> throw new JsonParseException(
                                parser,
                                "Unknown type \"" + type_name + "\".",
                                parser.getTokenLocation());
                    };
                }
                case"value" -> {
                    if (found_value) {
                        throw new JsonParseException(
                                parser,
                                "Field \"" + field_name + "\" occurs twice.",
                                parser.getTokenLocation());
                    }
                    if (type == null) {
                        throw new JsonParseException(
                                parser,
                                "\"value\" field must be specified after "
                                        + "\"type\" field.",
                                parser.getTokenLocation());
                    }
                    found_value = true;
                    switch (type) {
                        case DOUBLE, MIN_MAX_DOUBLE -> {
                            double_value = readDoubleArray(parser);
                        }
                        case ENUM -> {
                            enum_value = readIntArray(parser);
                        }
                        case LONG -> {
                            long_value = readLongArray(parser);
                        }
                        case STRING ->  {
                            string_value = readStringArray(parser);
                        }
                    }
                }
                default -> throw new JsonParseException(
                        parser,
                        "Found unknown field \"" + field_name + "\".",
                        parser.getTokenLocation());
            }
            field_name = null;
        }
        if (!found_value
                || quality == null
                || severity == null
                || status == null
                || timestamp == null
                || type == null) {
            throw new JsonParseException(
                    parser,
                    "Mandatory field is missing in object.",
                    parser.getTokenLocation());
        }
        if (type != ValueType.ENUM && enum_display != null) {
            throw new JsonParseException(
                    parser,
                    "Value of type \""
                            + type.name
                            + "\" does not accept enum meta-data.",
                    parser.getTokenLocation());
        }
        if (type != ValueType.MIN_MAX_DOUBLE && (
                minimum != null || maximum != null)) {
            throw new JsonParseException(
                    parser,
                    "Invalid field specified for value of type\""
                            + type.name
                            + "\".",
                    parser.getTokenLocation());
        }
        if ((type == ValueType.ENUM || type == ValueType.STRING)
                && display != null) {
            throw new JsonParseException(
                    parser,
                    "Value of type \""
                            + type.name
                            + "\" does not accept numeric meta-data.",
                    parser.getTokenLocation());
        }
        final var alarm = Alarm.of(severity, AlarmStatus.NONE, status);
        final var time = Time.of(timestamp);
        switch (type) {
            case DOUBLE -> {
                if (display == null) {
                    display = Display.none();
                }
                if (double_value.size() == 1) {
                    return VDouble.of(
                            double_value.get(0), alarm, time, display);
                } else {
                    return VDoubleArray.of(
                            toListDouble(double_value),
                            alarm,
                            time,
                            display);
                }
            }
            case ENUM -> {
                // Ensure that we have labels for all indices.
                int min_value = Integer.MAX_VALUE;
                int max_value = Integer.MIN_VALUE;
                for (var i = 0; i < enum_value.size(); ++i) {
                    final var value = enum_value.get(i);
                    min_value = Math.min(min_value, value);
                    max_value = Math.max(max_value, value);
                }
                // If we have a negative value or we have a value without a
                // label, we cannot use the meta-data and return a regular
                // integer instead.
                if (min_value < 0
                        || max_value >= enum_display.getChoices().size()) {
                    enum_display = null;
                }
                // If there is no meta-data, we cannot return an enum because
                // an enum must have meta-data and this meta-data must include
                // labels for all values.
                if (enum_display == null) {
                    // If there are no labels, there is no benefit in returning
                    // an enum, so we rather return an integer type.
                    display = Display.of(
                            Range.undefined(),
                            Range.undefined(),
                            Range.undefined(),
                            Range.undefined(),
                            "",
                            NumberFormats.precisionFormat(0));
                    if (enum_value.size() == 1) {
                        return VInt.of(
                                enum_value.get(0),
                                alarm,
                                time,
                                display);
                    } else {
                        return VIntArray.of(
                                toListInteger(enum_value),
                                alarm,
                                time,
                                display);
                    }
                }
                if (enum_value.size() == 1) {
                    return VEnum.of(
                            enum_value.get(0), enum_display, alarm, time);
                } else {
                    return VEnumArray.of(
                            toListInteger(enum_value),
                            enum_display,
                            alarm,
                            time);
                }
            }
            case LONG -> {
                if (display == null) {
                    display = Display.none();
                } else if (display.getFormat()
                        .getMaximumFractionDigits() != 0) {
                    // The Display instance that was generated by readMetaData
                    // might use a number format that includes fractional
                    // digits because that function does not know yet that we
                    // are dealing with an integer value. In this case, we
                    // replace the number format with one that does not include
                    // fractional digits.
                    display = Display.of(
                            display.getDisplayRange(),
                            display. getAlarmRange(),
                            display.getWarningRange(),
                            display.getControlRange(),
                            display.getUnit(),
                            NumberFormats.precisionFormat(0),
                            display.getDescription());
                }
                if (long_value.size() == 1) {
                    return VLong.of(long_value.get(0), alarm, time, display);
                } else {
                    return VLongArray.of(
                            toListLong(long_value),
                            alarm,
                            time,
                            display);
                }
            }
            case MIN_MAX_DOUBLE -> {
                if (display == null) {
                    display = Display.none();
                }
                if (minimum == null || maximum == null) {
                    throw new JsonParseException(
                            parser,
                            "Mandatory field is missing in object.",
                            parser.getTokenLocation());
                }
                if (double_value.size() == 1) {
                    return VStatistics.of(
                            double_value.get(0),
                            Double.NaN,
                            minimum,
                            maximum,
                            0,
                            alarm,
                            time,
                            display);
                } else {
                    // There is no type for arrays with statistics, so we have
                    // to choose between dropping statistics information and
                    // dropping array elements. We choose to drop statistics
                    // information. This is supposed to be a rare exception
                    // anyway, there typically is no sense in building this
                    // kind of statistics for arrays.
                    return VDoubleArray.of(
                            toListDouble(double_value),
                            alarm,
                            time,
                            display);
                }
            }
            case STRING -> {
                if (string_value.size() == 1) {
                    return VString.of(string_value.get(0), alarm, time);
                } else {
                    return VStringArray.of(string_value, alarm, time);
                }
            }
        }
        throw new JsonParseException(
                parser,
                "Invalid value type \"" + type + "\".",
                parser.getTokenLocation());
    }

    private static Instant bigIntegerToTimestamp(final BigInteger big_int) {
        BigInteger[] quotient_and_remainder = big_int
                .divideAndRemainder(ONE_BILLION);
        return Instant.ofEpochSecond(
                quotient_and_remainder[0].longValue(),
                quotient_and_remainder[1].longValue());
    }

    private static void duplicateFieldIfNotNull(
            final JsonParser parser,
            final String field_name,
            final Object field_value)
            throws JsonParseException {
        if (field_value != null) {
            throw new JsonParseException(
                    parser,
                    "Field \"" + field_name + "\" occurs twice.",
                    parser.getTokenLocation());
        }
    }

    private static boolean readBooleanValue(final JsonParser parser)
            throws IOException {
        final var token = parser.currentToken();
        if (token != JsonToken.VALUE_TRUE
                && token != JsonToken.VALUE_FALSE) {
            throw new JsonParseException(
                    parser,
                    "Expected VALUE_TRUE or VALUE_FALSE but got "
                            + token,
                    parser.getTokenLocation());
        }
        return parser.getBooleanValue();
    }

    private static List<Double> readDoubleArray(
            final JsonParser parser) throws IOException {

        final List<Double> values = new ArrayList<>();
        var token = parser.getCurrentToken();
        if (token != JsonToken.START_ARRAY) {
            throw new JsonParseException(
                    parser,
                    "Expected START_ARRAY but got " + token,
                    parser.getTokenLocation());
        }
        while (true) {
            token = parser.nextToken();
            if (token == null) {
                throw new IOException("Unexpected end of stream.");
            }
            if (token == JsonToken.END_ARRAY) {
                break;
            }
            values.add(readDoubleValue(parser));
        }
        return Collections.unmodifiableList(values);
    }

    private static double readDoubleValue(final JsonParser parser)
            throws IOException {
        final var token = parser.currentToken();
        if (token != JsonToken.VALUE_NUMBER_INT
                && token != JsonToken.VALUE_NUMBER_FLOAT) {
            if (token != JsonToken.VALUE_STRING) {
                throw new JsonParseException(
                        parser,
                        "Expected VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT, or "
                                + "VALUE_STRING but got "
                                + token,
                        parser.getTokenLocation());
            }
            return stringToSpecialDouble(parser.getText(),
                    parser);
        } else {
            return parser.getDoubleValue();
        }
    }

    private static Instant readInstant(final JsonParser parser)
            throws IOException {
        final var token = parser.currentToken();
        if (token != JsonToken.VALUE_NUMBER_INT) {
            throw new JsonParseException(
                    parser,
                    "Expected VALUE_NUMBER_INT but got "
                            + token,
                    parser.getTokenLocation());
        }
        return bigIntegerToTimestamp(parser.getBigIntegerValue());
    }

    private static List<Integer> readIntArray(final JsonParser parser)
            throws IOException {
        final List<Integer> values = new ArrayList<>();
        var token = parser.getCurrentToken();
        if (token != JsonToken.START_ARRAY) {
            throw new JsonParseException(
                    parser,
                    "Expected START_ARRAY but got " + token,
                    parser.getTokenLocation());
        }
        while (true) {
            token = parser.nextToken();
            if (token == null) {
                throw new IOException("Unexpected end of stream.");
            }
            if (token == JsonToken.END_ARRAY) {
                break;
            }
            values.add(readIntValue(parser));
        }
        return Collections.unmodifiableList(values);
    }

    private static int readIntValue(final JsonParser parser)
            throws IOException {
        final var token = parser.getCurrentToken();
        if (token != JsonToken.VALUE_NUMBER_INT) {
            throw new JsonParseException(
                    parser,
                    "Expected VALUE_NUMBER_INT but got "
                            + token,
                    parser.getTokenLocation());
        }
        return parser.getIntValue();
    }

    private static List<Long> readLongArray(final JsonParser parser)
            throws IOException {
        final List<Long> values = new ArrayList<>();
        var token = parser.getCurrentToken();
        if (token != JsonToken.START_ARRAY) {
            throw new JsonParseException(
                    parser,
                    "Expected START_ARRAY but got " + token,
                    parser.getTokenLocation());
        }
        while (true) {
            token = parser.nextToken();
            if (token == null) {
                throw new IOException("Unexpected end of stream.");
            }
            if (token == JsonToken.END_ARRAY) {
                break;
            }
            values.add(readLongValue(parser));
        }
        return Collections.unmodifiableList(values);
    }

    private static long readLongValue(final JsonParser parser)
            throws IOException {
        final var token = parser.getCurrentToken();
        if (token != JsonToken.VALUE_NUMBER_INT) {
            throw new JsonParseException(
                    parser,
                    "Expected VALUE_NUMBER_INT but got "
                            + token,
                    parser.getTokenLocation());
        }
        return parser.getLongValue();
    }

    /**
     * Reads the meta-data associated with a value. There are different
     * types of meta-data for numeric and enum values, therefore the type of
     * the return value has to be determined at runtime.
     *
     * @param parser the JSON parser that is used to read the meta-data.
     * @param honor_zero_precision
     *  whether a precision of zero should result in no fractional digits being
     *  used in the number format (<code>true</code>) or a default number
     *  format should be used when the precision is zero (<code>false</code>).
     * @return
     *  an instance of {@link String}<code>[]</code> (storing the enum labels)
     *  or an instance of {@link Display} (storing numeric limits and number
     *  formatting information).
     * @throws IOException
     *  if an error occurs while parsing the JSON input (e.g. interrupted
     *  stream, malformed data).
     */
    private static Object readMetaData(
            final JsonParser parser, boolean honor_zero_precision)
            throws IOException {
        JsonToken token = parser.getCurrentToken();
        if (token == null) {
            throw new IOException("Unexpected end of stream.");
        }
        if (token != JsonToken.START_OBJECT) {
            throw new JsonParseException(
                    parser,
                    "Expected START_OBJECT but got " + token,
                    parser.getTokenLocation());
        }
        Double alarm_high = null;
        Double alarm_low = null;
        Double display_high = null;
        Double display_low = null;
        String field_name = null;
        Integer precision = null;
        List<String> states = null;
        String type = null;
        String units = null;
        Double warn_high = null;
        Double warn_low = null;
        while (true) {
            token = parser.nextToken();
            if (token == null) {
                throw new IOException("Unexpected end of stream.");
            }
            if (token == JsonToken.END_OBJECT) {
                break;
            }
            if (field_name == null) {
                if (token != JsonToken.FIELD_NAME) {
                    throw new JsonParseException(
                            parser,
                            "Expected FIELD_NAME but got " + token,
                            parser.getTokenLocation());
                }
                field_name = parser.getCurrentName();
                continue;
            }
            switch (field_name) {
                case "precision" -> {
                    duplicateFieldIfNotNull(parser, field_name, precision);
                    precision = readIntValue(parser);
                }
                case "type" -> {
                    duplicateFieldIfNotNull(parser, field_name, type);
                    type = readStringValue(parser);
                }
                case "units" -> {
                    duplicateFieldIfNotNull(parser, field_name, units);
                    units = readStringValue(parser);
                }
                case "displayLow" -> {
                    duplicateFieldIfNotNull(parser, field_name, display_low);
                    display_low = readDoubleValue(parser);
                }
                case "displayHigh" -> {
                    duplicateFieldIfNotNull(parser, field_name, display_high);
                    display_high = readDoubleValue(parser);
                }
                case "warnLow" -> {
                    duplicateFieldIfNotNull(parser, field_name, warn_low);
                    warn_low = readDoubleValue(parser);
                }
                case "warnHigh" -> {
                    duplicateFieldIfNotNull(parser, field_name, warn_high);
                    warn_high = readDoubleValue(parser);
                }
                case "alarmLow" -> {
                    duplicateFieldIfNotNull(parser, field_name, alarm_low);
                    alarm_low = readDoubleValue(parser);
                }
                case "alarmHigh" -> {
                    duplicateFieldIfNotNull(parser, field_name, alarm_high);
                    alarm_high = readDoubleValue(parser);
                }
                case "states" -> {
                    duplicateFieldIfNotNull(parser, field_name, states);
                    states = readStringArray(parser);
                }
                default -> throw new JsonParseException(
                        parser,
                        "Found unknown field \"" + field_name + "\".",
                        parser.getTokenLocation());
            }
            field_name = null;
        }
        if (type == null) {
            throw new JsonParseException(
                    parser,
                    "Mandatory field is missing in object.",
                    parser.getTokenLocation());

        }
        if (type.equalsIgnoreCase("enum")) {
            if (states == null) {
                throw new JsonParseException(
                        parser,
                        "Mandatory field is missing in object.",
                        parser.getTokenLocation());
            }
            if (alarm_high != null
                    || alarm_low != null
                    || display_high != null
                    || display_low != null
                    || precision != null
                    || units != null
                    || warn_high != null
                    || warn_low != null) {
                throw new JsonParseException(
                        parser,
                        "Invalid field specified for enum meta-data.",
                        parser.getTokenLocation());
            }
            return EnumDisplay.of(states);
        } else if (type.equalsIgnoreCase("numeric")) {
            if (alarm_high == null
                    || alarm_low == null
                    || display_high == null
                    || display_low == null
                    || precision == null
                    || units == null
                    || warn_high == null
                    || warn_low == null) {
                throw new JsonParseException(
                        parser,
                        "Mandatory field is missing in object.",
                        parser.getTokenLocation());
            }
            if (states != null) {
                throw new JsonParseException(
                        parser,
                        "Invalid field specified for numeric meta-data.",
                        parser.getTokenLocation());
            }
            final NumberFormat format;
            if (precision > 0 || (precision == 0 && honor_zero_precision)) {
                format = NumberFormats.precisionFormat(precision);
            } else {
                format = NumberFormats.toStringFormat();
            }
            return Display.of(
                    Range.of(display_low, display_high),
                    Range.of(alarm_low, alarm_high),
                    Range.of(warn_low, warn_high),
                    Range.undefined(),
                    units,
                    format);
        } else {
            throw new JsonParseException(
                    parser,
                    "Invalid meta-data type \"" + type + "\".",
                    parser.getTokenLocation());
        }
    }

    private static AlarmSeverity readSeverity(final JsonParser parser)
            throws IOException {
        var token = parser.getCurrentToken();
        if (token != JsonToken.START_OBJECT) {
            throw new JsonParseException(
                    parser,
                    "Expected START_OBJECT but got " + token,
                    parser.getTokenLocation());
        }
        String field_name = null;
        Boolean has_value = null;
        String level_string = null;
        while (true) {
            token = parser.nextToken();
            if (token == null) {
                throw new IOException("Unexpected end of stream.");
            }
            if (token == JsonToken.END_OBJECT) {
                break;
            }
            if (field_name == null) {
                if (token != JsonToken.FIELD_NAME) {
                    throw new JsonParseException(
                            parser,
                            "Expected FIELD_NAME but got " + token,
                            parser.getTokenLocation());
                }
                field_name = parser.getCurrentName();
            } else {
                if (field_name.equals("level")) {
                    duplicateFieldIfNotNull(parser, field_name, level_string);
                    level_string = readStringValue(parser);
                } else if (field_name.equals("hasValue")) {
                    // We do not use the hasValue field any longer (Phoebus’s
                    // VType system does not support it), but we still want to
                    // ensure that the data is well-formed.
                    duplicateFieldIfNotNull(parser, field_name, has_value);
                    has_value = readBooleanValue(parser);
                } else {
                    throw new JsonParseException(
                            parser,
                            "Found unknown field \"" + field_name + "\".",
                            parser.getTokenLocation());
                }
                field_name = null;
            }
        }
        if (has_value == null || level_string == null) {
            throw new JsonParseException(
                    parser,
                    "Mandatory field is missing in object.",
                    parser.getTokenLocation());
        }
        return switch(level_string.toUpperCase(Locale.ROOT)) {
            case "OK" -> AlarmSeverity.NONE;
            case "MINOR" -> AlarmSeverity.MINOR;
            case "MAJOR" -> AlarmSeverity.MAJOR;
            case "INVALID" -> AlarmSeverity.INVALID;
            default -> throw new JsonParseException(
                    parser,
                    "Unknown severity \"" + level_string + "\".",
                    parser.getTokenLocation());
        };
    }

    private static List<String> readStringArray(final JsonParser parser)
            throws IOException {
        final var elements = new LinkedList<String>();
        JsonToken token = parser.getCurrentToken();
        if (token != JsonToken.START_ARRAY) {
            throw new JsonParseException(
                    parser,
                    "Expected START_ARRAY but got " + token,
                    parser.getTokenLocation());
        }
        while (true) {
            token = parser.nextToken();
            if (token == null) {
                throw new IOException("Unexpected end of stream.");
            }
            if (token == JsonToken.END_ARRAY) {
                break;
            }
            if (token == JsonToken.VALUE_STRING) {
                elements.add(parser.getText());
            } else {
                throw new JsonParseException(
                        parser,
                        "Expected VALUE_STRING but got " + token,
                        parser.getTokenLocation());
            }
        }
        return elements;
    }

    private static String readStringValue(final JsonParser parser)
            throws IOException {
        final var token = parser.currentToken();
        if (token != JsonToken.VALUE_STRING) {
            throw new JsonParseException(
                    parser,
                    "Expected VALUE_STRING but got " + token,
                    parser.getTokenLocation());
        }
        return parser.getText();
    }

    private static double stringToSpecialDouble(
            final String value, final JsonParser parser) throws IOException {
        return switch (value.toLowerCase()) {
            case "inf", "infinity", "+inf", "+infinity" -> (
                    Double.POSITIVE_INFINITY);
            case "-inf", "-infinity" -> Double.NEGATIVE_INFINITY;
            case "nan" -> Double.NaN;
            default -> throw new JsonParseException(
                    parser,
                    "String \""
                            + value
                            + "\" does not qualify as a special double "
                            + "number.",
                    parser.getTokenLocation());
        };
    }

    private static ListDouble toListDouble(final List<Double> array) {
        return new ListDouble() {
            @Override
            public double getDouble(int index) {
                return array.get(index);
            }

            @Override
            public int size() {
                return array.size();
            }
        };
    }

    private static ListInteger toListInteger(final List<Integer> array) {
        return new ListInteger() {
            @Override
            public int getInt(int index) {
                return array.get(index);
            }

            @Override
            public int size() {
                return array.size();
            }
        };
    }

    private static ListLong toListLong(final List<Long> array) {
        return new ListLong() {
            @Override
            public long getLong(int index) {
                return array.get(index);
            }

            @Override
            public int size() {
                return array.size();
            }
        };
    }

}
