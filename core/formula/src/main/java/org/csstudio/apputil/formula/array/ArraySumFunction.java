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

import org.epics.util.array.ListMath;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.List;

/**
 * Adds an offset to each element in an array. Offset may of
 * course be negative.
 */
public class ArraySumFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "arraySum";
    }

    @Override
    public String getDescription() {
        return "Result[x] = array[x] + offset.";
    }

    @Override
    public List<String> getArguments() {
        return List.of("array", "offset");
    }

    @Override
    public VType compute(VType... args)  {
        if(VTypeHelper.isNumericArray(args[0]) && args[1] instanceof VNumber){
            VNumberArray array = (VNumberArray)args[0];
            VNumber offset = (VNumber)args[1];
            return VNumberArray.of(
                    ListMath.rescale(array.getData(), 1, offset.getValue().doubleValue()),
                    Alarm.none(),
                    Time.now(),
                    Display.none());
        }
        else{
            return DEFAULT_NAN_DOUBLE_ARRAY;
        }
    }
}
