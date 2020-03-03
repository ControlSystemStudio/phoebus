package org.phoebus.core.vtypes;

import static org.junit.Assert.*;

import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayInteger;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;
import org.epics.vtype.VInt;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.Display;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;

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
    public void testGetSeverity(){

    }
}