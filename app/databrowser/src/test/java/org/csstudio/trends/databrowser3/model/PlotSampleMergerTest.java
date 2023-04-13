/*
 * Copyright (C) 2020 European Spallation Source ERIC.
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

import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VInt;
import org.epics.vtype.VType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class PlotSampleMergerTest {

    private Instant i980 = Instant.ofEpochSecond(980L);
    private Instant i990 = Instant.ofEpochSecond(990L);
    private Instant i1000 = Instant.ofEpochSecond(1000L);
    private Instant i1100= Instant.ofEpochSecond(1100L);
    private Instant i1200= Instant.ofEpochSecond(1200L);

    private VType v980 = VInt.of(980, Alarm.none(), Time.of(i980), Display.none());
    private VType v990= VInt.of(990, Alarm.none(), Time.of(i990), Display.none());
    private VType v1000 = VInt.of(1000, Alarm.none(), Time.of(i1000), Display.none());
    private VType v1100 = VInt.of(1100, Alarm.none(), Time.of(i1100), Display.none());
    private VType v1200 = VInt.of(1200, Alarm.none(), Time.of(i1200), Display.none());

    private PlotSample ps980 = new PlotSample("source", v980);
    private PlotSample ps990 = new PlotSample("source", v990);
    private PlotSample ps1000 = new PlotSample("source", v1000);
    private PlotSample ps1100 = new PlotSample("source", v1100);
    private PlotSample ps1200 = new PlotSample("source", v1200);

    @Test
    public void testMerge1(){
        PlotSample[] merged = PlotSampleMerger.merge(new PlotSample[]{ps980, ps990, ps1000}, new PlotSample[]{ps1100, ps1200});
        assertEquals(5, merged.length);
    }

    @Test
    public void testMerge2(){
        PlotSample[] merged = PlotSampleMerger.merge(new PlotSample[]{ps1100, ps1200}, new PlotSample[]{ps980, ps990, ps1000});
        assertEquals(5, merged.length);
    }

    @Test
    public void testMerge3(){
        PlotSample[] merged = PlotSampleMerger.merge(new PlotSample[]{ps990, ps1000, ps1100}, new PlotSample[]{ps980, ps1200});
        assertEquals(2, merged.length);
    }

    @Test
    public void testMerge4(){
        PlotSample[] merged = PlotSampleMerger.merge(new PlotSample[]{ps990, ps1000, ps1200}, new PlotSample[]{ps980, ps1100});
        assertEquals(3, merged.length);
        assertEquals(980, ((VInt)merged[0].getVType()).getValue());
        assertEquals(1100, ((VInt)merged[1].getVType()).getValue());
        assertEquals(1200, ((VInt)merged[2].getVType()).getValue());
    }

    @Test
    public void testMergeWithDuplicate(){
        VType v1000duplicate = VInt.of(990, Alarm.none(), Time.of(i1000), Display.none());
        PlotSample ps1000Duplicate = new PlotSample("source", v1000duplicate);
        PlotSample[] merged = PlotSampleMerger.merge(new PlotSample[]{ps980, ps990, ps1000, ps1100, ps1200}, new PlotSample[]{ps1000Duplicate});
        assertEquals(5, merged.length);
        assertEquals(990, ((VInt)merged[2].getVType()).getValue());

    }

    @Test
    public void testNulls(){
        PlotSample[] merged = PlotSampleMerger.merge(new PlotSample[]{ps980}, null);
        assertEquals(1, merged.length);
        assertEquals(980, ((VInt)merged[0].getVType()).getValue());

        merged = PlotSampleMerger.merge(null, new PlotSample[]{ps980});
        assertEquals(1, merged.length);
        assertEquals(980, ((VInt)merged[0].getVType()).getValue());
    }
}
