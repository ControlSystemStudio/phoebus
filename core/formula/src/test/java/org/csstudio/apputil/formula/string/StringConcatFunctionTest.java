package org.csstudio.apputil.formula.string;

import org.epics.util.array.ArrayDouble;
import org.epics.vtype.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringConcatFunctionTest {

    @Test
    public void concatStrings() throws Exception {
        StringConcatFunction concatFunction = new StringConcatFunction();

        VString a = VString.of("a", Alarm.none(), Time.now());
        VString b = VString.of("b", Alarm.none(), Time.now());
        VString c = VString.of("c", Alarm.none(), Time.now());

        VString res = (VString) concatFunction.compute(a,b,c);
        assertEquals(res.getValue(), "abc");
    }

    @Test
    public void concatStringArray() throws Exception {
        StringConcatFunction concatFunction = new StringConcatFunction();

        VType array = VStringArray.of(Arrays.asList("a", "b", "c"), Alarm.none(), Time.now());

        VString res = (VString) concatFunction.compute(array);
        assertEquals(res.getValue(), "abc");
    }

    @Test
    public void concatDoubleArray() throws Exception {
        StringConcatFunction concatFunction = new StringConcatFunction();

        VType array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0), Alarm.none(), Time.now(), Display.none());

        VString res = (VString) concatFunction.compute(array);
        assertEquals(res.getValue(), "1.02.03.0");
    }

    @Test
    public void concatInvalidVType() throws Exception {
        StringConcatFunction concatFunction = new StringConcatFunction();

        VType num1 = VNumber.of(1.0, Alarm.none(), Time.now(), Display.none());
        VType num2 = VNumber.of(2.0, Alarm.none(), Time.now(), Display.none());
        VType num3 = VNumber.of(3.0, Alarm.none(), Time.now(), Display.none());

        VString res = (VString) concatFunction.compute(num1, num2, num3);
        // Will not attempt to concat and will return empty string
        assertEquals(res.getValue(), "");
    }

    @Test
    public void concatEnums() throws Exception {
        StringConcatFunction concatFunction = new StringConcatFunction();

        VEnum enum1 = VEnum.of(0, EnumDisplay.of("a", "b", "c"), Alarm.none(), Time.now());
        VEnum enum2 = VEnum.of(1, EnumDisplay.of("a", "b", "c"), Alarm.none(), Time.now());
        VEnum enum3 = VEnum.of(2, EnumDisplay.of("a", "b", "c"), Alarm.none(), Time.now());

        VString res = (VString) concatFunction.compute(enum1, enum2, enum3);
        assertEquals(res.getValue(), "abc");
    }
}
