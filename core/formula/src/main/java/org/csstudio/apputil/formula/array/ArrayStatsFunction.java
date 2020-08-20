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
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Returns the statistic of the given array
 * average, min, max, element count
 */
public class ArrayStatsFunction extends BaseArrayFunction {

    @Override
    public String getName()
    {
        return "arrayStats";
    }

    @Override
    public String getDescription()
    {
        return "Returns the statistics of the given array";
    }

    @Override
    public List<String> getArguments()
    {
        return List.of("array");
    }

    @Override
    public VType compute(VType... args)
    {
        if (VTypeHelper.isNumericArray(args[0]))
        {
            DoubleSummaryStatistics stats = Arrays.stream(VTypeHelper.toDoubles(args[0])).summaryStatistics();
            return VStatistics.of(stats.getAverage(),
                    Double.NaN,
                    stats.getMin(),
                    stats.getMax(),
                    (int) stats.getCount(),
                    Alarm.none(),
                    Time.now(),
                    Display.none());
        }
        else
        {
            return DEFAULT_NAN_DOUBLE_ARRAY;
        }
    }
}
