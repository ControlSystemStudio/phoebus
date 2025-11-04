/*******************************************************************************
 * Copyright (c) 2024-2025 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.pv.jackie.util;

import com.aquenos.epics.jackie.common.util.NullTerminatedStringUtil;
import com.aquenos.epics.jackie.common.value.ChannelAccessControlsValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessFloatingPointControlsValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessGraphicsEnum;
import com.aquenos.epics.jackie.common.value.ChannelAccessNumericControlsValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessSimpleOnlyValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessTimeChar;
import com.aquenos.epics.jackie.common.value.ChannelAccessTimeDouble;
import com.aquenos.epics.jackie.common.value.ChannelAccessTimeEnum;
import com.aquenos.epics.jackie.common.value.ChannelAccessTimeFloat;
import com.aquenos.epics.jackie.common.value.ChannelAccessTimeLong;
import com.aquenos.epics.jackie.common.value.ChannelAccessTimeShort;
import com.aquenos.epics.jackie.common.value.ChannelAccessTimeString;
import com.aquenos.epics.jackie.common.value.ChannelAccessTimeValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessValueFactory;
import org.epics.util.array.ListByte;
import org.epics.util.array.ListDouble;
import org.epics.util.array.ListFloat;
import org.epics.util.array.ListInteger;
import org.epics.util.array.ListShort;
import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VByte;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VFloat;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VShort;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.jackie.JackiePreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Converts between VTypes and Channel Access values.
 */
public final class ValueConverter {

    /**
     * Offset between the UNIX and EPICS epoch in seconds.
     */
    public static final long OFFSET_EPICS_TO_UNIX_EPOCH_SECONDS = 631152000L;

    /**
     * Logger used by this class.
     *
     * The field is package private, so that unit-tests can inject a custom
     * implementation.
     */
    static Logger log = LoggerFactory.getLogger(ValueConverter.class);

    private ValueConverter() {
    }

