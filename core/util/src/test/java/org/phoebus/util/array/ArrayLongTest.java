/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;
import org.phoebus.util.array.ArrayLong;
import org.phoebus.util.array.CollectionNumbers;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author carcassi
 */
public class ArrayLongTest {

    public ArrayLongTest() {
    }

    @Test
    public void wrap1() {
        ArrayLong array = new ArrayLong(new long[] {0, 1, 2, 3, 4, 5});
        assertThat(CollectionNumbers.doubleArrayCopyOf(array), equalTo(new double[] {0, 1, 2, 3, 4, 5}));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void wrap2() {
        ArrayLong array = new ArrayLong(0, 1, 2, 3, 4, 5);
        array.setDouble(0, 0);
    }

    @Test
    public void wrap3() {
        ArrayLong array = new ArrayLong(new long[] {0, 1, 2, 3, 4, 5}, false);
        array.setDouble(0, 5);
        array.setDouble(5, 0);
        assertThat(CollectionNumbers.doubleArrayCopyOf(array), equalTo(new double[] {5, 1, 2, 3, 4, 0}));
    }

    @Test
    public void serialization1() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(buffer);
        ArrayLong array = new ArrayLong(new long[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        stream.writeObject(array);
        ObjectInputStream inStream = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        ArrayLong read = (ArrayLong) inStream.readObject();
        assertThat(read, not(sameInstance(array)));
        assertThat(read, equalTo(array));
    }
}
