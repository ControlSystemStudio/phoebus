/*
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.csstudio.apputil.formula.array;

import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayInteger;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VNumberArray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArrayMinFunctionTest {

    @Test
    public void compute() {

        ArrayMinFunction arrayMinFunction = new ArrayMinFunction();

        assertEquals("arrayMin", arrayMinFunction.getName());
        assertEquals("array", arrayMinFunction.getCategory());

        // double array
        VNumberArray doubleArray = VNumberArray.of(ArrayDouble.of(-1.0, 0, 1.0, 2.0, 3.0, 4.0, 5), Alarm.none(), Time.now(), Display.none());

        VDouble min = (VDouble) arrayMinFunction.compute(doubleArray);
        assertEquals("arrayMin Failed to calculate min for double array", Double.valueOf(-1.0), min.getValue());


        // int array
        VNumberArray intArray = VNumberArray.of(ArrayInteger.of(-1, 0, 1, 2, 3, 4, 5), Alarm.none(), Time.now(), Display.none());
        min = (VDouble) arrayMinFunction.compute(intArray);
        assertEquals("arrayMin Failed to calculate min for int array", Double.valueOf(-1), min.getValue());

    }
}