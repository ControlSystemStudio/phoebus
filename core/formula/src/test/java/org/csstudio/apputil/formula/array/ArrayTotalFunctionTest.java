package org.csstudio.apputil.formula.array;

import org.epics.util.array.ArrayDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArrayTotalFunctionTest {

    @Test
    public void compute() {
        ArrayTotalFunction totalArrayFunction = new ArrayTotalFunction();

        assertEquals("arrayTotal",  totalArrayFunction.getName());
        assertEquals("array", totalArrayFunction.getCategory());

        VType array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0), Alarm.none(), Time.now(), Display.none());

        VNumber result = (VNumber)totalArrayFunction.compute(array);
        assertEquals(6.0, result.getValue());

        array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0, Double.NaN), Alarm.none(), Time.now(), Display.none());
        result = (VNumber)totalArrayFunction.compute(array);
        assertEquals(Double.NaN, result.getValue());

        array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0, -6.0), Alarm.none(), Time.now(), Display.none());
        result = (VNumber)totalArrayFunction.compute(array);
        assertEquals(0.0, result.getValue());
    }
}
