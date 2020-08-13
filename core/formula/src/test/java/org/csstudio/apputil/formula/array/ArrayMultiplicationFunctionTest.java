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
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.junit.Test;

import static org.junit.Assert.*;

public class ArrayMultiplicationFunctionTest {

    @Test
    public void compute() throws Exception{
        ArrayMultiplicationFunction arrayMultiplicationFunction =
                new ArrayMultiplicationFunction();

        assertEquals("arrayMult", arrayMultiplicationFunction.getName());
        assertEquals("array", arrayMultiplicationFunction.getCategory());

        VType array1 = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0),
                Alarm.of(AlarmSeverity.MAJOR, AlarmStatus.NONE, ""), Time.now(), Display.none());
        VType array2 = VNumberArray.of(ArrayDouble.of(2.0, 5.0, 7.0),
                Alarm.of(AlarmSeverity.MINOR, AlarmStatus.NONE, ""), Time.now(), Display.none());

        VNumberArray result = (VNumberArray)arrayMultiplicationFunction.compute(array1, array2);

        assertEquals(3, result.getData().size());
        assertEquals(2, result.getData().getInt(0));
        assertEquals(10, result.getData().getInt(1));
        assertEquals(21, result.getData().getInt(2));

        VType exponent = VDouble.of(2.0, Alarm.none(), Time.now(), Display.none());

        result = (VNumberArray)arrayMultiplicationFunction.compute(array1, exponent);
        assertTrue(Double.valueOf(result.getData().getDouble(0)).equals(Double.NaN));
    }

    @Test(expected = Exception.class)
    public void testWrongArguments() throws Exception{
        ArrayMultiplicationFunction arrayMultiplicationFunction =
                new ArrayMultiplicationFunction();

        VType array1 = VNumberArray.of(ArrayDouble.of(1.0, 2.0),
                Alarm.of(AlarmSeverity.MAJOR, AlarmStatus.NONE, ""), Time.now(), Display.none());
        VType array2 = VNumberArray.of(ArrayDouble.of(2.0, 5.0, 7.0),
                Alarm.of(AlarmSeverity.MINOR, AlarmStatus.NONE, ""), Time.now(), Display.none());

        arrayMultiplicationFunction.compute(array1, array2);
    }
}