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
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;
import org.epics.vtype.VUIntArray;
import org.epics.vtype.VUShortArray;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.Assert.*;

public class VTypeHelperTest {

    @Test
    public void testToDouble(){

        VDouble doubleValue = VDouble.of(7.7, Alarm.none(), Time.now(), Display.none());
        double result = VTypeHelper.toDouble(doubleValue);
        assertEquals(7.7, result, 0);

        VString stringValue = VString.of("7.7", Alarm.none(), Time.now());
        result = VTypeHelper.toDouble(doubleValue);
        assertEquals(7.7, result, 0);

        stringValue = VString.of("NotANumber", Alarm.none(), Time.now());
        result = VTypeHelper.toDouble(stringValue);
        assertEquals(Double.NaN, result, 0);

        VEnum enumValue = VEnum.of(7, EnumDisplay.of("a", "b"), Alarm.none(), Time.now());
        result = VTypeHelper.toDouble(enumValue);
        assertEquals(7.0, result, 0);

        VStatistics statisticsValue =
                VStatistics.of(7.7, 0.1, 0.0, 10.0, 5, Alarm.none(), Time.now(), Display.none());
        result = VTypeHelper.toDouble(statisticsValue);
        assertEquals(7.7, result, 0);

        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), Alarm.none(), Time.now(), Display.none());
        result = VTypeHelper.toDouble(doubleArray);
        assertEquals(7.7, result, 0);

        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), Alarm.none(), Time.now());

        result = VTypeHelper.toDouble(enumArray);
        assertEquals(0.0, result, 0);

        VBoolean booleanValue = VBoolean.of(true, Alarm.none(), Time.now());
        assertEquals(1.0, VTypeHelper.toDouble(booleanValue), 0);
        booleanValue = VBoolean.of(false, Alarm.none(), Time.now());
        assertEquals(0.0, VTypeHelper.toDouble(booleanValue), 0);
    }

    @Test
    public void testArrayToDoubleWithValidIndex(){
        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), Alarm.none(), Time.now(), Display.none());
        double result = VTypeHelper.toDouble(doubleArray, 0);
        assertEquals(7.7, result, 0);
        result = VTypeHelper.toDouble(doubleArray, 1);
        assertEquals(8.8, result, 0);

        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), Alarm.none(), Time.now());
        result = VTypeHelper.toDouble(enumArray);
        assertEquals(0.0, result, 0);
        assertEquals(1.0, result, 1);

        VDouble doubleValue = VDouble.of(7.7, Alarm.none(), Time.now(), Display.none());
        result = VTypeHelper.toDouble(doubleValue, 0);
        assertEquals(7.7, result, 0);
    }

    @Test
    public void testArrayToDoubleWithInvalidIndex1(){
        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), Alarm.none(), Time.now(), Display.none());
        double result = VTypeHelper.toDouble(doubleArray, 7);
        assertEquals(result, Double.NaN, 0);
    }

    @Test
    public void testArrayToDoubleWithInvalidIndex2(){
        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), Alarm.none(), Time.now());
        double result = VTypeHelper.toDouble(enumArray, 7);
        assertEquals(result, Double.NaN, 0);
    }

    @Test
    public void testArrayToDoubleWithInvalidIndex3(){
        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), Alarm.none(), Time.now(), Display.none());
        double result = VTypeHelper.toDouble(doubleArray, -1);
        assertEquals(result, Double.NaN, 0);
    }

    @Test
    public void testToDoubles(){
        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), Alarm.none(), Time.now(), Display.none());
        double[] result = VTypeHelper.toDoubles(doubleArray);
        assertEquals(2, result.length);
        assertEquals(7.7, result[0], 0);
        assertEquals(8.8, result[1], 0);

        VDouble doubleValue = VDouble.of(7.7, Alarm.none(), Time.now(), Display.none());
        result = VTypeHelper.toDoubles(doubleValue);

        assertEquals(0, result.length);
    }

    @Test
    public void testGetString(){
        assertEquals("null", VTypeHelper.toString(null));

        VTable table = VTable.of(Arrays.asList(VDouble.class),
                Arrays.asList("name"),
                Arrays.asList(ArrayDouble.of(7.7)));
        assertNotNull(VTypeHelper.toString(table));

        VDouble vDouble = VDouble.of(7.7, Alarm.disconnected(), Time.now(), Display.none());
        assertNull(VTypeHelper.toString(vDouble));
        vDouble = VDouble.of(7.7, Alarm.none(), Time.now(), Display.none());
        assertEquals("7.7", VTypeHelper.toString(vDouble));

        VEnum enumValue = VEnum.of(0, EnumDisplay.of("a", "b"), Alarm.none(), Time.now());
        assertEquals("a", VTypeHelper.toString(enumValue));

        assertEquals("b", VTypeHelper.toString(VString.of("b", Alarm.none(), Time.now())));
    }

    @Test
    public void testIsNumericArray(){

        VDouble vDouble = VDouble.of(7.7, Alarm.none(), Time.now(), Display.none());
        assertFalse(VTypeHelper.isNumericArray(vDouble));

        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), Alarm.none(), Time.now(), Display.none());
        assertTrue(VTypeHelper.isNumericArray(doubleArray));

        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), Alarm.none(), Time.now());
        assertTrue(VTypeHelper.isNumericArray(enumArray));
    }

    @Test
    public void testGetArraySize(){
        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), Alarm.none(), Time.now(), Display.none());
        assertEquals(2, VTypeHelper.getArraySize(doubleArray));

        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), Alarm.none(), Time.now());
        assertEquals(2, VTypeHelper.getArraySize(enumArray));

        VStringArray stringArray =
                VStringArray.of(Arrays.asList("a","b"), Alarm.none(), Time.now());
        assertEquals(2, VTypeHelper.getArraySize(stringArray));
    }

    @Test
    public void testGetLatestTimeOf(){
        Instant now = Instant.now();
        Time t1 = Time.of(Instant.EPOCH);
        Time t2 = Time.of(now);

        VInt i1 = VInt.of(1, Alarm.none(), t1, Display.none());
        VInt i2 = VInt.of(2, Alarm.none(), t2, Display.none());

        assertEquals(t2, VTypeHelper.lastestTimeOf(i1, i2));
        assertEquals(t2, VTypeHelper.lastestTimeOf(i2, i1));
    }

    @Test
    public void testGetTimestamp() throws Exception{
        Instant epoch = Instant.EPOCH;
        Time t = Time.of(epoch);

        VInt i1 = VInt.of(1, Alarm.none(), t, Display.none());
        assertEquals(epoch, VTypeHelper.getTimestamp(i1));

        t = Time.nowInvalid();
        i1 = VInt.of(1, Alarm.none(), t, Display.none());

        Instant now = Instant.now();
        Thread.sleep(2);
        assertTrue(VTypeHelper.getTimestamp(i1).isAfter(now));
    }

    @Test
    public void testTransformTimestamp(){
        Instant instant = Instant.now();

        VInt intValue =
                VInt.of(7, Alarm.none(), Time.of(Instant.EPOCH), Display.none());
        intValue = (VInt)VTypeHelper.transformTimestamp(intValue, instant);
        assertEquals(instant, intValue.getTime().getTimestamp());

        VDouble doubleValue = VDouble.of(7.7, Alarm.none(), Time.of(Instant.EPOCH), Display.none());
        doubleValue = (VDouble)VTypeHelper.transformTimestamp(doubleValue, instant);
        assertEquals(instant, doubleValue.getTime().getTimestamp());

        VString stringValue = VString.of("test", Alarm.none(), Time.of(Instant.EPOCH));
        stringValue = (VString)VTypeHelper.transformTimestamp(stringValue, instant);
        assertEquals(instant, stringValue.getTime().getTimestamp());

        VEnum enumValue = VEnum.of(7, EnumDisplay.of("a", "b"), Alarm.none(), Time.now());
        enumValue = (VEnum)VTypeHelper.transformTimestamp(enumValue, instant);
        assertEquals(instant, enumValue.getTime().getTimestamp());

        VDoubleArray doubleArray =
                VDoubleArray.of(ArrayDouble.of(7.7, 8.8), Alarm.none(), Time.now(), Display.none());
        doubleArray = (VDoubleArray)VTypeHelper.transformTimestamp(doubleArray, instant);
        assertEquals(instant, doubleArray.getTime().getTimestamp());

        VEnumArray enumArray =
                VEnumArray.of(ArrayInteger.of(0, 1), EnumDisplay.of("a", "b"), Alarm.none(), Time.now());

        assertNull(VTypeHelper.transformTimestamp(enumArray, instant));
    }


    @Test
    public void highestAlarmOf() {
        VType arg1 = VInt.of(0, Alarm.none(), Time.now(), Display.none());
        VType arg2 = VInt.of(0, Alarm.lolo(), Time.now(), Display.none());

        Alarm alarm =  VTypeHelper.highestAlarmOf(arg1, arg2);

        assertTrue("Failed to correctly calculate highest alarm expected LOLO, got : " + alarm,
                Alarm.lolo().equals(alarm));
    }

    @Test
    public void testFormatArrayNumbersArrayZeroLength(){
        ListInteger listInteger = ArrayInteger.of();
        Array array = VNumberArray.of(listInteger, Alarm.none(), Time.now(), Display.none());
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VIntArray", string);
    }

    @Test
    public void testFormatArrayZeroMax(){
        ListInteger listInteger = ArrayInteger.of(1, 2, 3, 4, 5);
        Array array = VNumberArray.of(listInteger, Alarm.none(), Time.now(), Display.none());
        String string = VTypeHelper.formatArray(array, 0);
        assertEquals("VIntArray", string);
    }

    @Test
    public void testFormatArrayNegativeMax(){
        ListInteger listInteger = ArrayInteger.of(1, 2, 3, 4, 5);
        Array array = VNumberArray.of(listInteger, Alarm.none(), Time.now(), Display.none());
        String string = VTypeHelper.formatArray(array, -1);
        assertEquals("VIntArray[1, 2, 3, 4, 5]", string);
    }

    @Test
    public void testFormatArrayWithSizes(){
        ListInteger sizes = ArrayInteger.of(2, 3);
        ListInteger listInteger = ArrayInteger.of(11, 12, 21, 22, 31, 32);
        Array array = VNumberArray.of(listInteger, sizes, Alarm.none(), Time.now(), Display.none());
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VIntArray[11, 12, 21,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VIntArray[11, 12, 21, 22, 31, 32]", string);
    }

    @Test
    public void testFormatIntArray(){
        ListInteger listInteger = ArrayInteger.of(-1, 2, 3, 4, 5);
        Array array = VIntArray.of(listInteger, Alarm.none(), Time.now(), Display.none());
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VIntArray[-1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VIntArray[-1, 2, 3, 4, 5]", string);

        ListUInteger listUInteger = ArrayUInteger.of(1, 2, 3, 4, 5);
        array = VUIntArray.of(listUInteger, Alarm.none(), Time.now(), Display.none());
        string = VTypeHelper.formatArray(array, 3);
        assertEquals("VUIntArray[1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VUIntArray[1, 2, 3, 4, 5]", string);
    }

    @Test
    public void testFormatLongArray(){
        ListLong list = ArrayLong.of(-1L, 2L, 3L, 4L, 5L);
        Array array = VLongArray.of(list, Alarm.none(), Time.now(), Display.none());
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VLongArray[-1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VLongArray[-1, 2, 3, 4, 5]", string);

        ListULong listU = ArrayULong.of(1L, 2L, 3L, 4L, 5L);
        array = VUIntArray.of(listU, Alarm.none(), Time.now(), Display.none());
        string = VTypeHelper.formatArray(array, 3);
        assertEquals("VULongArray[1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VULongArray[1, 2, 3, 4, 5]", string);
    }

    @Test
    public void testFormatShortArray(){
        ListShort list = ArrayShort.of((short)-1, (short)2, (short)3, (short)4, (short)5);
        Array array = VShortArray.of(list, Alarm.none(), Time.now(), Display.none());
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VShortArray[-1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VShortArray[-1, 2, 3, 4, 5]", string);

        ListUShort listU = ArrayUShort.of((short)1, (short)2, (short)3, (short)4, (short)5);
        array = VUShortArray.of(listU, Alarm.none(), Time.now(), Display.none());
        string = VTypeHelper.formatArray(array, 3);
        assertEquals("VUShortArray[1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VUShortArray[1, 2, 3, 4, 5]", string);
    }

    @Test
    public void testFormatByteArray(){
        ListByte list = ArrayByte.of((byte)-1, (byte)2, (byte)3, (byte)4, (byte)5);
        Array array = VByteArray.of(list, Alarm.none(), Time.now(), Display.none());
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VByteArray[-1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VByteArray[-1, 2, 3, 4, 5]", string);

        ListUByte listU = ArrayUByte.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5);
        array = VUShortArray.of(listU, Alarm.none(), Time.now(), Display.none());
        string = VTypeHelper.formatArray(array, 3);
        assertEquals("VUByteArray[1, 2, 3,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VUByteArray[1, 2, 3, 4, 5]", string);
    }

    @Test
    public void testFormatBooleanArray(){
        ListBoolean list = ArrayBoolean.of(true, true, false, false ,false);
        Array array = VBooleanArray.of(list, Alarm.none(), Time.now());
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VBooleanArray[true, true, false,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VBooleanArray[true, true, false, false, false]", string);
    }

    @Test
    public void testFormatDoubleArray(){
        ListDouble list = ArrayDouble.of(-1d, 0.27, 3.0f, 4.0f, 5.0f);
        Array array = VDoubleArray.of(list, Alarm.none(), Time.now(), Display.none());
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VDoubleArray[-1.0, 0.27, 3.0,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VDoubleArray[-1.0, 0.27, 3.0, 4.0, 5.0]", string);
    }

    @Test
    public void testFormatFloatArray(){
        ListFloat list = ArrayFloat.of(-1f, 0.27f, 3.0f, 4.0f, 5.0f);
        Array array = VDoubleArray.of(list, Alarm.none(), Time.now(), Display.none());
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VFloatArray[-1.0, 0.27, 3.0,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VFloatArray[-1.0, 0.27, 3.0, 4.0, 5.0]", string);
    }

    @Test
    public void testFormatStringArray(){
        Array array = VStringArray.of(Arrays.asList("a", "b", "c", "d", "e"), Alarm.none(), Time.now());
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VStringArray[a, b, c,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VStringArray[a, b, c, d, e]", string);
    }

    @Test
    public void testFormatEnumArray(){
        ListInteger listInteger = ArrayInteger.of(0, 1, 2, 3, 4);
        Array array = VEnumArray.of(listInteger, EnumDisplay.of("a", "b", "c", "d", "e"), Alarm.none(), Time.now());
        String string = VTypeHelper.formatArray(array, 3);
        assertEquals("VEnumArray[a, b, c,...", string);
        string = VTypeHelper.formatArray(array, 10);
        assertEquals("VEnumArray[a, b, c, d, e]", string);
    }


}