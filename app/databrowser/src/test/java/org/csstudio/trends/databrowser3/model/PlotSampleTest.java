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

package org.csstudio.trends.databrowser3.model;

import org.epics.util.array.ArrayDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PlotSampleTest {

    @Test
    public void getValue() {

        VDouble vDouble = VDouble.of(7.7, Alarm.none(), Time.now(), Display.none());
        PlotSample plotSample = new PlotSample(new AtomicInteger(0), "source", vDouble, "info");

        double result = plotSample.getValue();
        assertEquals(7.7, result, 0);

        VDoubleArray vDoubleArray = VDoubleArray.of(ArrayDouble.of(7.7, 8.8, 9.9), Alarm.none(), Time.now(), Display.none());
        plotSample = new PlotSample(new AtomicInteger(1), "source", vDoubleArray, "info");
        result = plotSample.getValue();
        assertEquals(8.8, result, 0);
    }
}