/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.phoebus.util.array.CircularBufferDouble;
import org.phoebus.util.array.ListDouble;
import static org.phoebus.util.array.CollectionTest.testCollection;
import static org.phoebus.util.array.ListTest.testList;

/**
 *
 * @author carcassi
 */
public class CircularBufferDoubleTest {

    public CircularBufferDoubleTest() {
    }

    @Test
    public void iteration1() {
        CircularBufferDouble coll = new CircularBufferDouble(15);
        for (int i = 0; i < 10; i++) {
            coll.addDouble(1.0);
        }
        testCollection(coll);
        testList(coll);
    }

    @Test
    public void add1() {
        CircularBufferDouble coll = new CircularBufferDouble(10);
        for (int i = 0; i < 5; i++) {
            coll.addDouble(1.0);
        }
        assertThat(coll.size(), equalTo(5));
        for (int i = 0; i < 5; i++) {
            coll.addDouble(1.0);
        }
        assertThat(coll.size(), equalTo(10));
        for (int i = 0; i < 5; i++) {
            coll.addDouble(1.0);
        }
        assertThat(coll.size(), equalTo(10));
    }

    @Test
    public void add2() {
        CircularBufferDouble coll = new CircularBufferDouble(10);
        for (int i = 0; i < 11; i++) {
            coll.addDouble(i);
        }
        ListDouble reference = new ArrayDouble(new double[] {1,2,3,4,5,6,7,8,9,10});
        assertThat(coll, equalTo(reference));
    }

    @Test
    public void add3() {
        CircularBufferDouble coll = new CircularBufferDouble(10);
        for (int i = 0; i < 5; i++) {
            coll.addDouble(i);
        }
        ListDouble reference = new ArrayDouble(new double[] {0,1,2,3,4});
        assertThat(coll, equalTo(reference));
    }

    @Test
    public void add4() {
        CircularBufferDouble coll = new CircularBufferDouble(3);
        for (int i = 0; i < 5; i++) {
            coll.addDouble(i);
        }
        ListDouble reference = new ArrayDouble(new double[] {2,3,4});
        assertThat(coll, equalTo(reference));
    }

    @Test
    public void add5() {
        CircularBufferDouble coll = new CircularBufferDouble(25);
        for (int i = 0; i < 5; i++) {
            coll.addDouble(i);
        }
        assertThat(coll.size(), equalTo(5));
        assertThat(coll.getCurrentCapacity(), equalTo(10));
        for (int i = 0; i < 5; i++) {
            coll.addDouble(i);
        }
        assertThat(coll.size(), equalTo(10));
        assertThat(coll.getCurrentCapacity(), equalTo(20));
        for (int i = 0; i < 10; i++) {
            coll.addDouble(i);
        }
        assertThat(coll.size(), equalTo(20));
        assertThat(coll.getCurrentCapacity(), equalTo(25));
    }

    @Test
    public void clear1() {
        CircularBufferDouble coll = new CircularBufferDouble(10);
        for (int i = 0; i < 5; i++) {
            coll.addDouble(i);
        }
        coll.clear();
        assertThat(coll.size(), equalTo(0));
    }
}
