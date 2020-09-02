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
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SubArrayFunctionTest {

    @Test
    public void compute() throws Exception{
        SubArrayFunction subArrayFunction = new SubArrayFunction();

        assertEquals("subArray", subArrayFunction.getName());
        assertEquals("array", subArrayFunction.getCategory());

        VType array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0, 4.0, 5.0), Alarm.none(), Time.now(), Display.none());
        VType from = VDouble.of(1.0, Alarm.none(), Time.now(), Display.none());
        VType to = VDouble.of(3.0, Alarm.none(), Time.now(), Display.none());

        VNumberArray result = (VNumberArray)subArrayFunction.compute(array, from, to);

        assertEquals(2, result.getData().size());
        assertEquals(2, result.getData().getInt(0));
        assertEquals(3, result.getData().getInt(1));

        result = (VNumberArray)subArrayFunction.compute(from, from, to);
        assertTrue(Double.valueOf(result.getData().getDouble(0)).equals(Double.NaN));

        VStringArray stringArray = VStringArray.of(List.of("a", "b", "c", "d", "e"));

        VStringArray stringResult = (VStringArray) subArrayFunction.compute(stringArray, from, to);
        assertEquals(2, stringResult.getData().size());
        assertEquals("b", stringResult.getData().get(0));
        assertEquals("c", stringResult.getData().get(1));

    }

    @Test(expected = Exception.class)
    public void invalidArguments1() throws Exception{
        SubArrayFunction subArrayFunction = new SubArrayFunction();

        assertEquals("subArray", subArrayFunction.getName());

        VType array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0, 4.0, 5.0), Alarm.none(), Time.now(), Display.none());
        VType from = VDouble.of(-1.0, Alarm.none(), Time.now(), Display.none());
        VType to = VDouble.of(2.0, Alarm.none(), Time.now(), Display.none());

        subArrayFunction.compute(array, from, to);
    }

    @Test(expected = Exception.class)
    public void invalidArguments2() throws Exception{
        SubArrayFunction subArrayFunction = new SubArrayFunction();

        assertEquals("subArray", subArrayFunction.getName());

        VType array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0, 4.0, 5.0), Alarm.none(), Time.now(), Display.none());
        VType from = VDouble.of(3.0, Alarm.none(), Time.now(), Display.none());
        VType to = VDouble.of(2.0, Alarm.none(), Time.now(), Display.none());

        subArrayFunction.compute(array, from, to);
    }

    @Test(expected = Exception.class)
    public void invalidArguments3() throws Exception{
        SubArrayFunction subArrayFunction = new SubArrayFunction();

        assertEquals("subArray", subArrayFunction.getName());

        VType array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0, 4.0, 5.0), Alarm.none(), Time.now(), Display.none());
        VType from = VDouble.of(3.0, Alarm.none(), Time.now(), Display.none());
        VType to = VDouble.of(9.0, Alarm.none(), Time.now(), Display.none());

        subArrayFunction.compute(array, from, to);
    }
}