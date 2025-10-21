package org.csstudio.apputil.formula.array;

import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ListDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ArrayReversalFunctionTest {
    private final ArrayReversalFunction function = new ArrayReversalFunction();

    @Test
    void testReverseTypicalArray() throws Exception {
        VNumberArray input = VNumberArray.of(ArrayDouble.of(1, 2, 3, 4, 5), Alarm.none(), Time.now(), Display.none());
        VType result = function.compute(input);
        assertTrue(result instanceof VNumberArray);
        double[] reversed = ((VNumberArray) result).getData().toArray(new double[input.getData().size()]);
        assertArrayEquals(new double[]{5, 4, 3, 2, 1}, reversed, 1e-9);
    }

    @Test
    void testReverseEmptyArray() throws Exception {
        VNumberArray input = VNumberArray.of(ArrayDouble.of(), Alarm.none(), Time.now(), Display.none());
        VType result = function.compute(input);
        assertTrue(result instanceof VNumberArray);
        double[] reversed = ((VNumberArray) result).getData().toArray(new double[input.getData().size()]);
        assertArrayEquals(new double[]{}, reversed, 1e-9);
    }

    @Test
    void testReverseSingleElementArray() throws Exception {
        VNumberArray input = VNumberArray.of(ArrayDouble.of(42), Alarm.none(), Time.now(), Display.none());
        VType result = function.compute(input);
        assertTrue(result instanceof VNumberArray);
        double[] reversed = ((VNumberArray) result).getData().toArray(new double[input.getData().size()]);
        assertArrayEquals(new double[]{42}, reversed, 1e-9);
    }

    @Test
    void testReverseNegativeNumbers() throws Exception {
        VNumberArray input = VNumberArray.of(ArrayDouble.of(-1, -2, -3), Alarm.none(), Time.now(), Display.none());
        VType result = function.compute(input);
        assertTrue(result instanceof VNumberArray);
        double[] reversed = ((VNumberArray) result).getData().toArray(new double[input.getData().size()]);
        assertArrayEquals(new double[]{-3, -2, -1}, reversed, 1e-9);
    }
}