    /**
     * Converts a Channel Access value to a VType.
     * <p>
     * The underlying types of the <code>controls_value</code> and the
     * <code>time_value</code> must match.
     *
     * @param controls_value
     *  CA value from which the meta-data is used. May be <code>null></code>.
     *  If <code>null</code> the resulting value is constructed without
     *  meta-data.
     * @param time_value
     *  CA value from which the value, alarm severity and status, and time
     *  stamp are used.
     * @param charset
     *  charset that is used to convert arrays of bytes to strings (only
     *  relevant if <code>treat_char_as_long_string</code> is
     *  <code>true</code>).
     * @param force_array
     *  whether values with a single element should be converted to array
     *  VTypes.
     * @param honor_zero_precision
     *  whether floating-point values specifying a zero-precision should be
     *  rendered without any fractional digits. If <code>true</code>, they are
     *  rendered without fractional digits. If <code>false</code>, they are
     *  rendered using a default format.
     * @param treat_char_as_long_string
     *  whether values of type <code>DBR_CHAR_*</code> should be converted to
     *  strings.
     * @return
     *  VType representing the combination of <code>controls_value</code> and
     *  <code>time_value</code>.
     * @throws IllegalArgumentException
     *  if the underlying base types of <code>controls_value</code> and
     *  <code>time_value</code> do not match.
     */
    public static VType channelAccessToVType(
            ChannelAccessControlsValue<?> controls_value,
            ChannelAccessTimeValue<?> time_value,
            Charset charset,
            boolean force_array,
            boolean honor_zero_precision,
            boolean treat_char_as_long_string) {
        if (time_value == null) {
            throw new NullPointerException("time_value must not be null.");
        }
        // The controls and time values must have compatible types.
        if (controls_value != null
                && controls_value.getType().toSimpleType()
                != time_value.getType().toSimpleType()) {
            throw new IllegalArgumentException(
                    "Value of type " + controls_value.getType()
                            + " is not compatible with value of type "
                            + time_value.getType() + ".");
        }
        Alarm alarm = convertAlarm(time_value);
        Time time = convertTime(time_value);
        Display display = convertDisplay(controls_value, honor_zero_precision);
        return switch (time_value.getType()) {
            case DBR_TIME_CHAR -> {
                ChannelAccessTimeChar typed_time_value = (ChannelAccessTimeChar) time_value;
                if (treat_char_as_long_string) {
                    ByteBuffer buffer = typed_time_value.getValue();
                    byte[] bytes;
                    if (buffer.hasArray()) {
                        bytes = buffer.array();
                    } else {
                        bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                    }
                    String stringValue = NullTerminatedStringUtil.nullTerminatedBytesToString(
                            bytes,
                            charset);
                    yield VString.of(stringValue, alarm, time);
                } else if (typed_time_value.getValue().remaining() == 1 && !force_array) {
                    yield VByte.of(
                            typed_time_value.getValue().get(0),
                            alarm,
                            time,
                            display);
                } else {
                    yield VByteArray.of(
                            byteBufferToListByte(typed_time_value.getValue()),
                            alarm,
                            time,
                            display);
                }
            }
            case DBR_TIME_DOUBLE -> {
                ChannelAccessTimeDouble typed_time_value = (ChannelAccessTimeDouble) time_value;
                if (typed_time_value.getValue().remaining() == 1 && !force_array) {
                    yield VDouble.of(
                            typed_time_value.getValue().get(0),
                            alarm,
                            time,
                            display);
                } else {
                    yield VDoubleArray.of(
                            doubleBufferToListDouble(typed_time_value.getValue()),
                            alarm,
                            time,
                            display);
                }
            }
            case DBR_TIME_ENUM -> {
                final var typed_time_value = (ChannelAccessTimeEnum) time_value;
                if (typed_time_value.getValue().remaining() == 1 && !force_array) {
                    final var value = typed_time_value.getValue().get(0);
                    // If the value is in a reasonable range ([0, 15]), we
                    // generate the enum display and return a VEnum, Otherwise,
                    // we return a VShort. We also do this if we cannot
                    // generate the enum display for some other reason.
                    final EnumDisplay enum_display;
                    if (value >= 0 && value <= 15) {
                        enum_display = convertEnumDisplay(
                                controls_value, value + 1);
                    } else {
                        enum_display = null;
                    }
                    if (enum_display == null) {
                        yield VShort.of(value, alarm, time, Display.none());
                    }
                    yield VEnum.of(
                            typed_time_value.getValue().get(0),
                            enum_display,
                            alarm,
                            time);
                } else {
                    final var list_short = shortBufferToListShort(
                            typed_time_value.getValue());
                    var min_value = Short.MAX_VALUE;
                    var max_value = Short.MIN_VALUE;
                    final var list_short_iterator = list_short.iterator();
                    while (list_short_iterator.hasNext()) {
                        final var value = list_short_iterator.nextShort();
                        min_value = (min_value > value) ? value : min_value;
                        max_value = (max_value < value) ? value : max_value;
                    }
                    // If all values are in a reasonable range ([0, 15]), we
                    // generate the enum display and return a VEnum, Otherwise,
                    // we return a VShort. We also do this if we cannot
                    // generate the enum display for some other reason.
                    final EnumDisplay enum_display;
                    if (min_value >= 0 && max_value <= 15) {
                        enum_display = convertEnumDisplay(
                                controls_value, max_value + 1);
                    } else {
                        enum_display = null;
                    }
                    if (enum_display == null) {
                        yield VShortArray.of(
                                list_short, alarm, time, Display.none());
                    }
                    yield VEnumArray.of(
                            list_short,
                            enum_display,
                            alarm,
                            time);
                }
            }
            case DBR_TIME_FLOAT -> {
                ChannelAccessTimeFloat typed_time_value = (ChannelAccessTimeFloat) time_value;
                if (typed_time_value.getValue().remaining() == 1 && !force_array) {
                    yield VFloat.of(
                            typed_time_value.getValue().get(0),
                            alarm,
                            time,
                            display);
                } else {
                    yield VFloatArray.of(
                            floatBufferToListFloat(typed_time_value.getValue()),
                            alarm,
                            time,
                            display);
                }
            }
            case DBR_TIME_LONG -> {
                ChannelAccessTimeLong typed_time_value = (ChannelAccessTimeLong) time_value;
                if (typed_time_value.getValue().remaining() == 1 && !force_array) {
                    yield VInt.of(
                            typed_time_value.getValue().get(0),
                            alarm,
                            time,
                            display);
                } else {
                    yield VIntArray.of(
                            intBufferToListInteger(typed_time_value.getValue()),
                            alarm,
                            time,
                            display);
                }
            }
            case DBR_TIME_SHORT -> {
                ChannelAccessTimeShort typed_time_value = (ChannelAccessTimeShort) time_value;
                if (typed_time_value.getValue().remaining() == 1 && !force_array) {
                    yield VShort.of(
                            typed_time_value.getValue().get(0),
                            alarm,
                            time,
                            display);
                } else {
                    yield VShortArray.of(
                            shortBufferToListShort(typed_time_value.getValue()),
                            alarm,
                            time,
                            display);
                }
            }
            case DBR_TIME_STRING -> {
                ChannelAccessTimeString typed_time_value = (ChannelAccessTimeString) time_value;
                if (typed_time_value.getValue().size() == 1 && !force_array) {
                    yield VString.of(
                            typed_time_value.getValue().get(0),
                            alarm,
                            time);
                } else {
                    yield VStringArray.of(
                            typed_time_value.getValue(),
                            alarm,
                            time);
                }
            }
            default ->
                // This should never happen and indicates a bug in EPICS
                // Jackie.
                throw new RuntimeException(
                        "Instance of ChannelAccessTimeValue has unexpected type "
                                + time_value.getType() + ": " + time_value);
        };
    }

