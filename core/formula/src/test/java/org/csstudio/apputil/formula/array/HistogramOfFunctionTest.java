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
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class HistogramOfFunctionTest {

    @Test
    public void compute() {

       HistogramOfFunction function =
               new HistogramOfFunction();

        assertEquals("histogramOf", function.getName());
        assertEquals("array", function.getCategory());

        double[] data = new double[1000];
        for(int i = 0; i < 1000; i++){
            data[i] = 1.0 * i;
        }

        VDoubleArray vDoubleArray =
                VDoubleArray.of(ArrayDouble.of(data), Alarm.none(), Time.now(), Display.none());

        VNumberArray vNumberArray = (VNumberArray)function.compute(vDoubleArray);

        assertEquals(100, vNumberArray.getData().size());
        assertEquals(10, vNumberArray.getData().getInt(0));
        assertEquals(10, (int)vNumberArray.getDisplay().getDisplayRange().getMaximum());

        VNumber vNumber = VInt.of(200, Alarm.none(), Time.now(), Display.none());
        vNumberArray = (VNumberArray)function.compute(vDoubleArray, vNumber);

        assertEquals(200, vNumberArray.getData().size());
        assertEquals(5, vNumberArray.getData().getInt(0));
        assertEquals(5, (int)vNumberArray.getDisplay().getDisplayRange().getMaximum());

        vNumberArray = (VNumberArray)function.compute(vNumber);

        assertTrue(Double.valueOf(vNumberArray.getData().getDouble(0)).equals(Double.NaN));
    }
}