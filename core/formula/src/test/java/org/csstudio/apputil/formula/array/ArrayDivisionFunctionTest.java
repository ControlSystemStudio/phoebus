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
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ArrayDivisionFunctionTest {

    @Test
    public void compute() throws Exception {
        ArrayDivisionFunction arrayDivisionFunction =
                new ArrayDivisionFunction();

        assertEquals("arrayDiv", arrayDivisionFunction.getName());
        assertEquals("array", arrayDivisionFunction.getCategory());

        VType array1 = VNumberArray.of(ArrayDouble.of(2.0, 10.0, 30.0),
                Alarm.none(), Time.now(), Display.none());
        VType array2 = VNumberArray.of(ArrayDouble.of(1.0, 2.0, 5.0),
                Alarm.none(), Time.now(), Display.none());

        VNumberArray result = (VNumberArray) arrayDivisionFunction.compute(array1, array2);

        assertEquals(3, result.getData().size());
        assertEquals(2, result.getData().getInt(0));
        assertEquals(5, result.getData().getInt(1));
        assertEquals(6, result.getData().getInt(2));

        VType exponent = VDouble.of(2.0, Alarm.none(), Time.now(), Display.none());

        result = (VNumberArray) arrayDivisionFunction.compute(array1, exponent);
        assertEquals(Double.NaN, Double.valueOf(result.getData().getDouble(0)));
    }

    @Test
    public void testWrongArguments() {
        ArrayDivisionFunction arrayDivisionFunction =
                new ArrayDivisionFunction();

        VType array1 = VNumberArray.of(ArrayDouble.of(1.0, 2.0),
                Alarm.none(), Time.now(), Display.none());
        VType array2 = VNumberArray.of(ArrayDouble.of(2.0, 5.0, 7.0),
                Alarm.none(), Time.now(), Display.none());

        assertThrows(Exception.class,
                () -> arrayDivisionFunction.compute(array1, array2));
    }
}