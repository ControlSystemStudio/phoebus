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
import org.epics.vtype.VType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArrayPowFunctionTest {

    @Test
    public void compute() {
        ArrayPowFunction arrayPowFunction = new ArrayPowFunction();

        assertEquals("arrayPow", arrayPowFunction.getName());
        assertEquals("array", arrayPowFunction.getCategory());

        VType exponent = VDouble.of(2.0, Alarm.none(), Time.now(), Display.none());
        VType array = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 3.0), Alarm.none(), Time.now(), Display.none());

        VNumberArray result = (VNumberArray)arrayPowFunction.compute(array, exponent);
        assertEquals(3, result.getData().size());
        assertEquals(1, result.getData().getDouble(0));
        assertEquals(4, result.getData().getDouble(1));
        assertEquals(9, result.getData().getDouble(2));

        result = (VNumberArray)arrayPowFunction.compute(exponent, exponent);
        assertEquals(Double.NaN, Double.valueOf(result.getData().getDouble(0)));

        result = (VNumberArray)arrayPowFunction.compute(exponent, array);
        assertEquals(Double.NaN, Double.valueOf(result.getData().getDouble(0)));
    }
}