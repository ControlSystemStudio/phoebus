/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.phoebus.util.array.ArrayShort;
import org.phoebus.util.array.ListShort;
import static org.phoebus.util.array.CollectionTest.testCollection;
import static org.phoebus.util.array.ListTest.testList;

/**
 *
 * @author carcassi
 */
public class ListShortTest {

    public ListShortTest() {
    }

    @Test
    public void list1() {
        ListShort coll = new ListShort() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public short getShort(int index) {
                return 1;
            }
        };
        testCollection(coll);
        testList(coll);
    }

    @Test
    public void equals1() {
        ListShort coll = new ListShort() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public short getShort(int index) {
                return (short) index;
            }
        };
        ListShort other = new ArrayShort(new short[] {0,1,2,3,4,5,6,7,8,9});
        assertThat(coll, equalTo(other));
        assertThat(other, equalTo(coll));
    }

    @Test
    public void hashcode1() {
        ListShort coll = new ListShort() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public short getShort(int index) {
                return (short) index;
            }
        };
        ListShort other = new ArrayShort(new short[] {0,1,2,3,4,5,6,7,8,9});
        assertThat(coll.hashCode(), equalTo(other.hashCode()));
        assertThat(coll.hashCode(), equalTo(Arrays.hashCode(new short[] {0,1,2,3,4,5,6,7,8,9})));
    }

    @Test
    public void serialization1() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(buffer);
        ArrayShort array = new ArrayShort(new short[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        stream.writeObject(array);
        ObjectInputStream inStream = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        ArrayShort read = (ArrayShort) inStream.readObject();
        assertThat(read, not(sameInstance(array)));
        assertThat(read, equalTo(array));
    }

}
