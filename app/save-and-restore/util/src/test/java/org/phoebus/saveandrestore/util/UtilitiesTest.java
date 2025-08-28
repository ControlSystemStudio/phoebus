/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.saveandrestore.util;

import org.epics.pva.data.*;
import org.epics.util.array.*;
import org.epics.vtype.*;
import org.junit.jupiter.api.Test;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.saveandrestore.util.Threshold;
import org.phoebus.saveandrestore.util.Utilities;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UtilitiesTest {

    /**
     * Tests {@link Utilities#valueToString(VType)} and {@link Utilities#valueToString(VType, int)}.
     */
    @Test
    public void testValueToString() {
        Alarm alarm = Alarm.none();
        Display display = Display.none();
        Time time = Time.now();

        assertNull(Utilities.valueToString(null));

        VType val = VDouble.of(5d, alarm, time, display);
        String result = Utilities.valueToString(val);
        assertEquals("5.0", result);

        val = VFloat.of(5f, alarm, time, display);
        result = Utilities.valueToString(val);
        assertEquals("5.0", result);

        val = VLong.of(5L, alarm, time, display);
        result = Utilities.valueToString(val);
        assertEquals("5", result);

        val = VInt.of(5, alarm, time, display);
        result = Utilities.valueToString(val);
        assertEquals("5", result);

        val = VShort.of((short) 5, alarm, time, display);
        result = Utilities.valueToString(val);
        assertEquals("5", result);

        val = VByte.of((byte) 5, alarm, time, display);
        result = Utilities.valueToString(val);
        assertEquals("5", result);

        val = VEnum.of(1, EnumDisplay.of("first", "second", "third"), alarm, time);
        result = Utilities.valueToString(val);
        assertEquals("second", result);

        val = VEnum.of(1, EnumDisplay.of("", "", ""), alarm, time);
        result = Utilities.valueToString(val);
        assertEquals("1", result);

        val = VEnum.of(1, EnumDisplay.of("a", "", ""), alarm, time);
        result = Utilities.valueToString(val);
        assertEquals("", result);

        val = VString.of("third", alarm, time);
        result = Utilities.valueToString(val);
        assertEquals("third", result);

        val = VDoubleArray.of(ArrayDouble.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9), alarm, time, display);
        result = Utilities.valueToString(val);
        assertEquals("[1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0,...]", result);

        val = VFloatArray.of(ArrayFloat.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9), alarm, time, display);
        result = Utilities.valueToString(val);
        assertEquals("[1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0,...]", result);

        result = Utilities.valueToString(val, 3);
        assertEquals("[1.0, 2.0, 3.0,...]", result);

        result = Utilities.valueToString(val, 100);
        assertEquals("[1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]", result);

        val = VLongArray.of(ArrayLong.of(1, 2, 3, 4, 5), alarm, time, display);
        result = Utilities.valueToString(val, 3);
        assertEquals("[1, 2, 3,...]", result);

        val = VULongArray.of(ArrayULong.of(1, 2, 3, 4, 5), alarm, time, display);
        result = Utilities.valueToString(val, 3);
        assertEquals("[1, 2, 3,...]", result);

        val = VIntArray.of(ArrayInteger.of(1, 2, 3, 4, 5), alarm, time, display);
        result = Utilities.valueToString(val, 3);
        assertEquals("[1, 2, 3,...]", result);

        val = VUIntArray.of(ArrayUInteger.of(1, 2, 3, 4, 5), alarm, time, display);
        result = Utilities.valueToString(val, 3);
        assertEquals("[1, 2, 3,...]", result);

        val = VShortArray.of(ArrayShort.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), alarm, time, display);
        result = Utilities.valueToString(val, 3);
        assertEquals("[1, 2, 3,...]", result);

        val = VUShortArray.of(ArrayUShort.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5), alarm, time, display);
        result = Utilities.valueToString(val, 3);
        assertEquals("[1, 2, 3,...]", result);

        val = VByteArray.of(ArrayByte.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), alarm, time, display);
        result = Utilities.valueToString(val, 3);
        assertEquals("[1, 2, 3,...]", result);

        val = VUByteArray.of(ArrayUByte.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), alarm, time, display);
        result = Utilities.valueToString(val, 3);
        assertEquals("[1, 2, 3,...]", result);

        val = VIntArray.of(ArrayInteger.of(1, 2, 3, 4, 5), alarm, time, display);
        result = Utilities.valueToString(val, 0);
        assertEquals("[]", result);

        val = VBoolean.of(true, alarm, time);
        result = Utilities.valueToString(val);
        assertEquals("true", result);

        val = VStringArray.of(Arrays.asList("a", "b", "c"), alarm, time);
        result = Utilities.valueToString(val);
        assertEquals("[\"a\", \"b\", \"c\"]", result);
    }

    /**
     * Tests {@link Utilities#valueFromString(String, VType)}.
     */
    @Test
    public void testValueFromString() {

        Alarm alarm = Alarm.none();
        Display display = Display.none();
        Time time = Time.now();

        VType val = VDouble.of(5d, alarm, time, display);
        VType result = Utilities.valueFromString("5.0", val);
        assertTrue(result instanceof VDouble);
        assertEquals(5.0, ((VDouble) result).getValue(), 0);

        result = Utilities.valueFromString("", val);
        assertTrue(result instanceof VDouble);
        assertEquals(5.0, ((VDouble) result).getValue(), 0);

        val = VFloat.of(5f, alarm, time, display);
        result = Utilities.valueFromString("5.0", val);
        assertTrue(result instanceof VFloat);
        assertEquals(5.0f, ((VFloat) result).getValue(), 0);

        val = VLong.of(5L, alarm, time, display);
        result = Utilities.valueFromString("5", val);
        assertTrue(result instanceof VLong);
        assertEquals(5L, ((VLong) result).getValue().longValue());

        val = VULong.of(5L, alarm, time, display);
        result = Utilities.valueFromString("5", val);
        assertTrue(result instanceof VULong);
        assertEquals(5L, ((VULong) result).getValue().longValue());

        val = VUInt.of(5, alarm, time, display);
        result = Utilities.valueFromString("5", val);
        assertTrue(result instanceof VUInt);
        assertEquals(5, ((VUInt) result).getValue().intValue());

        val = VInt.of(5, alarm, time, display);
        result = Utilities.valueFromString("5", val);
        assertTrue(result instanceof VInt);
        assertEquals(5, ((VInt) result).getValue().intValue());

        val = VShort.of((short) 5, alarm, time, display);
        result = Utilities.valueFromString("5", val);
        assertTrue(result instanceof VShort);
        assertEquals((short) 5, ((VShort) result).getValue().shortValue());

        val = VUShort.of((short) 5, alarm, time, display);
        result = Utilities.valueFromString("5", val);
        assertTrue(result instanceof VUShort);
        assertEquals((short) 5, ((VUShort) result).getValue().shortValue());

        val = VByte.of((byte) 5, alarm, time, display);
        result = Utilities.valueFromString("5", val);
        assertTrue(result instanceof VByte);
        assertEquals((byte) 5, ((VByte) result).getValue().byteValue());

        val = VUByte.of((byte) 5, alarm, time, display);
        result = Utilities.valueFromString("5", val);
        assertTrue(result instanceof VUByte);
        assertEquals((byte) 5, ((VUByte) result).getValue().byteValue());

        val = VEnum.of(1, EnumDisplay.of("first", "second", "third"), alarm, time);
        result = Utilities.valueFromString("second", val);
        assertTrue(result instanceof VEnum);
        assertEquals("second", ((VEnum) result).getValue());

        val = VEnum.of(1, EnumDisplay.of("first", "second", "third"), alarm, time);
        try {
            Utilities.valueFromString("invalid", val);
            fail("Should throw exception");
        } catch (IllegalArgumentException e) {
            // Ignore
        }

        val = VBoolean.of(false, alarm, time);
        result = Utilities.valueFromString("false", val);
        assertTrue(result instanceof VBoolean);
        assertEquals(false, ((VBoolean) result).getValue());

        val = VString.of("third", alarm, time);
        result = Utilities.valueFromString("third", val);
        assertTrue(result instanceof VString);
        assertEquals("third", ((VString) result).getValue());


        try {
            val = VDoubleArray.of(ArrayDouble.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9), alarm, time, display);
            Utilities.valueFromString("[1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0,...]", val);
            fail("Exception should happen, because the number of elements is wrong");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        val = VDoubleArray.of(ArrayDouble.of(1, 2, 3, 4, 5, 6, 7, 8, 9), alarm, time, display);
        result = Utilities.valueFromString("[1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]", val);
        assertEquals(((VDoubleArray) result).getData(), ((VDoubleArray) val).getData());

        val = VFloatArray.of(ArrayFloat.of(1, 2, 3, 4, 5, 6, 7, 8, 9), alarm, time, display);
        result = Utilities.valueFromString("[1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]", val);
        assertEquals(((VFloatArray) result).getData(), ((VFloatArray) val).getData());

        val = VULongArray.of(ArrayULong.of(1, 2, 3, 4, 5, 6, 7, 8, 9), alarm, time, display);
        result = Utilities.valueFromString("[1, 2, 3, 4, 5, 6, 7, 8, 9]", val);
        assertEquals(((VULongArray) result).getData(), ((VULongArray) val).getData());

        val = VUIntArray.of(ArrayUInteger.of(1, 2, 3, 4, 5, 6, 7, 8, 9), alarm, time, display);
        result = Utilities.valueFromString("[1, 2, 3, 4, 5, 6, 7, 8, 9]", val);
        assertEquals(((VUIntArray) result).getData(), ((VUIntArray) val).getData());

        val = VIntArray.of(ArrayInteger.of(1, 2, 3, 4, 5, 6, 7, 8, 9), alarm, time, display);
        result = Utilities.valueFromString("[1, 2, 3, 4, 5, 6, 7, 8, 9]", val);
        assertEquals(((VIntArray) result).getData(), ((VIntArray) val).getData());

        val = VShortArray.of(ArrayShort.of((short) 1, (short) 2, (short) 3), alarm, time, display);
        result = Utilities.valueFromString("[1, 2, 3]", val);
        assertEquals(((VShortArray) result).getData(), ((VShortArray) val).getData());

        val = VUShortArray.of(ArrayUShort.of((short) 1, (short) 2, (short) 3), alarm, time, display);
        result = Utilities.valueFromString("[1, 2, 3]", val);
        assertEquals(((VUShortArray) result).getData(), ((VUShortArray) val).getData());

        val = VByteArray.of(ArrayByte.of((byte) 1, (byte) 2, (byte) 3), alarm, time, display);
        result = Utilities.valueFromString("[1, 2, 3]", val);
        assertEquals(((VByteArray) result).getData(), ((VByteArray) val).getData());

        val = VUByteArray.of(ArrayUByte.of((byte) 1, (byte) 2, (byte) 3), alarm, time, display);
        result = Utilities.valueFromString("[1, 2, 3]", val);
        assertEquals(((VUByteArray) result).getData(), ((VUByteArray) val).getData());

        val = VStringArray.of(Arrays.asList("first", "second", "third"), alarm, time);
        result = Utilities.valueFromString("[\"first\", \"second\", \"third\"]", val);
        assertTrue(result instanceof VStringArray);
        assertArrayEquals(new String[]{"first", "second", "third"}, ((VStringArray) result).getData().toArray(new String[0]));

        val = VLongArray.of(ArrayLong.of(1, 2, 3, 4, 5), alarm, time, display);
        result = Utilities.valueFromString("1, 2, 3, 4, 5", val);
        assertTrue(result instanceof VLongArray);
        assertNotNull(((VLongArray) result).getData());

        val = VBooleanArray.of(ArrayBoolean.of(true, true, false, true), alarm, time);
        result = Utilities.valueFromString("[1, 1, 0, 2]", val);
        assertTrue(result instanceof VBooleanArray);
        assertNotNull(((VBooleanArray) result).getData());

        val = VDisconnectedData.INSTANCE;
        result = Utilities.valueFromString("5", val);
        assertTrue(result instanceof VLong);
        assertEquals(5L, ((VLong) result).getValue().longValue());

        result = Utilities.valueFromString("5.1", val);
        assertTrue(result instanceof VDouble);
        assertEquals(5.1, ((VDouble) result).getValue(), 0);

        result = Utilities.valueFromString("string", val);
        assertTrue(result instanceof VString);
        assertEquals("string", ((VString) result).getValue());
    }


    /**
     * Tests {@link Utilities#valueToCompareString(VType, VType, Optional)}. The test doesn't cover all possible
     * combinations, but it does cover a handful of them.
     */
    @Test
    public void testValueToCompareString() {
        Alarm alarm = Alarm.none();
        Display display = Display.none();
        Time time = Time.now();

        Optional<Threshold<?>> threshold = Optional.of(new Threshold<>(5d, -5d));

        Utilities.VTypeComparison result = Utilities.valueToCompareString(null, null, threshold);
        assertEquals(VDisconnectedData.INSTANCE.toString(), result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());
        assertEquals("---", result.getString());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        VType val1 = VDouble.of(5d, alarm, time, display);
        result = Utilities.valueToCompareString(null, val1, threshold);
        assertEquals(VDisconnectedData.INSTANCE.toString(), result.getString());
        assertEquals(-1, result.getValuesEqual());
        assertFalse(result.isWithinThreshold());
        assertEquals("---", result.getString());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        result = Utilities.valueToCompareString(val1, null, threshold);
        assertEquals("5.0", result.getString());
        assertEquals(1, result.getValuesEqual());
        assertFalse(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        result = Utilities.valueToCompareString(VDisconnectedData.INSTANCE, val1, threshold);
        assertEquals(VDisconnectedData.INSTANCE.toString(), result.getString());
        assertEquals(-1, result.getValuesEqual());
        assertFalse(result.isWithinThreshold());
        assertEquals("---", result.getString());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        result = Utilities.valueToCompareString(val1, VDisconnectedData.INSTANCE, threshold);
        assertEquals("5.0", result.getString());
        assertEquals(1, result.getValuesEqual());
        assertFalse(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VDouble.of(5d, alarm, time, display);
        VType val2 = VDouble.of(6d, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("5 Δ-1.0", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VDouble.of(15d, alarm, time, display);
        val2 = VDouble.of(6d, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("15 Δ+9.0", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(9, result.getAbsoluteDelta(), 0.0);

        val1 = VFloat.of(15f, alarm, time, display);
        val2 = VFloat.of(6f, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("15 Δ+9.0", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(9, result.getAbsoluteDelta(), 0.0);

        val1 = VDouble.of(6d, alarm, time, display);
        val2 = VDouble.of(6d, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("6 Δ0.0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VFloat.of(5f, alarm, time, display);
        val2 = VFloat.of(6f, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("5 Δ-1.0", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VFloat.of(5f, alarm, time, display);
        val2 = VFloat.of(6f, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("5 Δ-1.0", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VFloat.of(5f, alarm, time, display);
        val2 = VFloat.of(6f, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("5 Δ-1.0", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);


        val1 = VLong.of(15L, alarm, time, display);
        val2 = VDouble.of(6d, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("15 Δ+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VLong.of(15L, alarm, time, display);
        val2 = VDouble.of(6d, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("15 Δ+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(9, result.getAbsoluteDelta(), 0.0);

        val1 = VULong.of(15L, alarm, time, display);
        val2 = VULong.of(6L, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("15 Δ+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(9, result.getAbsoluteDelta(), 0.0);

        val1 = VULong.of(5L, alarm, time, display);
        val2 = VULong.of(6L, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("5 Δ-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VUInt.of(15, alarm, time, display);
        val2 = VUInt.of(6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("15 Δ+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(9, result.getAbsoluteDelta(), 0.0);

        val1 = VUInt.of(15, alarm, time, display);
        val2 = VUInt.of(6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("15 Δ+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(9, result.getAbsoluteDelta(), 0.0);

        val1 = VInt.of(15, alarm, time, display);
        val2 = VInt.of(6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("15 Δ+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(9, result.getAbsoluteDelta(), 0.0);

        val1 = VDouble.of(15d, alarm, time, display);
        val2 = VLong.of(6L, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("15 Δ+9.0", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(9, result.getAbsoluteDelta(), 0.0);

        val1 = VDouble.of(15d, alarm, time, display);
        val2 = VLong.of(6L, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("15 Δ+9.0", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(9, result.getAbsoluteDelta(), 0.0);

        val1 = VDouble.of(15d, alarm, time, display);
        val2 = VLong.of(15L, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("15 Δ0.0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VString.of("first", alarm, time);
        val2 = VLong.of(15L, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("first", result.getString());
        assertNotEquals(0, result.getValuesEqual());
        assertFalse(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VDoubleArray.of(ArrayDouble.of(1, 2, 3), alarm, time, display);
        val2 = VDoubleArray.of(ArrayDouble.of(1, 2, 3), alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("[1.0, 2.0, 3.0]", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VDoubleArray.of(ArrayDouble.of(1, 2, 3), alarm, time, display);
        val2 = VLongArray.of(ArrayLong.of(1, 2, 3), alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("[1.0, 2.0, 3.0]", result.getString());
        assertNotEquals(0, result.getValuesEqual());
        assertFalse(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        //compare long values: equal, first less than second, second less than first
        val1 = VLong.of(6L, alarm, time, display);
        val2 = VLong.of(6L, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("6 Δ0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VLong.of(5L, alarm, time, display);
        val2 = VLong.of(6L, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("5 Δ-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VLong.of(6L, alarm, time, display);
        val2 = VLong.of(5L, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("6 Δ+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        //compare int values: equal, first less than second, second less than first
        val1 = VInt.of(6, alarm, time, display);
        val2 = VInt.of(6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("6 Δ0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VInt.of(5, alarm, time, display);
        val2 = VInt.of(6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("5 Δ-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VInt.of(6, alarm, time, display);
        val2 = VInt.of(5, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("6 Δ+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());

        //compare short values: equal, first less than second, second less than first
        val1 = VShort.of((short) 6, alarm, time, display);
        val2 = VShort.of((short) 6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("6 Δ0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        val1 = VShort.of((short) 5, alarm, time, display);
        val2 = VShort.of((short) 6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("5 Δ-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VShort.of((short) 6, alarm, time, display);
        val2 = VShort.of((short) 5, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("6 Δ+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VUShort.of((short) 6, alarm, time, display);
        val2 = VUShort.of((short) 5, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("6 Δ+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VUShort.of((short) 6, alarm, time, display);
        val2 = VUShort.of((short) 6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("6 Δ0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VUShort.of((short) 5, alarm, time, display);
        val2 = VUShort.of((short) 6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("5 Δ-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VShort.of((short) 5, alarm, time, display);
        val2 = VShort.of((short) 6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("5 Δ-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VUShort.of((short) 5, alarm, time, display);
        val2 = VUShort.of((short) 6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("5 Δ-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        //compare enum values: equal, first less than second, second less than first
        EnumDisplay labels = EnumDisplay.of("val1", "val2", "val3");

        val1 = VEnum.of(1, labels, alarm, time);
        val2 = VEnum.of(1, labels, alarm, time);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("val2", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VEnum.of(1, labels, alarm, time);
        val2 = VEnum.of(2, labels, alarm, time);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("val2", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VEnum.of(2, labels, alarm, time);
        val2 = VEnum.of(1, labels, alarm, time);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("val3", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VByte.of((byte) 5, alarm, time, display);
        val2 = VByte.of((byte) 6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("5 Δ-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VByte.of((byte) 6, alarm, time, display);
        val2 = VByte.of((byte) 5, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("6 Δ+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VByte.of((byte) 6, alarm, time, display);
        val2 = VByte.of((byte) 5, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("6 Δ+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VUByte.of((byte) 5, alarm, time, display);
        val2 = VUByte.of((byte) 6, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("5 Δ-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VUByte.of((byte) 6, alarm, time, display);
        val2 = VUByte.of((byte) 5, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, threshold);
        assertEquals("6 Δ+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());

        val1 = VUByte.of((byte) 6, alarm, time, display);
        val2 = VUByte.of((byte) 5, alarm, time, display);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("6 Δ+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());
        assertEquals(1, result.getAbsoluteDelta(), 0.0);

        val1 = VBoolean.of(false, alarm, time);
        val2 = VBoolean.of(false, alarm, time);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("false", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());
        assertEquals(0, result.getAbsoluteDelta(), 0.0);

        val1 = VString.of("a", alarm, time);
        val2 = VString.of("b", alarm, time);
        result = Utilities.valueToCompareString(val1, val2, Optional.empty());
        assertEquals("a", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());
        int delta = ((VString) val1).getValue().compareTo(((VString) val2).getValue());
        assertEquals(Math.abs(delta), result.getAbsoluteDelta(), 0.0);
    }

    /**
     * Tests {@link Utilities#areVTypesIdentical(VType, VType, boolean)} method.
     */
    @Test
    public void testVTypesIdentical() {
        Alarm alarm = Alarm.of(AlarmSeverity.INVALID, AlarmStatus.NONE, "name");
        Alarm alarm2 = Alarm.of(AlarmSeverity.INVALID, AlarmStatus.NONE, "name");
        Display display = Display.none();
        Time time = Time.now();
        Time time2 = Time.of(time.getTimestamp().plus(1, ChronoUnit.SECONDS));
        VType val1 = VDouble.of(5d, alarm, time, display);
        VType val2 = VDouble.of(6d, alarm2, time2, display);

        assertTrue(Utilities.areVTypesIdentical(null, null, false));
        assertFalse(Utilities.areVTypesIdentical(null, val1, false));
        assertFalse(Utilities.areVTypesIdentical(val1, null, false));

        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));
        val2 = VDouble.of(5d, alarm2, time, display);
        assertTrue(Utilities.areVTypesIdentical(val1, val2, true));
        assertTrue(Utilities.areVTypesIdentical(val1, val2, false));

        val2 = VDouble.of(5d, Alarm.none(), time, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertTrue(Utilities.areVTypesIdentical(val1, val2, false));

        val2 = VDouble.of(5d, alarm2, time2, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertTrue(Utilities.areVTypesIdentical(val1, val2, false));

        val2 = VLong.of(5L, alarm2, time, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));

        val1 = VFloat.of(5f, alarm, time, display);
        val2 = VFloat.of(6d, alarm2, time2, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));

        val1 = VULong.of(5L, alarm, time, display);
        val2 = VULong.of(6L, alarm2, time2, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));

        val1 = VLong.of(5L, alarm, time, display);
        val2 = VLong.of(6L, alarm2, time2, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));

        val1 = VUInt.of(5, alarm, time, display);
        val2 = VUInt.of(6, alarm2, time2, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));

        val1 = VInt.of(5, alarm, time, display);
        val2 = VInt.of(6, alarm2, time2, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));

        val1 = VUShort.of((short) 5, alarm, time, display);
        val2 = VUShort.of((short) 6, alarm2, time2, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));

        val1 = VShort.of((short) 5, alarm, time, display);
        val2 = VShort.of((short) 6, alarm2, time2, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));

        val1 = VUByte.of((byte) 5, alarm, time, display);
        val2 = VUByte.of((byte) 6, alarm2, time2, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));

        val1 = VByte.of((byte) 5, alarm, time, display);
        val2 = VByte.of((byte) 6, alarm2, time2, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));

        val1 = VBoolean.of(true, alarm, time);
        val2 = VBoolean.of(false, alarm2, time2);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));

        val1 = VEnum.of(1, EnumDisplay.of("a", "b", "c"), alarm, time);
        val2 = VEnum.of(2, EnumDisplay.of("a", "b", "c"), alarm2, time2);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val1, val2, false));
        VType val3 = VEnum.of(1, EnumDisplay.of("a", "b", "c"), alarm, time);
        assertTrue(Utilities.areVTypesIdentical(val1, val3, true));

        val1 = VDoubleArray.of(ArrayDouble.of(1, 2, 3), alarm, time, display);
        val2 = VDoubleArray.of(ArrayDouble.of(1, 2, 3), alarm, time, display);
        assertTrue(Utilities.areVTypesIdentical(val1, val2, true));
        assertTrue(Utilities.areVTypesIdentical(val1, val2, false));

        val2 = VDoubleArray.of(ArrayDouble.of(1, 2, 3, 4), alarm, time, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        val2 = VDoubleArray.of(ArrayDouble.of(1, 2, 4), alarm, time, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));

        val1 = VLongArray.of(ArrayLong.of(1, 2, 3), alarm, time, display);
        val2 = VLongArray.of(ArrayLong.of(1, 2, 3), alarm, time, display);
        assertTrue(Utilities.areVTypesIdentical(val1, val2, true));
        assertTrue(Utilities.areVTypesIdentical(val1, val2, false));

        val2 = VLongArray.of(ArrayLong.of(1, 2, 3, 4), alarm, time, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        val2 = VLongArray.of(ArrayLong.of(1, 2, 4), alarm, time, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));

        val1 = VULongArray.of(ArrayULong.of(1, 2, 3), alarm, time, display);
        val2 = VULongArray.of(ArrayULong.of(1, 2, 3), alarm, time, display);
        assertTrue(Utilities.areVTypesIdentical(val1, val2, true));
        assertTrue(Utilities.areVTypesIdentical(val1, val2, false));

        val2 = VULongArray.of(ArrayULong.of(1, 2, 3, 4), alarm, time, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        val2 = VULongArray.of(ArrayULong.of(1, 2, 4), alarm, time, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));

        val1 = VLongArray.of(ArrayLong.of(1, 2, 3), alarm, time, display);
        val2 = VLong.of(10L, alarm, time, display);
        assertFalse(Utilities.areVTypesIdentical(val1, val2, true));
        assertFalse(Utilities.areVTypesIdentical(val2, val1, true));
    }

    @Test
    public void testDeltaValueToString() {
        Alarm alarm = Alarm.none();
        Display display = Display.none();
        Time time = Time.now();

        Optional<Threshold<?>> threshold = Optional.of(new Threshold<>(5d, -5d));

        Utilities.VTypeComparison result = Utilities.deltaValueToString(null, null, threshold);
        assertEquals(VDisconnectedData.INSTANCE.toString(), result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        VType val1 = VDouble.of(5d, alarm, time, display);
        result = Utilities.deltaValueToString(null, val1, threshold);
        assertEquals(VDisconnectedData.INSTANCE.toString(), result.getString());
        assertEquals(-1, result.getValuesEqual());
        assertFalse(result.isWithinThreshold());

        result = Utilities.deltaValueToString(val1, null, threshold);
        assertEquals("5.0", result.getString());
        assertEquals(1, result.getValuesEqual());
        assertFalse(result.isWithinThreshold());

        result = Utilities.deltaValueToString(VDisconnectedData.INSTANCE, val1, threshold);
        assertEquals(VDisconnectedData.INSTANCE.toString(), result.getString());
        assertEquals(-1, result.getValuesEqual());
        assertFalse(result.isWithinThreshold());

        result = Utilities.deltaValueToString(val1, VDisconnectedData.INSTANCE, threshold);
        assertEquals("5.0", result.getString());
        assertEquals(1, result.getValuesEqual());
        assertFalse(result.isWithinThreshold());

        val1 = VDouble.of(5d, alarm, time, display);
        VType val2 = VDouble.of(6d, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("-1.0", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());


        val1 = VDouble.of(15d, alarm, time, display);
        val2 = VDouble.of(6d, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+9.0", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VFloat.of(15f, alarm, time, display);
        val2 = VFloat.of(6f, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+9.0", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VDouble.of(6d, alarm, time, display);
        val2 = VDouble.of(6d, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("0.0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        val1 = VFloat.of(5f, alarm, time, display);
        val2 = VFloat.of(6f, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("-1.0", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());

        val1 = VFloat.of(5f, alarm, time, display);
        val2 = VFloat.of(6f, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("-1.0", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());

        val1 = VFloat.of(5f, alarm, time, display);
        val2 = VFloat.of(6f, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("-1.0", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());


        val1 = VLong.of(15L, alarm, time, display);
        val2 = VDouble.of(6d, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VLong.of(15L, alarm, time, display);
        val2 = VDouble.of(6d, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VULong.of(15L, alarm, time, display);
        val2 = VULong.of(6L, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VULong.of(5L, alarm, time, display);
        val2 = VULong.of(6L, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());

        val1 = VUInt.of(15, alarm, time, display);
        val2 = VUInt.of(6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VUInt.of(15, alarm, time, display);
        val2 = VUInt.of(6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VInt.of(15, alarm, time, display);
        val2 = VInt.of(6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("+9", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VDouble.of(15d, alarm, time, display);
        val2 = VLong.of(6L, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+9.0", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VDouble.of(15d, alarm, time, display);
        val2 = VLong.of(6L, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("+9.0", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VDouble.of(15d, alarm, time, display);
        val2 = VLong.of(15L, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("0.0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        val1 = VString.of("first", alarm, time);
        val2 = VLong.of(15L, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("first", result.getString());
        assertNotEquals(0, result.getValuesEqual());
        assertFalse(result.isWithinThreshold());

        val1 = VDoubleArray.of(ArrayDouble.of(1, 2, 3), alarm, time, display);
        val2 = VDoubleArray.of(ArrayDouble.of(1, 2, 3), alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("---", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        val1 = VBooleanArray.of(ArrayBoolean.of(true, false), alarm, time);
        val2 = VBooleanArray.of(ArrayBoolean.of(true, false), alarm, time);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("---", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        //compare long values: equal, first less than second, second less than first
        val1 = VLong.of(6L, alarm, time, display);
        val2 = VLong.of(6L, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        val1 = VLong.of(5L, alarm, time, display);
        val2 = VLong.of(6L, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());

        val1 = VLong.of(6L, alarm, time, display);
        val2 = VLong.of(5L, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());

        //compare int values: equal, first less than second, second less than first
        val1 = VInt.of(6, alarm, time, display);
        val2 = VInt.of(6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        val1 = VInt.of(5, alarm, time, display);
        val2 = VInt.of(6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());

        val1 = VInt.of(6, alarm, time, display);
        val2 = VInt.of(5, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());

        //compare short values: equal, first less than second, second less than first
        val1 = VShort.of((short) 6, alarm, time, display);
        val2 = VShort.of((short) 6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        val1 = VShort.of((short) 5, alarm, time, display);
        val2 = VShort.of((short) 6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());

        val1 = VShort.of((short) 6, alarm, time, display);
        val2 = VShort.of((short) 5, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());

        val1 = VUShort.of((short) 6, alarm, time, display);
        val2 = VUShort.of((short) 5, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());

        val1 = VUShort.of((short) 6, alarm, time, display);
        val2 = VUShort.of((short) 6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("0", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        val1 = VUShort.of((short) 5, alarm, time, display);
        val2 = VUShort.of((short) 6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());

        val1 = VShort.of((short) 5, alarm, time, display);
        val2 = VShort.of((short) 6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());

        val1 = VUShort.of((short) 5, alarm, time, display);
        val2 = VUShort.of((short) 6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());

        //compare enum values: equal, first less than second, second less than first
        EnumDisplay labels = EnumDisplay.of("val1", "val2", "val3");

        val1 = VEnum.of(1, labels, alarm, time);
        val2 = VEnum.of(1, labels, alarm, time);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("val2", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        val1 = VEnum.of(1, labels, alarm, time);
        val2 = VEnum.of(2, labels, alarm, time);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("val2", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());

        val1 = VEnum.of(2, labels, alarm, time);
        val2 = VEnum.of(1, labels, alarm, time);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("val3", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VByte.of((byte) 5, alarm, time, display);
        val2 = VByte.of((byte) 6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());

        val1 = VByte.of((byte) 6, alarm, time, display);
        val2 = VByte.of((byte) 5, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());

        val1 = VByte.of((byte) 6, alarm, time, display);
        val2 = VByte.of((byte) 5, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VUByte.of((byte) 5, alarm, time, display);
        val2 = VUByte.of((byte) 6, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("-1", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertTrue(result.isWithinThreshold());

        val1 = VUByte.of((byte) 6, alarm, time, display);
        val2 = VUByte.of((byte) 5, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, threshold);
        assertEquals("+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertTrue(result.isWithinThreshold());

        val1 = VUByte.of((byte) 6, alarm, time, display);
        val2 = VUByte.of((byte) 5, alarm, time, display);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("+1", result.getString());
        assertTrue(result.getValuesEqual() > 0);
        assertFalse(result.isWithinThreshold());

        val1 = VBoolean.of(false, alarm, time);
        val2 = VBoolean.of(false, alarm, time);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("false", result.getString());
        assertEquals(0, result.getValuesEqual());
        assertTrue(result.isWithinThreshold());

        val1 = VString.of("a", alarm, time);
        val2 = VString.of("b", alarm, time);
        result = Utilities.deltaValueToString(val1, val2, Optional.empty());
        assertEquals("a", result.getString());
        assertTrue(result.getValuesEqual() < 0);
        assertFalse(result.isWithinThreshold());
    }

    @Test
    public void testDeltaValueToPercentage() {
        Alarm alarm = Alarm.none();
        Display display = Display.none();
        Time time = Time.now();

        VDouble val1 = VDouble.of(5d, alarm, time, display);
        VDouble val2 = VDouble.of(4d, alarm, time, display);
        String result = Utilities.deltaValueToPercentage(val1, val2);
        assertEquals("20%", result);

        val1 = VDouble.of(4d, alarm, time, display);
        result = Utilities.deltaValueToPercentage(val1, val2);
        assertEquals("", result);

        val2 = VDouble.of(0d, alarm, time, display);
        result = Utilities.deltaValueToPercentage(val1, val2);
        assertEquals("0 Live", result);

        val2 = VDouble.of(1d, alarm, time, display);
        val1 = VDouble.of(0d, alarm, time, display);
        result = Utilities.deltaValueToPercentage(val1, val2);
        assertEquals("0 Stored", result);

        VDoubleArray val = VDoubleArray.of(ArrayDouble.of(1d), alarm, time, display);
        result = Utilities.deltaValueToPercentage(val, val2);
        assertEquals("", result);
    }

    @Test
    public void testAreValuesEqual() {
        Alarm alarm = Alarm.none();
        Display display = Display.none();
        Time time = Time.now();

        Optional<Threshold<?>> threshold = Optional.of(new Threshold<>(5d, -5d));
        Optional<Threshold<?>> threshold2 = Optional.of(new Threshold<>(0.5d, -0.5d));

        assertTrue(Utilities.areValuesEqual(null, null, threshold));

        VType val1 = VDouble.of(10d, alarm, time, display);

        assertFalse(Utilities.areValuesEqual(val1, null, threshold));
        assertFalse(Utilities.areValuesEqual(null, val1, threshold));
        assertTrue(Utilities.areValuesEqual(VDisconnectedData.INSTANCE, VDisconnectedData.INSTANCE, threshold));
        assertFalse(Utilities.areValuesEqual(VDisconnectedData.INSTANCE, val1, threshold));
        assertFalse(Utilities.areValuesEqual(val1, VDisconnectedData.INSTANCE, threshold));

        VType val2 = VDouble.of(10d, alarm, time, display);

        boolean result = Utilities.areValuesEqual(val1, val2, Optional.empty());

        assertTrue(result);
        VType val3 = VDouble.of(11d, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val3, threshold);
        assertTrue(result);
        result = Utilities.areValuesEqual(val1, val3, threshold2);
        assertFalse(result);

        val1 = VFloat.of(10f, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val2, Optional.empty());
        assertTrue(result);
        result = Utilities.areValuesEqual(val1, val3, threshold);
        assertTrue(result);
        result = Utilities.areValuesEqual(val1, val3, threshold2);
        assertFalse(result);

        val2 = VULong.of(10L, alarm, time, display);
        val1 = VULong.of(10L, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val2, Optional.empty());
        assertTrue(result);
        val3 = VULong.of(11L, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val3, threshold);
        assertTrue(result);
        result = Utilities.areValuesEqual(val1, val3, threshold2);
        assertFalse(result);

        val2 = VLong.of(10L, alarm, time, display);
        val1 = VLong.of(10L, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val2, Optional.empty());
        assertTrue(result);
        val3 = VLong.of(11L, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val3, threshold);
        assertTrue(result);
        result = Utilities.areValuesEqual(val1, val3, threshold2);
        assertFalse(result);

        val2 = VUInt.of(10, alarm, time, display);
        val1 = VUInt.of(10, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val2, Optional.empty());
        assertTrue(result);
        val3 = VUInt.of(11, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val3, threshold);
        assertTrue(result);
        result = Utilities.areValuesEqual(val1, val3, threshold2);
        assertFalse(result);

        val2 = VInt.of(10L, alarm, time, display);
        val1 = VInt.of(10L, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val2, Optional.empty());
        assertTrue(result);
        val3 = VInt.of(11L, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val3, threshold);
        assertTrue(result);
        result = Utilities.areValuesEqual(val1, val3, threshold2);
        assertFalse(result);

        val2 = VUShort.of((short) 10, alarm, time, display);
        val1 = VUShort.of((short) 10, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val2, Optional.empty());
        assertTrue(result);
        val3 = VUShort.of((short) 11, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val3, threshold);
        assertTrue(result);
        result = Utilities.areValuesEqual(val1, val3, threshold2);
        assertFalse(result);

        val2 = VShort.of((short) 10, alarm, time, display);
        val1 = VShort.of((short) 10, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val2, Optional.empty());
        assertTrue(result);
        val3 = VShort.of((short) 11, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val3, threshold);
        assertTrue(result);
        result = Utilities.areValuesEqual(val1, val3, threshold2);
        assertFalse(result);

        val2 = VUByte.of((byte) 10, alarm, time, display);
        val1 = VUByte.of((byte) 10, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val2, Optional.empty());
        assertTrue(result);
        val3 = VUByte.of((byte) 11, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val3, threshold);
        assertTrue(result);
        result = Utilities.areValuesEqual(val1, val3, threshold2);
        assertFalse(result);

        val2 = VByte.of((byte) 10, alarm, time, display);
        val1 = VByte.of((byte) 10, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val2, Optional.empty());
        assertTrue(result);
        val3 = VByte.of((byte) 11, alarm, time, display);
        result = Utilities.areValuesEqual(val1, val3, threshold);
        assertTrue(result);
        result = Utilities.areValuesEqual(val1, val3, threshold2);
        assertFalse(result);

        val1 = VBoolean.of(true, alarm, time);
        val2 = VBoolean.of(true, alarm, time);
        val3 = VBoolean.of(false, alarm, time);
        assertTrue(Utilities.areValuesEqual(val1, val2, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(val1, val3, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(val1, null, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(null, val3, Optional.empty()));
        assertTrue(Utilities.areValuesEqual(null, null, Optional.empty()));

        val1 = VEnum.of(1, EnumDisplay.of("a", "b", "c"), alarm, time);
        val2 = VEnum.of(1, EnumDisplay.of("a", "b", "c"), alarm, time);
        val3 = VEnum.of(2, EnumDisplay.of("a", "b", "c"), alarm, time);
        assertTrue(Utilities.areValuesEqual(val1, val2, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(val1, val3, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(val1, null, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(null, val3, Optional.empty()));
        assertTrue(Utilities.areValuesEqual(null, null, Optional.empty()));

        val1 = VString.of("a", alarm, time);
        val2 = VString.of("a", alarm, time);
        val3 = VString.of("b", alarm, time);
        assertTrue(Utilities.areValuesEqual(val1, val2, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(val1, val3, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(val1, null, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(null, val3, Optional.empty()));
        assertTrue(Utilities.areValuesEqual(null, null, Optional.empty()));

        val1 = VDoubleArray.of(ArrayDouble.of(1, 2, 3), alarm, time, display);
        val2 = VDoubleArray.of(ArrayDouble.of(1, 2, 3), alarm, time, display);
        val3 = VDoubleArray.of(ArrayDouble.of(1, 2, 3, 4), alarm, time, display);
        assertTrue(Utilities.areValuesEqual(val1, val2, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(val1, val3, Optional.empty()));
        VType val4 = VDoubleArray.of(ArrayDouble.of(1, 2, 4), alarm, time, display);
        assertFalse(Utilities.areValuesEqual(val1, val4, Optional.empty()));

        val1 = VUIntArray.of(ArrayUInteger.of(1, 2, 3), alarm, time, display);
        val2 = VUIntArray.of(ArrayUInteger.of(1, 2, 3), alarm, time, display);
        val3 = VUIntArray.of(ArrayUInteger.of(1, 2, 3, 4), alarm, time, display);
        assertTrue(Utilities.areValuesEqual(val1, val2, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(val1, val3, Optional.empty()));
        val4 = VUIntArray.of(ArrayUInteger.of(1, 2, 4), alarm, time, display);
        assertFalse(Utilities.areValuesEqual(val1, val4, Optional.empty()));

        val1 = VLongArray.of(ArrayLong.of(1, 2, 3), alarm, time, display);
        val2 = VLongArray.of(ArrayLong.of(1, 2, 3), alarm, time, display);
        val3 = VLongArray.of(ArrayLong.of(1, 2, 3, 4), alarm, time, display);
        assertTrue(Utilities.areValuesEqual(val1, val2, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(val1, val3, Optional.empty()));
        val4 = VLongArray.of(ArrayLong.of(1, 2, 4), alarm, time, display);
        assertFalse(Utilities.areValuesEqual(val1, val4, Optional.empty()));

        val1 = VULongArray.of(ArrayULong.of(1, 2, 3), alarm, time, display);
        val2 = VULongArray.of(ArrayULong.of(1, 2, 3), alarm, time, display);
        val3 = VULongArray.of(ArrayULong.of(1, 2, 3, 4), alarm, time, display);
        assertTrue(Utilities.areValuesEqual(val1, val2, Optional.empty()));
        assertFalse(Utilities.areValuesEqual(val1, val3, Optional.empty()));
        val4 = VULongArray.of(ArrayULong.of(1, 2, 4), alarm, time, display);
        assertFalse(Utilities.areValuesEqual(val1, val4, Optional.empty()));

        val1 = VStringArray.of(Arrays.asList("a", "b", "c"), alarm, time);
        val2 = VStringArray.of(Arrays.asList("a", "b", "c"), alarm, time);
        assertTrue(Utilities.areValuesEqual(val1, val2, Optional.empty()));
    }

    @Test
    public void testAreVTablesEqual() {

        VTable vTable1 = VTable.of(List.of(Integer.TYPE, Double.TYPE),
                List.of("a", "b"),
                List.of(ArrayInteger.of(1, 2, 3), ArrayDouble.of(7.0, 8.0, 9.0)));
        VTable vTable2 = VTable.of(List.of(Integer.TYPE, Double.TYPE),
                List.of("a", "b"),
                List.of(ArrayInteger.of(1, 2, 3), ArrayDouble.of(7.0, 8.0, 9.0)));

        assertTrue(Utilities.areValuesEqual(vTable1, vTable2, Optional.empty()));

        vTable2 = VTable.of(List.of(Integer.TYPE, Integer.TYPE),
                List.of("a", "b"),
                List.of(ArrayInteger.of(1, 2, 3), ArrayInteger.of(7, 8, 9)));

        assertFalse(Utilities.areValuesEqual(vTable1, vTable2, Optional.empty()));

        vTable2 = VTable.of(List.of(Integer.TYPE, Double.TYPE),
                List.of("a", "b"),
                List.of(ArrayInteger.of(1, 2, 3), ArrayDouble.of(7.0, 8.0)));

        assertFalse(Utilities.areValuesEqual(vTable1, vTable2, Optional.empty()));

        vTable1 = VTable.of(List.of(String.class, Double.TYPE),
                List.of("a", "b"),
                List.of(List.of("AAA", "BBB", "CCC"), ArrayDouble.of(7.0, 8.0, 9.0)));

        vTable2 = VTable.of(List.of(String.class, Double.TYPE),
                List.of("a", "b"),
                List.of(List.of("AAA", "BBB", "CCC"), ArrayDouble.of(7.0, 8.0, 9.0)));

        assertTrue(Utilities.areValuesEqual(vTable1, vTable2, Optional.empty()));
    }

    @Test
    public void testAreVTypeArraysEqual() {
        assertTrue(Utilities.areVTypeArraysEqual(Integer.TYPE, ArrayInteger.of(1, 2, 3), ArrayInteger.of(1, 2, 3)));
        assertTrue(Utilities.areVTypeArraysEqual(Integer.TYPE, ArrayUInteger.of(1, 2, 3), ArrayUInteger.of(1, 2, 3)));
        assertTrue(Utilities.areVTypeArraysEqual(Long.TYPE, ArrayLong.of(1L, 2L, 3L), ArrayLong.of(1L, 2L, 3L)));
        assertTrue(Utilities.areVTypeArraysEqual(Long.TYPE, ArrayULong.of(1L, 2L, 3L), ArrayULong.of(1L, 2L, 3L)));
        assertTrue(Utilities.areVTypeArraysEqual(Double.TYPE, ArrayDouble.of(7.0, 8.0, 9.0), ArrayDouble.of(7.0, 8.0, 9.0)));
        assertTrue(Utilities.areVTypeArraysEqual(Float.TYPE, ArrayFloat.of(7.0f, 8.0f, 9.0f), ArrayFloat.of(7.0f, 8.0f, 9.0f)));
        assertTrue(Utilities.areVTypeArraysEqual(Short.TYPE, ArrayShort.of((short) 7.0, (short) 8.0, (short) 9.0), ArrayShort.of((short) 7.0, (short) 8.0, (short) 9.0)));
        assertTrue(Utilities.areVTypeArraysEqual(Short.TYPE, ArrayUShort.of((short) 7.0, (short) 8.0, (short) 9.0), ArrayUShort.of((short) 7.0, (short) 8.0, (short) 9.0)));
        assertTrue(Utilities.areVTypeArraysEqual(Byte.TYPE, ArrayByte.of((byte) 7, (byte) 8, (byte) 9), ArrayByte.of((byte) 7, (byte) 8, (byte) 9)));
        assertTrue(Utilities.areVTypeArraysEqual(Byte.TYPE, ArrayUByte.of((byte) 7, (byte) 8, (byte) 9), ArrayUByte.of((byte) 7, (byte) 8, (byte) 9)));
        assertTrue(Utilities.areVTypeArraysEqual(Boolean.TYPE, ArrayBoolean.of(true, false), ArrayBoolean.of(true, false)));

        assertTrue(Utilities.areVTypeArraysEqual(String.class, List.of("AAA", "BBB", "CCC"), List.of("AAA", "BBB", "CCC")));

        assertFalse(Utilities.areVTypeArraysEqual(Byte.TYPE, ArrayShort.of((byte) 7, (byte) 8, (byte) 9), ArrayShort.of((byte) 7, (byte) 8, (byte) 10)));
    }

    @Test
    public void testToPVArrayType() {

        boolean[] bools = new boolean[]{true, false, true};
        Object converted = VTypeHelper.toPVArrayType("bools", ArrayBoolean.of(bools));
        assertInstanceOf(PVABoolArray.class, converted);
        assertEquals(3, ((PVABoolArray) converted).get().length);
        assertArrayEquals(bools, ((PVABoolArray) converted).get());
        assertEquals("bools", ((PVABoolArray) converted).getName());

        byte[] bytes = new byte[]{(byte) -1, (byte) 2, (byte) 3};
        converted = VTypeHelper.toPVArrayType("bytes", ArrayByte.of(bytes));
        assertInstanceOf(PVAByteArray.class, converted);
        assertEquals(3, ((PVAByteArray) converted).get().length);
        assertArrayEquals(bytes, ((PVAByteArray) converted).get());
        assertEquals("bytes", ((PVAByteArray) converted).getName());
        assertFalse(((PVAByteArray) converted).isUnsigned());

        bytes = new byte[]{(byte) 1, (byte) 2, (byte) 3};
        converted = VTypeHelper.toPVArrayType("ubytes", ArrayUByte.of(bytes));
        assertInstanceOf(PVAByteArray.class, converted);
        assertEquals(3, ((PVAByteArray) converted).get().length);
        assertArrayEquals(bytes, ((PVAByteArray) converted).get());
        assertEquals("ubytes", ((PVAByteArray) converted).getName());
        assertTrue(((PVAByteArray) converted).isUnsigned());

        short[] shorts = new short[]{(short) -1, (short) 2, (short) 3};
        converted = VTypeHelper.toPVArrayType("shorts", ArrayShort.of(shorts));
        assertInstanceOf(PVAShortArray.class, converted);
        assertEquals(3, ((PVAShortArray) converted).get().length);
        assertArrayEquals(shorts, ((PVAShortArray) converted).get());
        assertEquals("shorts", ((PVAShortArray) converted).getName());
        assertFalse(((PVAShortArray) converted).isUnsigned());

        shorts = new short[]{(short) 1, (short) 2, (short) 3};
        converted = VTypeHelper.toPVArrayType("ushorts", ArrayUShort.of(shorts));
        assertInstanceOf(PVAShortArray.class, converted);
        assertEquals(3, ((PVAShortArray) converted).get().length);
        assertArrayEquals(shorts, ((PVAShortArray) converted).get());
        assertEquals("ushorts", ((PVAShortArray) converted).getName());
        assertTrue(((PVAShortArray) converted).isUnsigned());

        int[] ints = new int[]{-1, 2, 3};
        converted = VTypeHelper.toPVArrayType("ints", ArrayInteger.of(ints));
        assertInstanceOf(PVAIntArray.class, converted);
        assertEquals(3, ((PVAIntArray) converted).get().length);
        assertArrayEquals(ints, ((PVAIntArray) converted).get());
        assertEquals("ints", ((PVAIntArray) converted).getName());
        assertFalse(((PVAIntArray) converted).isUnsigned());

        ints = new int[]{1, 2, 3};
        converted = VTypeHelper.toPVArrayType("uints", ArrayUInteger.of(ints));
        assertInstanceOf(PVAIntArray.class, converted);
        assertEquals(3, ((PVAIntArray) converted).get().length);
        assertArrayEquals(ints, ((PVAIntArray) converted).get());
        assertEquals("uints", ((PVAIntArray) converted).getName());
        assertTrue(((PVAIntArray) converted).isUnsigned());

        long[] longs = new long[]{-1L, 2L, 3L};
        converted = VTypeHelper.toPVArrayType("longs", ArrayLong.of(longs));
        assertInstanceOf(PVALongArray.class, converted);
        assertEquals(3, ((PVALongArray) converted).get().length);
        assertArrayEquals(longs, ((PVALongArray) converted).get());
        assertEquals("longs", ((PVALongArray) converted).getName());
        assertFalse(((PVALongArray) converted).isUnsigned());

        longs = new long[]{1L, 2L, 3L};
        converted = VTypeHelper.toPVArrayType("ulongs", ArrayULong.of(longs));
        assertInstanceOf(PVALongArray.class, converted);
        assertEquals(3, ((PVALongArray) converted).get().length);
        assertArrayEquals(longs, ((PVALongArray) converted).get());
        assertEquals("ulongs", ((PVALongArray) converted).getName());
        assertTrue(((PVALongArray) converted).isUnsigned());

        float[] floats = new float[]{-1.0f, 2.0f, 3.0f};
        converted = VTypeHelper.toPVArrayType("floats", ArrayFloat.of(floats));
        assertInstanceOf(PVAFloatArray.class, converted);
        assertEquals(3, ((PVAFloatArray) converted).get().length);
        assertArrayEquals(floats, ((PVAFloatArray) converted).get());
        assertEquals("floats", ((PVAFloatArray) converted).getName());

        double[] doubles = new double[]{-1.0, 2.0, 3.0};
        converted = VTypeHelper.toPVArrayType("doubles", ArrayDouble.of(doubles));
        assertInstanceOf(PVADoubleArray.class, converted);
        assertEquals(3, ((PVADoubleArray) converted).get().length);
        assertArrayEquals(doubles, ((PVADoubleArray) converted).get());
        assertEquals("doubles", ((PVADoubleArray) converted).getName());

        List<String> strings = new ArrayList<>();
        strings.add("a");
        strings.add("b");
        strings.add("c");
        converted = VTypeHelper.toPVArrayType("strings", strings);
        assertInstanceOf(PVAStringArray.class, converted);
        assertEquals(3, ((PVAStringArray) converted).get().length);
        assertArrayEquals(new String[]{"a", "b", "c"}, ((PVAStringArray) converted).get());
        assertEquals("strings", ((PVAStringArray) converted).getName());
    }
}
