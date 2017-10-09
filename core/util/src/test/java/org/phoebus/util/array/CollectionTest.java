/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import org.phoebus.util.array.CollectionByte;
import org.phoebus.util.array.CollectionDouble;
import org.phoebus.util.array.CollectionFloat;
import org.phoebus.util.array.CollectionInt;
import org.phoebus.util.array.CollectionLong;
import org.phoebus.util.array.CollectionNumber;
import org.phoebus.util.array.CollectionShort;
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
public class CollectionTest {

    public CollectionTest() {
    }

    @org.junit.BeforeClass
    public static void setUpClass() throws Exception {
    }

    @org.junit.AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testCollectionDouble() {
        CollectionDouble coll = new CollectionDouble() {

            public IteratorDouble iterator() {
                return new IteratorDouble() {

                    int n=0;

                    public boolean hasNext() {
                        return n < 10;
                    }

                    public double nextDouble() {
                        n++;
                        return 1.0;
                    }
                };
            }

            public int size() {
                return 10;
            }
        };
        testCollection(coll);
    }

    @Test
    public void testCollectionFloat() {
        CollectionFloat coll = new CollectionFloat() {

            public IteratorFloat iterator() {
                return new IteratorFloat() {

                    int n=0;

                    public boolean hasNext() {
                        return n < 10;
                    }

                    public float nextFloat() {
                        n++;
                        return (float) 1.0;
                    }
                };
            }

            public int size() {
                return 10;
            }
        };
        testCollection(coll);
    }

    @Test
    public void testCollectionLong() {
        CollectionLong coll = new CollectionLong() {

            public IteratorLong iterator() {
                return new IteratorLong() {

                    int n=0;

                    public boolean hasNext() {
                        return n < 10;
                    }

                    public long nextLong() {
                        n++;
                        return 1L;
                    }
                };
            }

            public int size() {
                return 10;
            }
        };
        testCollection(coll);
    }

    @Test
    public void testCollectionInt() {
        CollectionInt coll = new CollectionInt() {

            public IteratorInt iterator() {
                return new IteratorInt() {

                    int n=0;

                    public boolean hasNext() {
                        return n < 10;
                    }

                    public int nextInt() {
                        n++;
                        return 1;
                    }
                };
            }

            public int size() {
                return 10;
            }
        };
        testCollection(coll);
    }

    @Test
    public void testCollectionShort() {
        CollectionShort coll = new CollectionShort() {

            public IteratorShort iterator() {
                return new IteratorShort() {

                    int n=0;

                    public boolean hasNext() {
                        return n < 10;
                    }

                    public short nextShort() {
                        n++;
                        return (short) 1;
                    }
                };
            }

            public int size() {
                return 10;
            }
        };
        testCollection(coll);
    }

    @Test
    public void testCollectionByte() {
        CollectionByte coll = new CollectionByte() {

            public IteratorByte iterator() {
                return new IteratorByte() {

                    int n=0;

                    public boolean hasNext() {
                        return n < 10;
                    }

                    public byte nextByte() {
                        n++;
                        return (byte) 1;
                    }
                };
            }

            public int size() {
                return 10;
            }
        };
        testCollection(coll);
    }

    public static void testCollection(CollectionNumber coll) {
        assertEquals(10, coll.size());
        IteratorNumber iter = coll.iterator();
        while (iter.hasNext()) {
            assertEquals(1.0, iter.nextDouble(), 0.0001);
        }
        iter = coll.iterator();
        while (iter.hasNext()) {
            assertEquals((float) 1.0, iter.nextFloat(), 0.0001);
        }
        iter = coll.iterator();
        while (iter.hasNext()) {
            assertEquals(1L, iter.nextLong());
        }
        iter = coll.iterator();
        while (iter.hasNext()) {
            assertEquals(1, iter.nextInt());
        }
        iter = coll.iterator();
        while (iter.hasNext()) {
            assertEquals((short) 1, iter.nextShort());
        }
        iter = coll.iterator();
        while (iter.hasNext()) {
            assertEquals((byte) 1, iter.nextByte());
        }
    }
}
