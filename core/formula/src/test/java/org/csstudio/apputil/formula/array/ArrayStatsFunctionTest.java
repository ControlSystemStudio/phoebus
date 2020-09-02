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
import org.epics.vtype.VStatistics;
import org.junit.Test;

import java.text.DecimalFormat;

import static org.junit.Assert.*;

public class ArrayStatsFunctionTest {

    @Test
    public void compute() {

        ArrayStatsFunction arrayStatsFunction = new ArrayStatsFunction();

        assertEquals("arrayStats", arrayStatsFunction.getName());
        assertEquals("array", arrayStatsFunction.getCategory());

        VNumberArray numberArray = VNumberArray.of(ArrayDouble.of(1d, 2d), Alarm.none(), Time.now(), Display.none());

        VStatistics stats = (VStatistics) arrayStatsFunction.compute(numberArray);

        assertEquals("arrayStats Failed to calculate min", Double.valueOf(1), stats.getMin());
        assertEquals("arrayStats Failed to calculate max", Double.valueOf(2), stats.getMax());

    }
}