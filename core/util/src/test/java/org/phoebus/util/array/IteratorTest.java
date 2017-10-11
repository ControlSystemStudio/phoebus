/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import org.phoebus.util.array.IteratorByte;
import org.phoebus.util.array.IteratorDouble;
import org.phoebus.util.array.IteratorFloat;
import org.phoebus.util.array.IteratorInt;
import org.phoebus.util.array.IteratorLong;
import org.phoebus.util.array.IteratorNumber;
import org.phoebus.util.array.IteratorShort;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author carcassi
 */
public class IteratorTest {

    public IteratorTest() {
    }

    @org.junit.BeforeClass
    public static void setUpClass() throws Exception {
    }

    @org.junit.AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testIteratorDouble() {
        IteratorDouble iter = new IteratorDouble() {

            public boolean hasNext() {
                return true;
            }

            public double nextDouble() {
                return 1.0;
            }
        };
        testIterator(iter);
    }

    @Test
    public void testIteratorFloat() {
        IteratorFloat iter = new IteratorFloat() {

            public boolean hasNext() {
                return true;
            }

            public float nextFloat() {
                return (float) 1.0;
            }
        };
        testIterator(iter);
    }

    @Test
    public void testIteratorLong() {
        IteratorLong iter = new IteratorLong() {

            public boolean hasNext() {
                return true;
            }

            public long nextLong() {
                return 1L;
            }
        };
        testIterator(iter);
    }

    @Test
    public void testIteratorInt() {
        IteratorInt iter = new IteratorInt() {

            public boolean hasNext() {
                return true;
            }

            public int nextInt() {
                return 1;
            }
        };
        testIterator(iter);
    }

    @Test
    public void testIteratorShort() {
        IteratorShort iter = new IteratorShort() {

            public boolean hasNext() {
                return true;
            }

            public short nextShort() {
                return 1;
            }
        };
        testIterator(iter);
    }

    @Test
    public void testIteratorByte() {
        IteratorByte iter = new IteratorByte() {

            public boolean hasNext() {
                return true;
            }

            public byte nextByte() {
                return 1;
            }
        };
        testIterator(iter);
    }

    public void testIterator(IteratorNumber iter) {
        for (int i = 0; i < 20; i++) {
            assertTrue(iter.hasNext());
            assertEquals(1.0, iter.nextDouble(), 0.00001);
            assertEquals((float) 1.0, iter.nextFloat(), 0.00001);
            assertEquals(1L, iter.nextLong());
            assertEquals(1, iter.nextInt());
            assertEquals((short) 1, iter.nextShort());
            assertEquals((byte) 1, iter.nextByte());
        }
    }
}