    /**
     * Converts an object to a value that can be sent via Channel access. This
     * method supports most {@link VType} objects (through the help of
     * {@link VTypeHelper#toObject(VType)}), the primitive Java types
     * <code>byte</code>, <code>double</code>, <code>float</code>,
     * <code>int</code>, and <code>short</code>, arrays of these primitive
     * types, {@link String}, and arrays of {@link String}.
     *
     * @param pv_name
     *  name of the process variable for which the value is converted. This is
     *  only used for logging purposes.
     * @param object
     *  object to be converted.
     * @param charset
     *  charset to be used when converting strings.
     * @param convert_string_as_long_string
     *  indicates whether a {@link String} or single element
     *  <code>String[]</code> array should be converted to a
     *  <code>DBR_CHAR</code> instead of a <code>DBR_STRING</code>.
     * @param long_conversion_mode
     *  behavior when converting a {@link Long} value that does not fit into an
     *  {@link Integer}.
     * @return
     *  the converted value.
     * @throws IllegalArgumentException
     *  if <code>object</code> cannot be converted.
     */
    public static ChannelAccessSimpleOnlyValue<?> objectToChannelAccessSimpleOnlyValue(
            String pv_name,
            Object object,
            Charset charset,
            boolean convert_string_as_long_string,
            JackiePreferences.LongConversionMode long_conversion_mode) {
        if (object instanceof VType vtype) {
            var converted_object = VTypeHelper.toObject(vtype);
            // VTypeHelper.toObject returns null if it does not know how to
            // convert the object. In this case, we rather want to keep the
            // original object that we got, so that the resulting error message
            // is more specific.
            if (converted_object != null) {
                object = converted_object;
            }
        }
        if (object instanceof Byte value) {
            return ChannelAccessValueFactory.createChar(new byte[] {value});
        }
        if (object instanceof byte[] value) {
            return ChannelAccessValueFactory.createChar(value);
        }
        if (object instanceof Double value) {
            return ChannelAccessValueFactory.createDouble(new double[] {value});
        }
        if (object instanceof double[] value) {
            return ChannelAccessValueFactory.createDouble(value);
        }
        if (object instanceof Float value) {
            return ChannelAccessValueFactory.createFloat(new float[] {value});
        }
        if (object instanceof float[] value) {
            return ChannelAccessValueFactory.createFloat(value);
        }
        if (object instanceof Integer value) {
            return ChannelAccessValueFactory.createLong(new int[] {value});
        }
        if (object instanceof int[] value) {
            return ChannelAccessValueFactory.createLong(value);
        }
        if (object instanceof Long value) {
            if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return ChannelAccessValueFactory.createLong(
                        new int[]{value.intValue()});
            }
            return switch (long_conversion_mode) {
                case COERCE -> ChannelAccessValueFactory.createLong(
                        new int[]{coerceToInt(value)});
                case COERCE_AND_WARN -> {
                    final var coerced_value = coerceToInt(value);
                    log.warn(
                            "Writing long {} as int {} for PV {}",
                            value,
                            coerced_value,
                            pv_name);
                    yield ChannelAccessValueFactory.createLong(
                            new int[]{coerced_value});
                }
                case CONVERT -> ChannelAccessValueFactory.createDouble(
                        new double[]{value.doubleValue()});
                case CONVERT_AND_WARN -> {
                    final var converted_value = value.doubleValue();
                    log.warn(
                            "Writing long {} as double {} for PV {}",
                            value,
                            converted_value,
                            pv_name);
                    yield ChannelAccessValueFactory.createDouble(
                            new double[]{converted_value});
                }
                case FAIL -> throw new IllegalArgumentException(
                        "Cannot write long value " + value
                                + ". The value is outside the range of a "
                                + "32-bit signed integer.");
                case TRUNCATE -> ChannelAccessValueFactory.createLong(
                        new int[]{value.intValue()});
                case TRUNCATE_AND_WARN -> {
                    final var truncated_value = value.intValue();
                    log.warn(
                            "Writing long {} as int {} for PV {}",
                            value,
                            truncated_value,
                            pv_name);
                    yield ChannelAccessValueFactory.createLong(
                            new int[]{truncated_value});
                }
            };
        }
        if (object instanceof long[] value) {
            boolean limit_exceeded = false;
            for (long element : value) {
                if (element < Integer.MIN_VALUE
                        || element > Integer.MAX_VALUE) {
                    limit_exceeded = true;
                    break;
                }
            }
            if (!limit_exceeded) {
                return ChannelAccessValueFactory.createLong(
                        longArrayToIntArray(value));
            }
            return switch (long_conversion_mode) {
                case COERCE -> ChannelAccessValueFactory.createLong(
                        coerceToInt(value));
                case COERCE_AND_WARN -> {
                    final var coerced_value = coerceToInt(value);
                    log.warn(
                            "Writing long[] {} as int[] {} for PV {}",
                            Arrays.toString(value),
                            Arrays.toString(coerced_value),
                            pv_name);
                    yield ChannelAccessValueFactory.createLong(
                            coerced_value);
                }
                case CONVERT -> ChannelAccessValueFactory.createDouble(
                        longArrayToDoubleArray(value));
                case CONVERT_AND_WARN -> {
                    final var converted_value = longArrayToDoubleArray(value);
                    log.warn(
                            "Writing long[] {} as double[] {} for PV {}",
                            Arrays.toString(value),
                            Arrays.toString(converted_value),
                            pv_name);
                    yield ChannelAccessValueFactory.createDouble(
                            converted_value);
                }
                case FAIL -> throw new IllegalArgumentException(
                        "Cannot write long[] value " + Arrays.toString(value)
                                + ". The value is outside the range of a "
                                + "32-bit signed integer.");
                case TRUNCATE -> ChannelAccessValueFactory.createLong(
                        longArrayToIntArray(value));
                case TRUNCATE_AND_WARN -> {
                    final var truncated_value = longArrayToIntArray(value);
                    log.warn(
                            "Writing long[] {} as int[] {} for PV {}",
                            Arrays.toString(value),
                            Arrays.toString(truncated_value),
                            pv_name);
                    yield ChannelAccessValueFactory.createLong(
                            truncated_value);
                }
            };
        }
        if (object instanceof Short value) {
            return ChannelAccessValueFactory.createShort(new short[] {value});
        }
        if (object instanceof short[] value) {
            return ChannelAccessValueFactory.createShort(value);
        }
        if (object instanceof String value) {
            if (convert_string_as_long_string) {
                // Convert string to an array of bytes.
                final var byte_buffer = charset.encode(value);
                final var byte_array = new byte[byte_buffer.remaining()];
                byte_buffer.get(byte_array);
                return ChannelAccessValueFactory.createChar(byte_array);
            }
            return ChannelAccessValueFactory.createString(
                    Collections.singleton(value), charset);
        }
        if (object instanceof String[] value) {
            // In case of a string array, we can only use the long-string
            // conversion if the array has a single element.
            if (value.length == 1 && convert_string_as_long_string) {
                return objectToChannelAccessSimpleOnlyValue(
                        pv_name, value[0], charset, true, long_conversion_mode);
            }
            return ChannelAccessValueFactory.createString(
                    Arrays.asList(value), charset);
        }
        throw new IllegalArgumentException(
                "Cannot convert object of type "
                        + object.getClass().getName()
                        + ": "
                        + object);
    }

    private static ListByte byteBufferToListByte(ByteBuffer buffer) {
        return new ListByte() {
            @Override
            public byte getByte(int index) {
                return buffer.get(index);
            }

            @Override
            public int size() {
                return buffer.remaining();
            }
        };
    }

    private static int coerceToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        } else {
            return (int) value;
        }
    }

    private static int[] coerceToInt(long[] array) {
        return Arrays.stream(array).mapToInt(
                ValueConverter::coerceToInt).toArray();
    }

    private static Alarm convertAlarm(ChannelAccessTimeValue<?> time_value) {
        AlarmSeverity severity = switch (time_value.getAlarmSeverity()) {
            case NO_ALARM -> AlarmSeverity.NONE;
            case MINOR_ALARM -> AlarmSeverity.MINOR;
            case MAJOR_ALARM -> AlarmSeverity.MAJOR;
            case INVALID_ALARM -> AlarmSeverity.INVALID;
        };
        return Alarm.of(
                severity,
                AlarmStatus.NONE,
                time_value.getAlarmStatus().toString());
    }

    private static Display convertDisplay(
            ChannelAccessControlsValue<?> controls_value,
            boolean honor_zero_precision) {
        if (controls_value instanceof ChannelAccessNumericControlsValue<?> numeric_value) {
            Range alarm_range = Range.of(
                    numeric_value.getGenericLowerAlarmLimit().doubleValue(),
                    numeric_value.getGenericUpperAlarmLimit().doubleValue());
            Range control_range = Range.of(
                    numeric_value.getGenericLowerControlLimit().doubleValue(),
                    numeric_value.getGenericUpperControlLimit().doubleValue());
            Range display_range = Range.of(
                    numeric_value.getGenericLowerDisplayLimit().doubleValue(),
                    numeric_value.getGenericUpperDisplayLimit().doubleValue());
            Range warning_range = Range.of(
                    numeric_value.getGenericLowerWarningLimit().doubleValue(),
                    numeric_value.getGenericUpperWarningLimit().doubleValue());
            String units = numeric_value.getUnits();
            short precision = 0;
            if (numeric_value instanceof ChannelAccessFloatingPointControlsValue<?> fp_value) {
                precision = fp_value.getPrecision();
            }
            NumberFormat number_format;
            if (precision > 0 || (honor_zero_precision && precision == 0)) {
                number_format = NumberFormats.precisionFormat(precision);
            } else {
                number_format = NumberFormats.toStringFormat();
            }
            return Display.of(
                    display_range,
                    alarm_range,
                    warning_range,
                    control_range,
                    units,
                    number_format);
        }
        return Display.none();
    }

    private static EnumDisplay convertEnumDisplay(
            ChannelAccessControlsValue<?> controls_value,
            int min_number_of_labels) {
        if (controls_value instanceof ChannelAccessGraphicsEnum enum_value) {
            final var original_labels = enum_value.getLabels();
            // If the highest does not have a label in the meta-data, we have
            // to generate such a label. Otherwise, we would get an
            // IndexOutOfBoundsError when trying to create the VEnum.
            if (min_number_of_labels <= original_labels.size()) {
                return EnumDisplay.of(original_labels);
            }
            var labels = new ArrayList<String>(min_number_of_labels);
            for (int index = 0; index < min_number_of_labels; ++index) {
                if (index < original_labels.size()) {
                    labels.add(original_labels.get(index));
                } else {
                    labels.add("Index " + index);
                }
            }
            return EnumDisplay.of(labels);
        }
        return null;
    }

    private static Time convertTime(ChannelAccessTimeValue<?> time_value) {
        return Time.of(Instant.ofEpochSecond(
                time_value.getTimeSeconds()
                        + OFFSET_EPICS_TO_UNIX_EPOCH_SECONDS,
                time_value.getTimeNanoseconds()));
    }

    private static ListDouble doubleBufferToListDouble(DoubleBuffer buffer) {
        return new ListDouble() {
            @Override
            public double getDouble(int index) {
                return buffer.get(index);
            }

            @Override
            public int size() {
                return buffer.remaining();
            }
        };
    }

    private static ListFloat floatBufferToListFloat(FloatBuffer buffer) {
        return new ListFloat() {
            @Override
            public float getFloat(int index) {
                return buffer.get(index);
            }

            @Override
            public int size() {
                return buffer.remaining();
            }
        };
    }

    private static ListInteger intBufferToListInteger(IntBuffer buffer) {
        return new ListInteger() {
            @Override
            public int getInt(int index) {
                return buffer.get(index);
            }

            @Override
            public int size() {
                return buffer.remaining();
            }
        };
    }

    private static double[] longArrayToDoubleArray(long[] array) {
        return Arrays.stream(array).mapToDouble(
                element -> (double) element).toArray();
    }

    private static int[] longArrayToIntArray(long[] array) {
        return Arrays.stream(array).mapToInt(
                element -> (int) element).toArray();
    }

    private static ListShort shortBufferToListShort(ShortBuffer buffer) {
        return new ListShort() {
            @Override
            public short getShort(int index) {
                return buffer.get(index);
            }

            @Override
            public int size() {
                return buffer.remaining();
            }
        };
    }

}
