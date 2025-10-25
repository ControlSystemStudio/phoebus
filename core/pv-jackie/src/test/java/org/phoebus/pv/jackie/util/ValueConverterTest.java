/*******************************************************************************
 * Copyright (c) 2024-2025 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.pv.jackie.util;

import com.aquenos.epics.jackie.common.value.ChannelAccessAlarmSeverity;
import com.aquenos.epics.jackie.common.value.ChannelAccessAlarmStatus;
import com.aquenos.epics.jackie.common.value.ChannelAccessFloatingPointControlsValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessGraphicsEnum;
import com.aquenos.epics.jackie.common.value.ChannelAccessNumericControlsValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessSimpleOnlyChar;
import com.aquenos.epics.jackie.common.value.ChannelAccessSimpleOnlyDouble;
import com.aquenos.epics.jackie.common.value.ChannelAccessSimpleOnlyFloat;
import com.aquenos.epics.jackie.common.value.ChannelAccessSimpleOnlyLong;
import com.aquenos.epics.jackie.common.value.ChannelAccessSimpleOnlyShort;
import com.aquenos.epics.jackie.common.value.ChannelAccessSimpleOnlyString;
import com.aquenos.epics.jackie.common.value.ChannelAccessSimpleOnlyValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessTimeValue;
import com.aquenos.epics.jackie.common.value.ChannelAccessValueFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.epics.vtype.AlarmProvider;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.DisplayProvider;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.TimeProvider;
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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.phoebus.pv.jackie.JackiePreferences;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link ValueConverter}.
 */
public class ValueConverterTest {

    private final static Charset UTF_8 = StandardCharsets.UTF_8;

    /**
     * Test conversion of a <code>byte[]</code> to a CA value.
     */
    @Test
    public void byteArrayToChannelAccessValue() {
        final byte[] value = new byte[] {2, 4};
        final var ca_value = (ChannelAccessSimpleOnlyChar) objectToChannelAccessSimpleOnlyValue(value);
        assertEquals(ByteBuffer.wrap(value), ca_value.getValue());
    }

    /**
     * Test conversion of a {@link Byte} to a CA value.
     */
    @Test
    public void byteToChannelAccessValue() {
        final byte value = 3;
        final var ca_value = (ChannelAccessSimpleOnlyChar) objectToChannelAccessSimpleOnlyValue(value);
        assertEquals(value, ca_value.getValue().get(0));
    }

    /**
     * Test conversion of a char CA value representing a long string to a VType.
     */
    @Test
    public void caCharAsStringToVType() {
        final var value = "This is a string.";
        final var byte_buffer = UTF_8.encode(value);
        final var bytes_value = new byte[byte_buffer.remaining()];
        byte_buffer.get(bytes_value);
        final var time_value = ChannelAccessValueFactory.createTimeChar(
                bytes_value,
                ChannelAccessAlarmSeverity.NO_ALARM,
                ChannelAccessAlarmStatus.NO_ALARM,
                789,
                132);
        final var vtype = (VString) ValueConverter.channelAccessToVType(
                null, time_value, UTF_8, false, false, true);
        assertEquals(value, vtype.getValue());
        checkAlarm(time_value, vtype);
        checkTime(time_value, vtype);
    }

