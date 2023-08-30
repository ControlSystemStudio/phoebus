package org.phoebus.core.vtypes;

import org.epics.util.array.ArrayBoolean;
import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayLong;
import org.epics.util.array.ArrayShort;
import org.epics.util.array.ArrayUByte;
import org.epics.util.array.ArrayUInteger;
import org.epics.util.array.ArrayULong;
import org.epics.util.array.ArrayUShort;
import org.epics.util.array.ListBoolean;
import org.epics.util.array.ListByte;
import org.epics.util.array.ListDouble;
import org.epics.util.array.ListFloat;
import org.epics.util.array.ListInteger;
import org.epics.util.array.ListLong;
import org.epics.util.array.ListShort;
import org.epics.util.array.ListUByte;
import org.epics.util.array.ListUInteger;
import org.epics.util.array.ListULong;
import org.epics.util.array.ListUShort;
import org.epics.vtype.Alarm;
import org.epics.vtype.Array;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VBooleanArray;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;
import org.epics.vtype.VUIntArray;
import org.epics.vtype.VULongArray;
import org.epics.vtype.VUShortArray;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VTypeHelperTest {

    private static Alarm alarm = Alarm.none();
    private static Time time = Time.now();
    private static Display display = Display.none();

    @Test
    public void testToDouble() {

        VDouble doubleValue = VDouble.of(7.7, alarm, time, display);
        double result = VTypeHelper.toDouble(doubleValue);
        assertEquals(7.7, result, 0);

        VString stringValue = VString.of("7.7", alarm, time);
        result = VTypeHelper.toDouble(doubleValue);
        assertEquals(7.7, result, 0);

        stringValue = VString.of("NotANumber", alarm, time);
        result = VTypeHelper.toDouble(stringValue);
        assertEquals(Double.NaN, result, 0);

        VEnum enumValue = VEnum.of(7, EnumDisplay.of("a", "b"), alarm, time);
        result = VTypeHelper.toDouble(enumValue);
        assertEquals(7.0, result, 0);

        VStatistics statisticsValue =
                VStatistics.of(7.7, 0.1, 0.0, 10.0, 5, alarm, time, display);
        result = VTypeHelper.toDouble(statisticsValue);
        assertEquals(7.7, result, 0);

        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), alarm, time, display);
        result = VTypeHelper.toDouble(doubleArray);
        assertEquals(7.7, result, 0);

        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), alarm, time);

        result = VTypeHelper.toDouble(enumArray);
        assertEquals(0.0, result, 0);

        VBoolean booleanValue = VBoolean.of(true, alarm, time);
        assertEquals(1.0, VTypeHelper.toDouble(booleanValue), 0);
        booleanValue = VBoolean.of(false, alarm, time);
        assertEquals(0.0, VTypeHelper.toDouble(booleanValue), 0);
    }

    @Test
    public void testArrayToDoubleWithValidIndex() {
        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), alarm, time, display);
        double result = VTypeHelper.toDouble(doubleArray, 0);
        assertEquals(7.7, result, 0);
        result = VTypeHelper.toDouble(doubleArray, 1);
        assertEquals(8.8, result, 0);

        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), alarm, time);
        result = VTypeHelper.toDouble(enumArray);
        assertEquals(0.0, result, 0);
        assertEquals(1.0, result, 1);

        VDouble doubleValue = VDouble.of(7.7, alarm, time, display);
        result = VTypeHelper.toDouble(doubleValue, 0);
        assertEquals(7.7, result, 0);
    }

    @Test
    public void testArrayToDoubleWithInvalidIndex1() {
        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), alarm, time, display);
        double result = VTypeHelper.toDouble(doubleArray, 7);
        assertEquals(result, Double.NaN, 0);
    }

    @Test
    public void testArrayToDoubleWithInvalidIndex2() {
        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), alarm, time);
        double result = VTypeHelper.toDouble(enumArray, 7);
        assertEquals(result, Double.NaN, 0);
    }

    @Test
    public void testArrayToDoubleWithInvalidIndex3() {
        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), alarm, time, display);
        double result = VTypeHelper.toDouble(doubleArray, -1);
        assertEquals(result, Double.NaN, 0);
    }

    @Test
    public void testToDoubles() {
        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), alarm, time, display);
        double[] result = VTypeHelper.toDoubles(doubleArray);
        assertEquals(2, result.length);
        assertEquals(7.7, result[0], 0);
        assertEquals(8.8, result[1], 0);

        VDouble doubleValue = VDouble.of(7.7, alarm, time, display);
        result = VTypeHelper.toDoubles(doubleValue);

        assertEquals(0, result.length);
    }

    @Test
    public void testGetString() {
        assertEquals("null", VTypeHelper.toString(null));

        VTable table = VTable.of(Arrays.asList(VDouble.class),
                Arrays.asList("name"),
                Arrays.asList(ArrayDouble.of(7.7)));
        assertNotNull(VTypeHelper.toString(table));

        VDouble vDouble = VDouble.of(7.7, Alarm.disconnected(), time, display);
        assertNull(VTypeHelper.toString(vDouble));
        vDouble = VDouble.of(7.7, alarm, time, display);
        assertEquals("7.7", VTypeHelper.toString(vDouble));

        VEnum enumValue = VEnum.of(0, EnumDisplay.of("a", "b"), alarm, time);
        assertEquals("a", VTypeHelper.toString(enumValue));

        assertEquals("b", VTypeHelper.toString(VString.of("b", alarm, time)));
    }

    @Test
    public void testIsNumericArray() {

        VDouble vDouble = VDouble.of(7.7, alarm, time, display);
        assertFalse(VTypeHelper.isNumericArray(vDouble));

        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), alarm, time, display);
        assertTrue(VTypeHelper.isNumericArray(doubleArray));

        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), alarm, time);
        assertTrue(VTypeHelper.isNumericArray(enumArray));
    }

    @Test
    public void testGetArraySize() {
        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), alarm, time, display);
        assertEquals(2, VTypeHelper.getArraySize(doubleArray));

        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), alarm, time);
        assertEquals(2, VTypeHelper.getArraySize(enumArray));

        VStringArray stringArray =
                VStringArray.of(Arrays.asList("a", "b"), alarm, time);
        assertEquals(2, VTypeHelper.getArraySize(stringArray));
    }

    @Test
    public void testGetLatestTimeOf() {
        Instant now = Instant.now();
        Time t1 = Time.of(Instant.EPOCH);
        Time t2 = Time.of(now);

        VInt i1 = VInt.of(1, alarm, t1, display);
        VInt i2 = VInt.of(2, alarm, t2, display);

        assertEquals(t2, VTypeHelper.lastestTimeOf(i1, i2));
        assertEquals(t2, VTypeHelper.lastestTimeOf(i2, i1));
    }

    @Test
    public void testGetTimestamp() throws Exception {
        Instant epoch = Instant.EPOCH;
        Time t = Time.of(epoch);

        VInt i1 = VInt.of(1, alarm, t, display);
        assertEquals(epoch, VTypeHelper.getTimestamp(i1));

        t = Time.nowInvalid();
        i1 = VInt.of(1, alarm, t, display);

        Instant now = Instant.now();
        Thread.sleep(2);
        assertTrue(VTypeHelper.getTimestamp(i1).isAfter(now));
    }

    @Test
    public void testTransformTimestamp() {
        Instant instant = Instant.now();

        VInt intValue =
                VInt.of(7, alarm, Time.of(Instant.EPOCH), display);
        intValue = (VInt) VTypeHelper.transformTimestamp(intValue, instant);
        assertEquals(instant, intValue.getTime().getTimestamp());

        VDouble doubleValue = VDouble.of(7.7, alarm, Time.of(Instant.EPOCH), display);
        doubleValue = (VDouble) VTypeHelper.transformTimestamp(doubleValue, instant);
        assertEquals(instant, doubleValue.getTime().getTimestamp());

        VString stringValue = VString.of("test", alarm, Time.of(Instant.EPOCH));
        stringValue = (VString) VTypeHelper.transformTimestamp(stringValue, instant);
        assertEquals(instant, stringValue.getTime().getTimestamp());

        VEnum enumValue = VEnum.of(7, EnumDisplay.of("a", "b"), alarm, time);
        enumValue = (VEnum) VTypeHelper.transformTimestamp(enumValue, instant);
        assertEquals(instant, enumValue.getTime().getTimestamp());

        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), alarm, time, display);
        doubleArray = (VDoubleArray) VTypeHelper.transformTimestamp(doubleArray, instant);
        assertEquals(instant, doubleArray.getTime().getTimestamp());

        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), alarm, time);

        assertNull(VTypeHelper.transformTimestamp(enumArray, instant));
    }


    @Test
    public void highestAlarmOf() {
        VType arg1 = VInt.of(0, alarm, time, display);
        VType arg2 = VInt.of(0, Alarm.lolo(), time, display);

        Alarm alarm = VTypeHelper.highestAlarmOf(arg1, arg2);

        assertTrue(
                Alarm.lolo().equals(alarm),
                "Failed to correctly calculate highest alarm expected LOLO, got : " + alarm);
    }

    @Test
    public void testFormatArrayNumbersArrayZeroLength() {
        ListInteger listInteger = ArrayInteger.of();
        Array array = VNumberArray.of(listInteger, alarm, time, display);
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VIntArray", string);
    }

    @Test
    public void testFormatArrayZeroMax() {
        ListInteger listInteger = ArrayInteger.of(1, 2, 3, 4, 5);
        Array array = VNumberArray.of(listInteger, alarm, time, display);
        String string = VTypeHelper.formatArray(array, 0);
        assertEquals("VIntArray", string);
    }

    @Test
    public void testFormatArrayNegativeMax() {
        ListInteger listInteger = ArrayInteger.of(1, 2, 3, 4, 5);
        Array array = VNumberArray.of(listInteger, alarm, time, display);
        String string = VTypeHelper.formatArray(array, -1);
        assertEquals("VIntArray[1, 2, 3, 4, 5]", string);
    }

    @Test
    public void testFormatArrayWithSizes() {
        ListInteger sizes = ArrayInteger.of(2, 3);
        ListInteger listInteger = ArrayInteger.of(11, 12, 21, 22, 31, 32);
        Array array = VNumberArray.of(listInteger, sizes, alarm, time, display);
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VIntArray[11, 12, 21,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VIntArray[11, 12, 21, 22, 31, 32]", string);
    }

    @Test
    public void testFormatIntArray() {
        ListInteger listInteger = ArrayInteger.of(-1, 2, 3, 4, 5);
        Array array = VIntArray.of(listInteger, alarm, time, display);
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VIntArray[-1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VIntArray[-1, 2, 3, 4, 5]", string);

        ListUInteger listUInteger = ArrayUInteger.of(1, 2, 3, 4, 5);
        array = VUIntArray.of(listUInteger, alarm, time, display);
        string = VTypeHelper.formatArray(array, 3);
        assertEquals("VUIntArray[1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VUIntArray[1, 2, 3, 4, 5]", string);
    }

    @Test
    public void testFormatLongArray() {
        ListLong list = ArrayLong.of(-1L, 2L, 3L, 4L, 5L);
        Array array = VLongArray.of(list, alarm, time, display);
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VLongArray[-1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VLongArray[-1, 2, 3, 4, 5]", string);

        ListULong listU = ArrayULong.of(1L, 2L, 3L, 4L, 5L);
        array = VUIntArray.of(listU, alarm, time, display);
        string = VTypeHelper.formatArray(array, 3);
        assertEquals("VULongArray[1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VULongArray[1, 2, 3, 4, 5]", string);
    }

    @Test
    public void testFormatShortArray() {
        ListShort list = ArrayShort.of((short) -1, (short) 2, (short) 3, (short) 4, (short) 5);
        Array array = VShortArray.of(list, alarm, time, display);
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VShortArray[-1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VShortArray[-1, 2, 3, 4, 5]", string);

        ListUShort listU = ArrayUShort.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        array = VUShortArray.of(listU, alarm, time, display);
        string = VTypeHelper.formatArray(array, 3);
        assertEquals("VUShortArray[1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VUShortArray[1, 2, 3, 4, 5]", string);
    }

    @Test
    public void testFormatByteArray() {
        ListByte list = ArrayByte.of((byte) -1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Array array = VByteArray.of(list, alarm, time, display);
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VByteArray[-1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VByteArray[-1, 2, 3, 4, 5]", string);

        ListUByte listU = ArrayUByte.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        array = VUShortArray.of(listU, alarm, time, display);
        string = VTypeHelper.formatArray(array, 3);
        assertEquals("VUByteArray[1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VUByteArray[1, 2, 3, 4, 5]", string);
    }

    @Test
    public void testFormatBooleanArray() {
        ListBoolean list = ArrayBoolean.of(true, true, false, false, false);
        Array array = VBooleanArray.of(list, alarm, time);
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VBooleanArray[true, true, false,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VBooleanArray[true, true, false, false, false]", string);
    }

    @Test
    public void testFormatDoubleArray() {
        ListDouble list = ArrayDouble.of(-1d, 0.27, 3.0f, 4.0f, 5.0f);
        Array array = VDoubleArray.of(list, alarm, time, display);
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VDoubleArray[-1.0, 0.27, 3.0,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VDoubleArray[-1.0, 0.27, 3.0, 4.0, 5.0]", string);
    }

    @Test
    public void testFormatFloatArray() {
        ListFloat list = ArrayFloat.of(-1f, 0.27f, 3.0f, 4.0f, 5.0f);
        Array array = VDoubleArray.of(list, alarm, time, display);
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VFloatArray[-1.0, 0.27, 3.0,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VFloatArray[-1.0, 0.27, 3.0, 4.0, 5.0]", string);
    }

    @Test
    public void testFormatStringArray() {
        Array array = VStringArray.of(Arrays.asList("a", "b", "c", "d", "e"), alarm, time);
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VStringArray[a, b, c,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VStringArray[a, b, c, d, e]", string);
    }

    @Test
    public void testFormatEnumArray() {
        ListInteger listInteger = ArrayInteger.of(0, 1, 2, 3, 4);
        Array array = VEnumArray.of(listInteger, EnumDisplay.of("a", "b", "c", "d", "e"), alarm, time);
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VEnumArray[a, b, c,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VEnumArray[a, b, c, d, e]", string);
    }

    @Test
    public void testToBoolean() {
        // Test parsing of VBoolean to boolean
        VType vtype = VBoolean.of(Boolean.TRUE, alarm, time);
        assertEquals(true, VTypeHelper.toBoolean(vtype),
                "Failed to parse boolean from VBoolean : " + vtype.toString());
        vtype = VBoolean.of(Boolean.FALSE, alarm, time);
        assertEquals(false, VTypeHelper.toBoolean(vtype),
                "Failed to parse boolean from VBoolean : " + vtype.toString());

        // Test parsing of VString to boolean
        vtype = VString.of("true", alarm, time);
        assertEquals(true, VTypeHelper.toBoolean(vtype),
                "Failed to parse boolean from VString : " + vtype.toString());
        vtype = VString.of("TRUE", alarm, time);
        assertEquals(true, VTypeHelper.toBoolean(vtype),
                "Failed to parse boolean from VString : " + vtype.toString());
        vtype = VString.of("false", alarm, time);
        assertEquals(false, VTypeHelper.toBoolean(vtype),
                "Failed to parse boolean from VString : " + vtype.toString());
        vtype = VString.of("FALSE", alarm, time);
        assertEquals(false, VTypeHelper.toBoolean(vtype),
                "Failed to parse boolean from VString : " + vtype.toString());

        // Test parsing of VNumber to boolean
        vtype = VNumber.of(0, alarm, time, display);
        assertEquals(false, VTypeHelper.toBoolean(vtype),
                "Failed to parse boolean from VNumber : " + vtype.toString());
        vtype = VNumber.of(0.0, alarm, time, display);
        assertEquals(false, VTypeHelper.toBoolean(vtype),
                "Failed to parse boolean from VNumber : " + vtype.toString());
        vtype = VNumber.of(0.001, alarm, time, display);
        assertEquals(true, VTypeHelper.toBoolean(vtype),
                "Failed to parse boolean from VNumber : " + vtype.toString());
        vtype = VNumber.of(1, alarm, time, display);
        assertEquals(true, VTypeHelper.toBoolean(vtype),
                "Failed to parse boolean from VNumber : " + vtype.toString());
    }

    @Test
    public void testArrayToIntegersWithValidIndex() {
        VIntArray intArray =
                VIntArray.of(ArrayInteger.of(7, 8), alarm, time, display);
        int[] integers = VTypeHelper.toIntegers(intArray);
        assertEquals(integers[0], 7);
        assertEquals(integers[1], 8);

        assertTrue(VTypeHelper.toIntegers(VDouble.of(7.0, alarm, time, display)).length == 0);

        VUIntArray uintArray =
                VUIntArray.of(ArrayUInteger.of(7, 8), alarm, time, display);
        integers = VTypeHelper.toIntegers(uintArray);
        assertEquals(integers[0], 7);
        assertEquals(integers[1], 8);
    }

    @Test
    public void testArrayToLongsWithValidIndex() {
        VLongArray longArray =
                VLongArray.of(ArrayLong.of(7L, 8L), alarm, time, display);
        long[] longs = VTypeHelper.toLongs(longArray);
        assertEquals(longs[0], 7);
        assertEquals(longs[1], 8);

        assertTrue(VTypeHelper.toLongs(VDouble.of(7.0, alarm, time, display)).length == 0);

        VULongArray ulongArray =
                VULongArray.of(ArrayULong.of(7L, 8L), alarm, time, display);
        longs = VTypeHelper.toLongs(ulongArray);
        assertEquals(longs[0], 7);
        assertEquals(longs[1], 8);
    }

    @Test
    public void testArrayToShortsWithValidIndex() {
        VShortArray shortArray =
                VShortArray.of(ArrayShort.of((short) 7, (short) 8), alarm, time, display);
        short[] shorts = VTypeHelper.toShorts(shortArray);
        assertEquals(shorts[0], 7);
        assertEquals(shorts[1], 8);

        assertTrue(VTypeHelper.toShorts(VDouble.of(7.0, alarm, time, display)).length == 0);

        VUShortArray ushortArray =
                VUShortArray.of(ArrayUShort.of((short) 7, (short) 8), alarm, time, display);
        shorts = VTypeHelper.toShorts(ushortArray);
        assertEquals(shorts[0], 7);
        assertEquals(shorts[1], 8);
    }

    @Test
    public void testArrayToFloatsWithValidIndex() {
        VFloatArray floatArray =
                VFloatArray.of(ArrayFloat.of(7.0f, 8.0f), alarm, time, display);
        float[] floats = VTypeHelper.toFloats(floatArray);
        assertEquals(floats[0], 7, 0);
        assertEquals(floats[1], 8, 0);

        assertTrue(VTypeHelper.toFloats(VDouble.of(7.0, alarm, time, display)).length == 0);
    }

    @Test
    public void testArrayToBytesWithValidIndex() {
        VByteArray byteArray =
                VByteArray.of(ArrayByte.of((byte) 7, (byte) 8), alarm, time, display);
        byte[] bytes = VTypeHelper.toBytes(byteArray);
        assertEquals(bytes[0], 7);
        assertEquals(bytes[1], 8);

        assertTrue(VTypeHelper.toBytes(VDouble.of(7.0, alarm, time, display)).length == 0);
    }

    @Test
    public void testArrayToBooleansWithValidIndex() {
        VBooleanArray boolArray =
                VBooleanArray.of(ArrayBoolean.of(true, false), alarm, time);
        boolean[] booleans = VTypeHelper.toBooleans(boolArray);
        assertTrue(booleans[0]);
        assertFalse(booleans[1]);

        assertTrue(VTypeHelper.toBooleans(VDouble.of(7.0, alarm, time, display)).length == 0);
    }
}