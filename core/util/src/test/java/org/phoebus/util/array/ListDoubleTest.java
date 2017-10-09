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
import org.phoebus.util.array.ListDouble;
import org.phoebus.util.array.ListNumber;
import static org.phoebus.util.array.CollectionTest.testCollection;
import static org.phoebus.util.array.ListTest.testList;

/**
 *
 * @author carcassi
 */
public class ListDoubleTest {

    public ListDoubleTest() {
    }

    @Test
    public void testListDouble() {
        ListDouble coll = new ListDouble() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public double getDouble(int index) {
                return 1.0;
            }
        };
        testCollection(coll);
        testList(coll);
    }

    @Test
    public void equals1() {
        ListDouble coll = new ListDouble() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public double getDouble(int index) {
                return index;
            }
        };
        ListDouble other = new ArrayDouble(new double[] {0,1,2,3,4,5,6,7,8,9});
        assertThat(coll, equalTo(other));
        assertThat(other, equalTo(coll));
    }

    @Test
    public void equals2() {
        ListNumber coll = new ArrayDouble(new double[] {0,1,2,3,4,5,6,7,8,9});
        ListNumber other = new ArrayFloat(new float[] {0,1,2,3,4,5,6,7,8,9});
        assertThat(other, not(equalTo(coll)));
        assertThat(coll, not(equalTo(other)));
    }

    @Test
    public void hashcode1() {
        ListDouble coll = new ListDouble() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public double getDouble(int index) {
                return index;
            }
        };
        ListDouble other = new ArrayDouble(new double[] {0,1,2,3,4,5,6,7,8,9});
        assertThat(coll.hashCode(), equalTo(other.hashCode()));
        assertThat(coll.hashCode(), equalTo(Arrays.hashCode(new double[] {0,1,2,3,4,5,6,7,8,9})));
    }

    @Test
    public void toString1() {
        ListDouble coll = new ListDouble() {

            @Override
            public int size() {
                return 10;
            }

            @Override
            public double getDouble(int index) {
                return index;
            }
        };
        assertThat(coll.toString(), equalTo("[0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]"));
    }

    @Test
    public void testConcatenation1() {
        //straightforward test to check if concatenation seems to work
        ArrayDouble l1 = new ArrayDouble( new double[] {0, 1, 2, 3, 4} );
        ArrayDouble l2 = new ArrayDouble( new double[] {5, 6, 7, 8, 9} );
        ListDouble combined = ListDouble.concatenate( l1 , l2 );
        for ( int i=0 ; i<9 ; i++ ) {
            assertThat( (int) combined.getDouble( i ) , equalTo( i ) );
        }
    }

    @Test
    public void testConcatenation2() {
        //test concatenating lists of just 1 element
        ArrayDouble[] lists = new ArrayDouble[ 10 ];
        for ( int i=0 ; i<lists.length ; i++ ) {
            lists[ i ] = new ArrayDouble( new double[]{ i } );
        }
        ListDouble concatenated = ListDouble.concatenate( lists );
        assertThat( concatenated.toString() , equalTo( "[0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]" ) );
    }

    @Test
    public void testConcatenation3() {
        //test concatenating empty lists
        ArrayDouble[] lists = new ArrayDouble[ 10 ];
        for ( int i=0 ; i<lists.length ; i++ ) {
            lists[ i ] = new ArrayDouble( new double[]{} );
        }
        ListDouble concatenated = ListDouble.concatenate( lists );
        assertThat( concatenated.size() , equalTo( 0 ) );
        try {
            concatenated.getDouble( 0 );
            assertTrue( false );
        }
        catch ( Exception e ) {
            //ok, should throw exception because list is emtpy
        }
    }

    @Test
    public void testConcatenation4() {
        //test concatenating lists of varying sizes
        ArrayDouble l1 = new ArrayDouble( new double[]{ 1 } );
        ArrayDouble l2 = new ArrayDouble( new double[]{1, 2} );
        ArrayDouble l3 = new ArrayDouble( new double[]{1, 2, 3} );
        ArrayDouble l4 = new ArrayDouble( new double[]{1, 2, 3, 4} );
        ArrayDouble l5 = new ArrayDouble( new double[]{1, 2, 3, 4, 5} );

        ListDouble concatenated = ListDouble.concatenate( l1 , l2 , l3 , l4 , l5 );
        double[] expValues = { 1 , 1 , 2 , 1 , 2 , 3 , 1 , 2 , 3 , 4 , 1 , 2 , 3 , 4 , 5 };
        for ( int i=0 ; i<expValues.length ; i++ ) {
            assertThat( expValues[ i ] , equalTo( concatenated.getDouble( i ) ) );
        }
    }
}
