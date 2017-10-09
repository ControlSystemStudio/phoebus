/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.phoebus.util.array.ArrayInt;
import org.phoebus.util.array.ListInt;
import static org.phoebus.util.array.CollectionTest.testCollection;
import static org.phoebus.util.array.ListTest.testList;

/**
 *
 * @author carcassi
 */
public class ListIntTest {

    public ListIntTest() {
    }

    @Test
    public void list1() {
        ListInt coll = new ListInt() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public int getInt(int index) {
                return 1;
            }
        };
        testCollection(coll);
        testList(coll);
    }

    @Test
    public void equals1() {
        ListInt coll = new ListInt() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public int getInt(int index) {
                return index;
            }
        };
        ListInt other = new ArrayInt(new int[] {0,1,2,3,4,5,6,7,8,9});
        assertThat(coll, equalTo(other));
        assertThat(other, equalTo(coll));
    }

    @Test
    public void hashcode1() {
        ListInt coll = new ListInt() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public int getInt(int index) {
                return index;
            }
        };
        ListInt other = new ArrayInt(new int[] {0,1,2,3,4,5,6,7,8,9});
        assertThat(coll.hashCode(), equalTo(other.hashCode()));
        assertThat(coll.hashCode(), equalTo(Arrays.hashCode(new int[] {0,1,2,3,4,5,6,7,8,9})));
    }

    @Test
    public void toString1() {
        ListInt coll = new ListInt() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public int getInt(int index) {
                return index;
            }
        };
        assertThat(coll.toString(), equalTo("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]"));
    }

}
