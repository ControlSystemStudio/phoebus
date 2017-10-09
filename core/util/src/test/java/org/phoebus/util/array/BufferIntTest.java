/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.phoebus.util.array.ArrayInt;
import org.phoebus.util.array.BufferInt;
import org.phoebus.util.array.ListInt;
import static org.phoebus.util.array.CollectionTest.testCollection;
import static org.phoebus.util.array.ListTest.testList;

/**
 *
 * @author carcassi
 */
public class BufferIntTest {

    public BufferIntTest() {
    }

    @Test
    public void iteration1() {
        BufferInt coll = new BufferInt();
        for (int i = 0; i < 10; i++) {
            coll.addInt(1);
        }
        testCollection(coll);
        testList(coll);
    }

    @Test
    public void add1() {
        BufferInt coll = new BufferInt();
        for (int i = 0; i < 5; i++) {
            coll.addInt(1);
        }
        assertThat(coll.size(), equalTo(5));
        for (int i = 0; i < 5; i++) {
            coll.addInt(1);
        }
        assertThat(coll.size(), equalTo(10));
        for (int i = 0; i < 5; i++) {
            coll.addInt(1);
        }
        assertThat(coll.size(), equalTo(15));
    }

    @Test
    public void add2() {
        BufferInt coll = new BufferInt();
        for (int i = 0; i < 11; i++) {
            coll.addInt(i);
        }
        ListInt reference = new ArrayInt(new int[] {0, 1,2,3,4,5,6,7,8,9,10});
        assertThat(coll, equalTo(reference));
    }

    @Test
    public void add3() {
        BufferInt coll = new BufferInt();
        for (int i = 0; i < 5; i++) {
            coll.addInt(i);
        }
        ListInt reference = new ArrayInt(new int[] {0,1,2,3,4});
        assertThat(coll, equalTo(reference));
    }

    @Test
    public void clear1() {
        BufferInt coll = new BufferInt();
        for (int i = 0; i < 5; i++) {
            coll.addInt(i);
        }
        coll.clear();
        assertThat(coll.size(), equalTo(0));
    }
}
