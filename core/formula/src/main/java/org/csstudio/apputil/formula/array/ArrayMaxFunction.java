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
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Returns the greatest value of the given array
 */
public class ArrayMaxFunction extends BaseArrayFunction {

    @Override
    public String getName()
    {
        return "arrayMax";
    }

    @Override
    public String getDescription()
    {
        return "Returns the greatest of the given array";
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
            return VDouble.of(Arrays.stream(VTypeHelper.toDoubles(args[0])).summaryStatistics().getMax(),
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
