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
import org.phoebus.util.array.ArrayByte;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author carcassi
 */
public class ArrayBooleanTest {

    public ArrayBooleanTest() {
    }

    @Test
    public void wrap1() {
        ArrayBoolean array = new ArrayBoolean(new boolean[] {true, false, true, true});
        assertThat(array.wrappedArray(), equalTo(new boolean[] {true, false, true, true}));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void wrap2() {
        ArrayBoolean array = new ArrayBoolean(new boolean[] {true, false, true, true});
        array.setBoolean(0, true);
    }

    @Test
    public void wrap3() {
        ArrayBoolean array = new ArrayBoolean(new boolean[] {true, false, true, true}, false);
        array.setBoolean(0, false);
        array.setBoolean(3, true);
        assertThat(array.wrappedArray(), equalTo(new boolean[] {false, false, true, true}));
    }

    @Test
    public void serialization1() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(buffer);
        ArrayByte array = new ArrayByte(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        stream.writeObject(array);
        ObjectInputStream inStream = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        ArrayByte read = (ArrayByte) inStream.readObject();
        assertThat(read, not(sameInstance(array)));
        assertThat(read, equalTo(array));
    }
}
