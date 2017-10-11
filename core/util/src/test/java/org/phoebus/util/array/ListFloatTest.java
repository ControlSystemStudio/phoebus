/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.phoebus.util.array.ArrayFloat;
import org.phoebus.util.array.ListFloat;
import static org.phoebus.util.array.CollectionTest.testCollection;
import static org.phoebus.util.array.ListTest.testList;

/**
 *
 * @author carcassi
 */
public class ListFloatTest {

    public ListFloatTest() {
    }

    @Test
    public void list1() {
        ListFloat coll = new ListFloat() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public float getFloat(int index) {
                return 1.0F;
            }
        };
        testCollection(coll);
        testList(coll);
    }

    @Test
    public void equals1() {
        ListFloat coll = new ListFloat() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public float getFloat(int index) {
                return index;
            }
        };
        ListFloat other = new ArrayFloat(new float[] {0,1,2,3,4,5,6,7,8,9});
        assertThat(coll, equalTo(other));
        assertThat(other, equalTo(coll));
    }

    @Test
    public void hashcode1() {
        ListFloat coll = new ListFloat() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public float getFloat(int index) {
                return index;
            }
        };
        ListFloat other = new ArrayFloat(new float[] {0,1,2,3,4,5,6,7,8,9});
        assertThat(coll.hashCode(), equalTo(other.hashCode()));
        assertThat(coll.hashCode(), equalTo(Arrays.hashCode(new float[] {0,1,2,3,4,5,6,7,8,9})));
    }

}
