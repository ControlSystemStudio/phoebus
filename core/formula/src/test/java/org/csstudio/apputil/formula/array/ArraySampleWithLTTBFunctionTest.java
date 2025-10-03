package org.csstudio.apputil.formula.array;

import org.epics.util.array.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArraySampleWithLTTBFunctionTest {
    @Test
    void testThresholdEqualsOne() {
        ListNumber data = CollectionNumbers.toListDouble(1.0, 2.0, 3.0, 4.0);
        ListNumber result = ArraySampleWithLTTBFunction.sampleWithLTTB(data, 1);
        assertEquals(1, result.size());
        assertEquals(1.0, result.getDouble(0));
    }

    @Test
    void testThresholdEqualsTwo() {
        ListNumber data = CollectionNumbers.toListDouble(1.0, 2.0, 3.0, 4.0);
        ListNumber result = ArraySampleWithLTTBFunction.sampleWithLTTB(data, 2);
        assertEquals(2, result.size());
        assertEquals(1.0, result.getDouble(0));
        assertEquals(2.0, result.getDouble(1));
    }

    @Test
    void testThresholdGreaterThanDataSize() {
        ListNumber data = CollectionNumbers.toListDouble(1.0, 2.0, 3.0);
        ListNumber result = ArraySampleWithLTTBFunction.sampleWithLTTB(data, 5);
        assertEquals(data.size(), result.size());
        for (int i = 0; i < data.size(); i++) {
            assertEquals(data.getDouble(i), result.getDouble(i));
        }
    }

    @Test
    void testMonotonicIncreasing() {
        ListNumber data = CollectionNumbers.toListDouble(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        ListNumber result = ArraySampleWithLTTBFunction.sampleWithLTTB(data, 3);
        assertEquals(3, result.size());
        assertEquals(1.0, result.getDouble(0));
        assertEquals(5.0, result.getDouble(1));
        assertEquals(6.0, result.getDouble(2));
    }

    @Test
    void testConstantData() {
        ListNumber data = CollectionNumbers.toListDouble(5.0, 5.0, 5.0, 5.0, 5.0);
        ListNumber result = ArraySampleWithLTTBFunction.sampleWithLTTB(data, 3);
        assertEquals(3, result.size());
        assertEquals(5.0, result.getDouble(0));
        assertEquals(5.0, result.getDouble(1));
        assertEquals(5.0, result.getDouble(2));
    }

}

