/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import org.junit.Test;
import static org.junit.Assert.*;
import org.phoebus.util.array.ArrayShort;
import org.phoebus.util.array.CollectionNumbers;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author carcassi
 */
public class ArrayShortTest {

    public ArrayShortTest() {
    }

    @Test
    public void wrap1() {
        ArrayShort array = new ArrayShort(new short[] {0, 1, 2, 3, 4, 5});
        assertThat(CollectionNumbers.doubleArrayCopyOf(array), equalTo(new double[] {0, 1, 2, 3, 4, 5}));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void wrap2() {
        ArrayShort array = new ArrayShort(new short[] {0, 1, 2, 3, 4, 5});
        array.setDouble(0, 0);
    }

    @Test
    public void wrap3() {
        ArrayShort array = new ArrayShort(new short[] {0, 1, 2, 3, 4, 5}, false);
        array.setDouble(0, 5);
        array.setDouble(5, 0);
        assertThat(CollectionNumbers.doubleArrayCopyOf(array), equalTo(new double[] {5, 1, 2, 3, 4, 0}));
    }
}
