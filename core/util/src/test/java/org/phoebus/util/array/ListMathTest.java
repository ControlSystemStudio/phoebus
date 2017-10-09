/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.phoebus.util.array.ArrayLong;
import org.phoebus.util.array.CollectionNumbers;
import org.phoebus.util.array.ListDouble;
import org.phoebus.util.array.ListLong;
import org.phoebus.util.array.ListMath;
import org.phoebus.util.array.ListNumber;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author carcassi
 */
public class ListMathTest {

    public ListMathTest() {
    }

    @Test
    public void rescale1() {
        ArrayDouble array1 = new ArrayDouble(new double[] {0, 1, 2, 3, 4, 5});
        ListDouble rescaled = ListMath.rescale(array1, 2.5, -5.0);
        assertThat(CollectionNumbers.doubleArrayCopyOf(rescaled), equalTo(new double[] {-5.0, -2.5, 0, 2.5, 5.0, 7.5}));
    }

    @Test
    public void rescaleWithfactor1() {
        ArrayDouble array1 = new ArrayDouble(new double[] {0, 1, 2, 3, 4, 5});
        ListDouble rescaled = ListMath.rescale(array1, 1, 1);
        assertThat(CollectionNumbers.doubleArrayCopyOf(rescaled), equalTo(new double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0}));
    }

    @Test
    public void sum1() {
        ArrayDouble array1 = new ArrayDouble(new double[] {0, 1, 2, 3, 4, 5});
        ListDouble summed = ListMath.add(array1, ListMath.rescale(array1, -1.0, 0.0));
        assertThat(CollectionNumbers.doubleArrayCopyOf(summed), equalTo(new double[] {0, 0, 0, 0, 0, 0}));
    }

    @Test
    public void limit1() {
        ListDouble array1 = new ArrayDouble(0, 1, 2, 3, 4, 5);
        ListDouble limit = ListMath.limit(array1, 1, 5);
        ListDouble reference = new ArrayDouble(1, 2, 3, 4);
        assertThat(limit, equalTo(reference));
    }

    @Test
    public void limit2() {
        ListLong array1 = new ArrayLong(0, 1, 2, 3, 4, 5);
        ListLong limit = ListMath.limit(array1, 1, 5);
        ListLong reference = new ArrayLong(1, 2, 3, 4);
        assertThat(limit, equalTo(reference));
    }

    @Test
    public void dft1() {
        ListDouble x = new ArrayDouble(0, 1.0, 0, -1.0, 0, 1, 0, -1);
        ListDouble y = new ArrayDouble(0, 0, 0, 0, 0, 0, 0, 0);
        List<ListNumber> res = ListMath.dft(x, y);
    }
}
