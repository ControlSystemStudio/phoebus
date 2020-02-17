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

import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VInt;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.junit.Test;

import static org.junit.Assert.*;

public class ArrayOfFunctionTest {

    @Test
    public void compute() {

        ArrayOfFunction arrayOfFunction = new ArrayOfFunction();

        assertEquals("arrayOf", arrayOfFunction.getName());
        assertEquals("array", arrayOfFunction.getCategory());

        VString a = VString.of("a", Alarm.none(), Time.now());
        VString b = VString.of("b", Alarm.none(), Time.now());
        VString c = VString.of("c", Alarm.none(), Time.now());

        VStringArray vStringArray = (VStringArray)arrayOfFunction.compute(a, b, c);

        assertEquals(3, vStringArray.getData().size());
        assertEquals("a", vStringArray.getData().get(0));
        assertEquals("b", vStringArray.getData().get(1));
        assertEquals("c", vStringArray.getData().get(2));

        VInt d0 = VInt.of(1, Alarm.none(), Time.now(), Display.none());
        VInt d1 = VInt.of(2, Alarm.none(), Time.now(), Display.none());
        VInt d2 = VInt.of(3, Alarm.none(), Time.now(), Display.none());

        VNumberArray vNumberArray = (VNumberArray)arrayOfFunction.compute(d0, d1, d2);
        assertEquals(3, vNumberArray.getData().size());
        assertEquals(1, vNumberArray.getData().getInt(0));
        assertEquals(2, vNumberArray.getData().getInt(1));
        assertEquals(3, vNumberArray.getData().getInt(2));

        VEnum vEnum = VEnum.of(0, EnumDisplay.of(), Alarm.none(), Time.now());

        vNumberArray = (VNumberArray)arrayOfFunction.compute(vEnum);
        assertTrue(Double.valueOf(vNumberArray.getData().getDouble(0)).equals(Double.NaN));
    }
}