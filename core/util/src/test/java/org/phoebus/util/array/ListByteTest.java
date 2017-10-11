/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.phoebus.util.array.ArrayByte;
import org.phoebus.util.array.ListByte;
import static org.phoebus.util.array.CollectionTest.testCollection;
import static org.phoebus.util.array.ListTest.testList;

/**
 *
 * @author carcassi
 */
public class ListByteTest {

    public ListByteTest() {
    }

    @Test
    public void list1() {
        ListByte coll = new ListByte() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public byte getByte(int index) {
                return 1;
            }
        };
        testCollection(coll);
        testList(coll);
    }

    @Test
    public void equals1() {
        ListByte coll = new ListByte() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public byte getByte(int index) {
                return (byte) index;
            }
        };
        ListByte other = new ArrayByte(new byte[] {0,1,2,3,4,5,6,7,8,9});
        assertThat(coll, equalTo(other));
        assertThat(other, equalTo(coll));
    }

    @Test
    public void hashcode1() {
        ListByte coll = new ListByte() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public byte getByte(int index) {
                return (byte) index;
            }
        };
        ListByte other = new ArrayByte(new byte[] {0,1,2,3,4,5,6,7,8,9});
        assertThat(coll.hashCode(), equalTo(other.hashCode()));
        assertThat(coll.hashCode(), equalTo(Arrays.hashCode(new byte[] {0,1,2,3,4,5,6,7,8,9})));
    }

}