    /**
     * Test conversion of a char CA value to a VString.
     */
    @Test
    public void caCharToVType() {
        final var controls_value = ChannelAccessValueFactory.createControlsChar(
                ArrayUtils.EMPTY_BYTE_ARRAY,
                ChannelAccessAlarmSeverity.MAJOR_ALARM,
                ChannelAccessAlarmStatus.LOLO,
                (byte) -15,
                (byte) 5,
                (byte) -5,
                (byte) 40,
                (byte) -50,
                (byte) 50,
                "some unit",
                UTF_8,
                (byte) -10,
                (byte) 10);
        var value = new byte[] {1, 2};
        final var time_value = ChannelAccessValueFactory.createTimeChar(
                value,
                ChannelAccessAlarmSeverity.NO_ALARM,
                ChannelAccessAlarmStatus.NO_ALARM,
                789,
                132);
        var vtype_array = (VByteArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertArrayEquals(
                value,
                vtype_array.getData().toArray(ArrayUtils.EMPTY_BYTE_ARRAY));
        checkAlarm(time_value, vtype_array);
        checkDisplay(controls_value, vtype_array);
        checkTime(time_value, vtype_array);
        // Test a single-element value.
        value = new byte[] {3};
        time_value.setValue(value);
        vtype_array = (VByteArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, true, false, false);
        assertArrayEquals(
                value,
                vtype_array.getData().toArray(ArrayUtils.EMPTY_BYTE_ARRAY));
        var vtype_single = (VByte) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertEquals((byte) 3, vtype_single.getValue().byteValue());
    }

    /**
     * Test conversion of a double CA value to a VType.
     */
    @Test
    public void caDoubleToVType() {
        final var controls_value = ChannelAccessValueFactory.createControlsDouble(
                ArrayUtils.EMPTY_DOUBLE_ARRAY,
                ChannelAccessAlarmSeverity.MINOR_ALARM,
                ChannelAccessAlarmStatus.HIGH,
                -15.0,
                500.0,
                -5.0,
                400.0,
                -1000.0,
                1000.0,
                "V",
                UTF_8,
                (short) 3,
                -10.0,
                10.0);
        var value = new double[] {1.0, 2.0};
        final var time_value = ChannelAccessValueFactory.createTimeDouble(
                value,
                ChannelAccessAlarmSeverity.MINOR_ALARM,
                ChannelAccessAlarmStatus.LOW,
                123,
                456);
        var vtype_array = (VDoubleArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertArrayEquals(
                value,
                vtype_array.getData().toArray(ArrayUtils.EMPTY_DOUBLE_ARRAY));
        checkAlarm(time_value, vtype_array);
        checkDisplay(controls_value, vtype_array);
        checkTime(time_value, vtype_array);
        // Test a single-element value.
        value = new double[] {3.1};
        time_value.setValue(value);
        vtype_array = (VDoubleArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, true, false, false);
        assertArrayEquals(
                value,
                vtype_array.getData().toArray(ArrayUtils.EMPTY_DOUBLE_ARRAY));
        var vtype_single = (VDouble) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertEquals(3.1, vtype_single.getValue().doubleValue());
    }

    /**
     * Test conversion of a double CA value to a VType.
     */
    @Test
    public void caEnumToVType() {
        var labels = List.of("a", "b", "c", "d");
        final var controls_value = ChannelAccessValueFactory.createControlsEnum(
                ArrayUtils.EMPTY_SHORT_ARRAY,
                ChannelAccessAlarmSeverity.NO_ALARM,
                ChannelAccessAlarmStatus.NO_ALARM,
                labels,
                UTF_8);
        var value = new short[] {1, 2};
        final var time_value = ChannelAccessValueFactory.createTimeEnum(
                value,
                ChannelAccessAlarmSeverity.MINOR_ALARM,
                ChannelAccessAlarmStatus.STATE,
                1234,
                4567);
        var vtype_array = (VEnumArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertArrayEquals(
                value,
                vtype_array.getIndexes().toArray(
                        ArrayUtils.EMPTY_SHORT_ARRAY));
        assertEquals(List.of("b", "c"), vtype_array.getData());
        checkAlarm(time_value, vtype_array);
        checkEnumDisplay(controls_value, vtype_array.getDisplay());
        checkTime(time_value, vtype_array);
        // Test a single-element value.
        value = new short[] {3};
        time_value.setValue(value);
        vtype_array = (VEnumArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, true, false, false);
        assertArrayEquals(
                value,
                vtype_array.getIndexes().toArray(
                        ArrayUtils.EMPTY_SHORT_ARRAY));
        assertEquals(List.of("d"), vtype_array.getData());
        var vtype_single = (VEnum) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertEquals(3, vtype_single.getIndex());
        assertEquals("d", vtype_single.getValue());
        // Test an array with a value for which there is no label, but which is
        // reasonably small (less than 16).
        value = new short[] {1, 14};
        time_value.setValue(value);
        vtype_array = (VEnumArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertArrayEquals(
                value,
                vtype_array.getIndexes().toArray(
                        ArrayUtils.EMPTY_SHORT_ARRAY));
        assertEquals(List.of("b", "Index 14"), vtype_array.getData());
        assertEquals(
                List.of(
                        "a",
                        "b",
                        "c",
                        "d",
                        "Index 4",
                        "Index 5",
                        "Index 6",
                        "Index 7",
                        "Index 8",
                        "Index 9",
                        "Index 10",
                        "Index 11",
                        "Index 12",
                        "Index 13",
                        "Index 14"),
                vtype_array.getDisplay().getChoices());
        // Repeat the test with a single element.
        value = new short[] {15};
        time_value.setValue(value);
        vtype_single = (VEnum) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertEquals(15, vtype_single.getIndex());
        assertEquals("Index 15", vtype_single.getValue());
        assertEquals(
                List.of(
                        "a",
                        "b",
                        "c",
                        "d",
                        "Index 4",
                        "Index 5",
                        "Index 6",
                        "Index 7",
                        "Index 8",
                        "Index 9",
                        "Index 10",
                        "Index 11",
                        "Index 12",
                        "Index 13",
                        "Index 14",
                        "Index 15"),
                vtype_single.getDisplay().getChoices());
        // Test an array with a value for which there is no label and which has
        // a value greater than 15. In this case, we expect a VShortArray
        // instead of a VEnumArray.
        value = new short[] {0, 42};
        time_value.setValue(value);
        var vshort_array = (VShortArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, true, false, false);
        assertArrayEquals(
                value,
                vshort_array.getData().toArray(ArrayUtils.EMPTY_SHORT_ARRAY));
        // Repeat the test with a single element.
        value = new short[] {16};
        time_value.setValue(value);
        var vshort_single = (VShort) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertEquals((short) 16, vshort_single.getValue().shortValue());
        // Finally, we repeat the test with a negative number.
        value = new short[] {-5, 2};
        time_value.setValue(value);
        vshort_array = (VShortArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, true, false, false);
        assertArrayEquals(
                value,
                vshort_array.getData().toArray(ArrayUtils.EMPTY_SHORT_ARRAY));
        value = new short[] {-1};
        time_value.setValue(value);
        vshort_single = (VShort) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertEquals((short) -1, vshort_single.getValue().shortValue());
    }

    /**
     * Test conversion of a float CA value to a VType.
     */
    @Test
    public void caFloatToVType() {
        final var controls_value = ChannelAccessValueFactory.createControlsFloat(
                ArrayUtils.EMPTY_FLOAT_ARRAY,
                ChannelAccessAlarmSeverity.INVALID_ALARM,
                ChannelAccessAlarmStatus.BAD_SUB,
                -15.0f,
                500.0f,
                -5.0f,
                400.0f,
                -1000.0f,
                1000.0f,
                "A",
                UTF_8,
                (short) 2,
                -10.0f,
                10.0f);
        var value = new float[] {1.0f, 2.0f};
        final var time_value = ChannelAccessValueFactory.createTimeFloat(
                value,
                ChannelAccessAlarmSeverity.MAJOR_ALARM,
                ChannelAccessAlarmStatus.HIHI,
                123,
                456);
        var vtype_array = (VFloatArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertArrayEquals(
                value,
                vtype_array.getData().toArray(ArrayUtils.EMPTY_FLOAT_ARRAY));
        checkAlarm(time_value, vtype_array);
        checkDisplay(controls_value, vtype_array);
        checkTime(time_value, vtype_array);
        // Test a single-element value.
        value = new float[] {3.1f};
        time_value.setValue(value);
        vtype_array = (VFloatArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, true, false, false);
        assertArrayEquals(
                value,
                vtype_array.getData().toArray(ArrayUtils.EMPTY_FLOAT_ARRAY));
        var vtype_single = (VFloat) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertEquals(3.1f, vtype_single.getValue().floatValue());
    }

    /**
     * Test conversion of a long CA value to a VType.
     */
    @Test
    public void caLongToVType() {
        final var controls_value = ChannelAccessValueFactory.createControlsLong(
                ArrayUtils.EMPTY_INT_ARRAY,
                ChannelAccessAlarmSeverity.MINOR_ALARM,
                ChannelAccessAlarmStatus.HIGH,
                -15,
                500,
                -5,
                400,
                -1000,
                1000,
                "V",
                UTF_8,
                -10,
                10);
        var value = new int[] {1, 2};
        final var time_value = ChannelAccessValueFactory.createTimeLong(
                value,
                ChannelAccessAlarmSeverity.INVALID_ALARM,
                ChannelAccessAlarmStatus.CALC,
                123,
                456);
        var vtype_array = (VIntArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertArrayEquals(
                value,
                vtype_array.getData().toArray(ArrayUtils.EMPTY_INT_ARRAY));
        checkAlarm(time_value, vtype_array);
        checkDisplay(controls_value, vtype_array);
        checkTime(time_value, vtype_array);
        // Test a single-element value.
        value = new int[] {3};
        time_value.setValue(value);
        vtype_array = (VIntArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, true, false, false);
        assertArrayEquals(
                value,
                vtype_array.getData().toArray(ArrayUtils.EMPTY_INT_ARRAY));
        var vtype_single = (VInt) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertEquals(3, vtype_single.getValue().intValue());
    }

    /**
     * Test conversion of a short CA value to a VType.
     */
    @Test
    public void caShortToVType() {
        final var controls_value = ChannelAccessValueFactory.createControlsShort(
                ArrayUtils.EMPTY_SHORT_ARRAY,
                ChannelAccessAlarmSeverity.MINOR_ALARM,
                ChannelAccessAlarmStatus.HIGH,
                (short) -15,
                (short) 500,
                (short) -5,
                (short) 400,
                (short) -1000,
                (short) 1000,
                "V",
                UTF_8,
                (short) -10,
                (short) 10);
        var value = new short[] {1, 2};
        final var time_value = ChannelAccessValueFactory.createTimeShort(
                value,
                ChannelAccessAlarmSeverity.NO_ALARM,
                ChannelAccessAlarmStatus.NO_ALARM,
                123,
                456);
        var vtype_array = (VShortArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertArrayEquals(
                value,
                vtype_array.getData().toArray(ArrayUtils.EMPTY_SHORT_ARRAY));
        checkAlarm(time_value, vtype_array);
        checkDisplay(controls_value, vtype_array);
        checkTime(time_value, vtype_array);
        // Test a single-element value.
        value = new short[] {3};
        time_value.setValue(value);
        vtype_array = (VShortArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, true, false, false);
        assertArrayEquals(
                value,
                vtype_array.getData().toArray(ArrayUtils.EMPTY_SHORT_ARRAY));
        var vtype_single = (VShort) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        assertEquals((short) 3, vtype_single.getValue().shortValue());
    }

    /**
     * Test conversion of a string CA value to a VType.
     */
    @Test
    public void caStringToVType() {
        var value = List.of("abc", "def");
        final var time_value = ChannelAccessValueFactory.createTimeString(
                value,
                UTF_8,
                ChannelAccessAlarmSeverity.MAJOR_ALARM,
                ChannelAccessAlarmStatus.STATE,
                123,
                456);
        var vtype_array = (VStringArray) ValueConverter.channelAccessToVType(
                null, time_value, UTF_8, false, false, false);
        assertEquals(value, vtype_array.getData());
        checkAlarm(time_value, vtype_array);
        checkTime(time_value, vtype_array);
        // Test a single-element value.
        value = Collections.singletonList("some string");
        time_value.setValue(value);
        vtype_array = (VStringArray) ValueConverter.channelAccessToVType(
                null, time_value, UTF_8, true, false, false);
        assertEquals(value, vtype_array.getData());
        var vtype_single = (VString) ValueConverter.channelAccessToVType(
                null, time_value, UTF_8, false, false, false);
        assertEquals("some string", vtype_single.getValue());
    }

    /**
     * Test the <code>honor_zero_precision</code> flag when converting from a
     * CA value to a VType.
     */
    @Test
    public void caToVTypeHonorZeroPrecision() {
        final var controls_value = ChannelAccessValueFactory.createControlsDouble(
                ArrayUtils.EMPTY_DOUBLE_ARRAY,
                ChannelAccessAlarmSeverity.MINOR_ALARM,
                ChannelAccessAlarmStatus.HIGH,
                -15.0,
                500.0,
                -5.0,
                400.0,
                -1000.0,
                1000.0,
                "V",
                UTF_8,
                (short) 0,
                -10.0,
                10.0);
        var value = new double[] {1.0, 2.0};
        final var time_value = ChannelAccessValueFactory.createTimeDouble(
                value,
                ChannelAccessAlarmSeverity.NO_ALARM,
                ChannelAccessAlarmStatus.NO_ALARM,
                123,
                456);
        // If honor_zero_precision is set to false, the display format should
        // include fractional digits, even if the precision is zero. We do not
        // check the minimum fraction digits here because due to using a
        // default format, those might well be zero.
        var vtype = (VDoubleArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, false, false);
        var format = vtype.getDisplay().getFormat();
        assertNotEquals(0, format.getMaximumFractionDigits());
        // If honor_zero_precision is true, the display format should not
        // include any fractional digits if the precision is zero.
        vtype = (VDoubleArray) ValueConverter.channelAccessToVType(
                controls_value, time_value, UTF_8, false, true, false);
        format = vtype.getDisplay().getFormat();
        assertEquals(0, format.getMaximumFractionDigits());
        assertEquals(0, format.getMinimumFractionDigits());
    }

    /**
     * Test conversion of a <code>double[]</code> to a CA value.
     */
    @Test
    public void doubleArrayToChannelAccessValue() {
        final double[] value = new double[] {2.0, 4.0};
        final var ca_value = (ChannelAccessSimpleOnlyDouble) objectToChannelAccessSimpleOnlyValue(value);
        assertEquals(DoubleBuffer.wrap(value), ca_value.getValue());
    }

    /**
     * Test conversion of a {@link Double} to a CA value.
     */
    @Test
    public void doubleToChannelAccessValue() {
        final double value = 3.0;
        final var ca_value = (ChannelAccessSimpleOnlyDouble) objectToChannelAccessSimpleOnlyValue(value);
        assertEquals(value, ca_value.getValue().get(0));
    }

    /**
     * Test conversion of a <code>float[]</code> to a CA value.
     */
    @Test
    public void floatArrayToChannelAccessValue() {
        final float[] value = new float[] {2.0f, 4.0f};
        final var ca_value = (ChannelAccessSimpleOnlyFloat) objectToChannelAccessSimpleOnlyValue(value);
        assertEquals(FloatBuffer.wrap(value), ca_value.getValue());
    }

    /**
     * Test conversion of a {@link Float} to a CA value.
     */
    @Test
    public void floatToChannelAccessValue() {
        final float value = 3.0f;
        final var ca_value = (ChannelAccessSimpleOnlyFloat) objectToChannelAccessSimpleOnlyValue(value);
        assertEquals(value, ca_value.getValue().get(0));
    }

    /**
     * Test conversion of an <code>int[]</code> to a CA value.
     */
    @Test
    public void intArrayToChannelAccessValue() {
        final int[] value = new int[] {2, 4};
        final var ca_value = (ChannelAccessSimpleOnlyLong) objectToChannelAccessSimpleOnlyValue(value);
        assertEquals(IntBuffer.wrap(value), ca_value.getValue());
    }

    /**
     * Test conversion of an {@link Integer} to a CA value.
     */
    @Test
    public void intToChannelAccessValue() {
        final int value = 3;
        final var ca_value = (ChannelAccessSimpleOnlyLong) objectToChannelAccessSimpleOnlyValue(value);
        assertEquals(value, ca_value.getValue().get(0));
    }

    /**
     * Test conversion of a <code>long[]</code> to a CA value.
     */
    @Test
    public void longArrayToChannelAccessValue() {
        // Test an array that has a value that is too small.
        var original = new long[] {15L, 23L, Integer.MIN_VALUE - 1L};
        var coerced = new int[]{15, 23, Integer.MIN_VALUE};
        var converted = new double[]{
                15.0, 23.0, (double) (Integer.MIN_VALUE - 1L)};
        var truncated = new int[]{15, 23, Integer.MAX_VALUE};
        testLong(original, coerced, converted, truncated, false);
        // Test an array that has a value that is too large.
        original = new long[] {15L, 23L, Integer.MAX_VALUE + 1L};
        coerced = new int[]{15, 23, Integer.MAX_VALUE};
        converted = new double[]{15.0, 23.0, (double) (Integer.MAX_VALUE + 1L)};
        truncated = new int[]{15, 23, Integer.MIN_VALUE};
        testLong(original, coerced, converted, truncated, false);
        // Test an array where all values are within the allowed range.
        original = new long[] {15L, 23L, Integer.MIN_VALUE, Integer.MAX_VALUE};
        coerced = new int[]{15, 23, Integer.MIN_VALUE, Integer.MAX_VALUE};
        // converted is not used when all elements are within the limits.
        converted = null;
        truncated = coerced;
        testLong(original, coerced, converted, truncated, true);
    }

    /**
     * Test conversion of a {@link Long} to a CA value.
     */
    @Test
    public void longToChannelAccessValue() {
        // Test a value that is too small.
        var original = Integer.MIN_VALUE - 1L;
        var coerced = Integer.MIN_VALUE;
        var converted = (double) (Integer.MIN_VALUE - 1L);
        var truncated = Integer.MAX_VALUE;
        testLong(original, coerced, converted, truncated, false);
        // Test a value that is too large.
        original = Integer.MAX_VALUE + 1L;
        coerced = Integer.MAX_VALUE;
        converted = (double) (Integer.MAX_VALUE + 1L);
        truncated = Integer.MIN_VALUE;
        testLong(original, coerced, converted, truncated, false);
        // Test values in the allowed range. The converted value does not
        // matter in this context, because it is never used.
        original = 15L;
        coerced = (int) original;
        converted = Double.NaN;
        truncated = (int) original;
        testLong(original, coerced, converted, truncated, true);
        original = 23L;
        coerced = (int) original;
        truncated = (int) original;
        testLong(original, coerced, converted, truncated, true);
        original = Integer.MIN_VALUE;
        coerced = (int) original;
        truncated = (int) original;
        testLong(original, coerced, converted, truncated, true);
        original = Integer.MAX_VALUE;
        coerced = (int) original;
        truncated = (int) original;
        testLong(original, coerced, converted, truncated, true);
    }

    /**
     * Test conversion of a <code>short[]</code> to a CA value.
     */
    @Test
    public void shortArrayToChannelAccessValue() {
        final short[] value = new short[] {2, 4};
        final var ca_value = (ChannelAccessSimpleOnlyShort) objectToChannelAccessSimpleOnlyValue(value);
        assertEquals(ShortBuffer.wrap(value), ca_value.getValue());
    }

    /**
     * Test conversion of a {@link Short} to a CA value.
     */
    @Test
    public void shortToChannelAccessValue() {
        final short value = 3;
        final var ca_value = (ChannelAccessSimpleOnlyShort) objectToChannelAccessSimpleOnlyValue(value);
        assertEquals(value, ca_value.getValue().get(0));
    }

    /**
     * Test conversion of a <code>String[]</code> to a CA value.
     */
    @Test
    public void stringArrayToChannelAccessValue() {
        var value = new String[] {"abc", "123"};
        var ca_value = (ChannelAccessSimpleOnlyString) ValueConverter
                .objectToChannelAccessSimpleOnlyValue(
                        "",
                        value,
                        UTF_8,
                        false,
                        JackiePreferences.LongConversionMode.FAIL);
        assertEquals(Arrays.asList(value), ca_value.getValue());
        // For an array with multiple elements, it should not make a difference
        // if we enable the convert_string_as_long_string option.
        ca_value = (ChannelAccessSimpleOnlyString) ValueConverter
                .objectToChannelAccessSimpleOnlyValue(
                        "",
                        value,
                        UTF_8,
                        true,
                        JackiePreferences.LongConversionMode.FAIL);
        assertEquals(Arrays.asList(value), ca_value.getValue());
        // For a single-element array, we expect a different result.
        value = new String[] {"a single string"};
        final var ca_char_array = (ChannelAccessSimpleOnlyChar) ValueConverter
                .objectToChannelAccessSimpleOnlyValue(
                        "",
                        value,
                        UTF_8,
                        true,
                        JackiePreferences.LongConversionMode.FAIL);
        assertEquals(
                value[0], UTF_8.decode(ca_char_array.getValue()).toString());
    }

    /**
     * Test conversion of a {@link String} to a CA value.
     */
    @Test
    public void stringToChannelAccessValue() {
        final String value = "some string";
        final var ca_value = (ChannelAccessSimpleOnlyString) ValueConverter
                .objectToChannelAccessSimpleOnlyValue(
                        "",
                        value,
                        UTF_8,
                        false,
                        JackiePreferences.LongConversionMode.FAIL);
        assertEquals(value, ca_value.getValue().get(0));
        // When setting convert_string_as_long_string to true, we expect a
        // ChannelAccessSimpleOnlyChar instead.
        final var ca_char_array = (ChannelAccessSimpleOnlyChar) ValueConverter
                .objectToChannelAccessSimpleOnlyValue(
                        "",
                        value,
                        UTF_8,
                        true,
                        JackiePreferences.LongConversionMode.FAIL);
        assertEquals(value, UTF_8.decode(ca_char_array.getValue()).toString());
    }

    private static void checkAlarm(
            ChannelAccessTimeValue<?> time_value,
            AlarmProvider alarmProvider_provider) {
        final var alarm = alarmProvider_provider.getAlarm();
        switch (time_value.getAlarmSeverity()) {
            case NO_ALARM -> {
                assertEquals(AlarmSeverity.NONE, alarm.getSeverity());
            }
            case MINOR_ALARM -> {
                assertEquals(AlarmSeverity.MINOR, alarm.getSeverity());
            }
            case MAJOR_ALARM -> {
                assertEquals(AlarmSeverity.MAJOR, alarm.getSeverity());
            }
            case INVALID_ALARM -> {
                assertEquals(AlarmSeverity.INVALID, alarm.getSeverity());
            }
        }
    }

    private static void checkDisplay(
            ChannelAccessNumericControlsValue<?> controls_value,
            DisplayProvider display_provider) {
        final var display = display_provider.getDisplay();
        assertEquals(
                controls_value.getGenericLowerAlarmLimit().doubleValue(),
                display.getAlarmRange().getMinimum());
        assertEquals(
                controls_value.getGenericUpperAlarmLimit().doubleValue(),
                display.getAlarmRange().getMaximum());
        assertEquals(
                controls_value.getGenericLowerDisplayLimit().doubleValue(),
                display.getDisplayRange().getMinimum());
        assertEquals(
                controls_value.getGenericUpperDisplayLimit().doubleValue(),
                display.getDisplayRange().getMaximum());
        assertEquals(
                controls_value.getGenericLowerControlLimit().doubleValue(),
                display.getControlRange().getMinimum());
        assertEquals(
                controls_value.getGenericUpperControlLimit().doubleValue(),
                display.getControlRange().getMaximum());
        assertEquals(
                controls_value.getGenericLowerWarningLimit().doubleValue(),
                display.getWarningRange().getMinimum());
        assertEquals(
                controls_value.getGenericUpperWarningLimit().doubleValue(),
                display.getWarningRange().getMaximum());
        assertEquals(controls_value.getUnits(), display.getUnit());
        if (controls_value instanceof ChannelAccessFloatingPointControlsValue<?> fp_value) {
            final var precision = fp_value.getPrecision();
            if (precision != 0) {
                final var format = display.getFormat();
                assertEquals(precision, format.getMinimumFractionDigits());
                assertEquals(precision, format.getMaximumFractionDigits());
            }
        }
    }

    private static void checkEnumDisplay(
            ChannelAccessGraphicsEnum controls_value,
            EnumDisplay display) {
        assertEquals(controls_value.getLabels(),display.getChoices());
    }

    private static void checkWarnCalled(
            Logger logger_mock, VerificationMode verification_mode) {
        Mockito.verify(logger_mock, verification_mode).warn(
                Mockito.anyString(), (Object[]) Mockito.any());
    }

    private static void checkTime(
            ChannelAccessTimeValue<?> time_value,
            TimeProvider time_provider) {
        final var instant = time_provider.getTime().getTimestamp();
        assertEquals(
                time_value.getTimeSeconds()
                        + ValueConverter.OFFSET_EPICS_TO_UNIX_EPOCH_SECONDS,
                instant.getEpochSecond());
        assertEquals(time_value.getTimeNanoseconds(), instant.getNano());
    }

    private static ChannelAccessSimpleOnlyValue<?> objectToChannelAccessSimpleOnlyValue(
            Object value) {
        return ValueConverter.objectToChannelAccessSimpleOnlyValue(
                "test_pv",
                value,
                UTF_8,
                false,
                JackiePreferences.LongConversionMode.FAIL);
    }

    private static ChannelAccessSimpleOnlyValue<?> objectToChannelAccessSimpleOnlyValue(
            Object value,
            JackiePreferences.LongConversionMode long_conversion_mode,
            Logger logger) {
        final var original_logger = ValueConverter.log;
        try {
            if (logger != null) {
                ValueConverter.log = logger;
            }
            return ValueConverter.objectToChannelAccessSimpleOnlyValue(
                    "test_pv",
                    value,
                    UTF_8,
                    false,
                    long_conversion_mode);
        } finally {
            if (logger != null) {
                ValueConverter.log = original_logger;
            }
        }
    }

    private static void testLong(
            long original,
            int coerced,
            double converted,
            int truncated,
            boolean in_limits) {
        testLong(
                original,
                new int[]{coerced},
                new double[]{converted},
                new int[]{truncated},
                in_limits);
    }

    private static void testLong(
            Object original,
            int[] coerced,
            double[] converted,
            int[] truncated,
            boolean in_limits) {
        // Test the COERCE mode.
        var logger_mock = Mockito.mock(Logger.class);
        var ca_value = objectToChannelAccessSimpleOnlyValue(
                original,
                JackiePreferences.LongConversionMode.COERCE,
                logger_mock);
        assertEquals(
                IntBuffer.wrap(coerced),
                ((ChannelAccessSimpleOnlyLong) ca_value).getValue());
        checkWarnCalled(logger_mock, Mockito.never());
        // Test the COERCE_AND_WARN mode.
        logger_mock = Mockito.mock(Logger.class);
        ca_value = objectToChannelAccessSimpleOnlyValue(
                original,
                JackiePreferences.LongConversionMode.COERCE_AND_WARN,
                logger_mock);
        assertEquals(
                IntBuffer.wrap(coerced),
                ((ChannelAccessSimpleOnlyLong) ca_value).getValue());
        if (in_limits) {
            checkWarnCalled(logger_mock, Mockito.never());
        } else {
            checkWarnCalled(logger_mock, Mockito.times(1));
        }
        // Test the CONVERT mode.
        logger_mock = Mockito.mock(Logger.class);
        ca_value = objectToChannelAccessSimpleOnlyValue(
                original,
                JackiePreferences.LongConversionMode.CONVERT, logger_mock);
        if (in_limits) {
            assertEquals(
                    IntBuffer.wrap(truncated),
                    ((ChannelAccessSimpleOnlyLong) ca_value).getValue());
            checkWarnCalled(logger_mock, Mockito.never());
        } else {
            assertEquals(
                    DoubleBuffer.wrap(converted),
                    ((ChannelAccessSimpleOnlyDouble) ca_value).getValue());
            checkWarnCalled(logger_mock, Mockito.never());
        }
        // Test the CONVERT_AND_WARN mode.
        logger_mock = Mockito.mock(Logger.class);
        ca_value = objectToChannelAccessSimpleOnlyValue(
                original,
                JackiePreferences.LongConversionMode.CONVERT_AND_WARN,
                logger_mock);
        if (in_limits) {
            assertEquals(
                    IntBuffer.wrap(truncated),
                    ((ChannelAccessSimpleOnlyLong) ca_value).getValue());
            checkWarnCalled(logger_mock, Mockito.never());
        } else {
            assertEquals(
                    DoubleBuffer.wrap(converted),
                    ((ChannelAccessSimpleOnlyDouble) ca_value).getValue());
            checkWarnCalled(logger_mock, Mockito.times(1));
        }
        // Test the FAIL mode.
        logger_mock = Mockito.mock(Logger.class);
        if (in_limits) {
            ca_value = objectToChannelAccessSimpleOnlyValue(
                    original,
                    JackiePreferences.LongConversionMode.FAIL,
                    logger_mock);
            assertEquals(
                    IntBuffer.wrap(truncated),
                    ((ChannelAccessSimpleOnlyLong) ca_value).getValue());
            checkWarnCalled(logger_mock, Mockito.never());
        } else {
            final var final_logger_mock = logger_mock;
            assertThrows(
                    IllegalArgumentException.class,
                    () -> objectToChannelAccessSimpleOnlyValue(
                            original,
                            JackiePreferences.LongConversionMode.FAIL,
                            final_logger_mock));
            checkWarnCalled(logger_mock, Mockito.never());
        }
        // Test the TRUNCATE mode.
        logger_mock = Mockito.mock(Logger.class);
        ca_value = objectToChannelAccessSimpleOnlyValue(
                original,
                JackiePreferences.LongConversionMode.TRUNCATE,
                logger_mock);
        assertEquals(
                IntBuffer.wrap(truncated),
                ((ChannelAccessSimpleOnlyLong) ca_value).getValue());
        checkWarnCalled(logger_mock, Mockito.never());
        // Test the TRUNCATE_AND_WARN mode.
        logger_mock = Mockito.mock(Logger.class);
        ca_value = objectToChannelAccessSimpleOnlyValue(
                original,
                JackiePreferences.LongConversionMode.TRUNCATE_AND_WARN,
                logger_mock);
        assertEquals(
                IntBuffer.wrap(truncated),
                ((ChannelAccessSimpleOnlyLong) ca_value).getValue());
        if (in_limits) {
            checkWarnCalled(logger_mock, Mockito.never());
        } else {
            checkWarnCalled(logger_mock, Mockito.times(1));
        }
    }

}
