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
import org.epics.util.stats.Range;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VNumberArray;
import org.junit.Test;

import java.text.DecimalFormat;

import static org.junit.Assert.*;

public class ArrayRangeOfFunctionTest {

    @Test
    public void compute() {

        ArrayRangeOfFunction arrayRangeOfFunction = new ArrayRangeOfFunction();

        assertEquals("arrayRangeOf", arrayRangeOfFunction.getName());
        assertEquals("array", arrayRangeOfFunction.getCategory());

        Display display =
                Display.of(Range.of(1d, 10d), Range.of(10d, 20d), Range.of(20d, 30d), Range.of(30d, 40d), "N", new DecimalFormat(""));

        VNumberArray numberArray = VNumberArray.of(ArrayDouble.of(1d, 2d), Alarm.none(), Time.now(), display);
        VNumberArray range = (VNumberArray)arrayRangeOfFunction.compute(numberArray);

        assertEquals(1, range.getData().getInt(0));
        assertEquals(10, range.getData().getInt(1));

    }
}