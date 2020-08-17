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
 * Computes Math.pow(base, exponent) on each element in the specified array.
 */
public class ArrayPowFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "arrayPow";
    }

    @Override
    public String getDescription() {
        return "Result[x] = pow(array[x], exponent).";
    }

    @Override
    public List<String> getArguments() {
        return List.of("array", "exponent");
    }

    /**
     *
     * @param args Arguments, count will match <code>getArgumentCount()</code>
     * @return A {@link VNumberArray} or {@link BaseArrayFunction#DEFAULT_NAN_DOUBLE_ARRAY} if
     * the input arguments are of wrong type.
     */
    @Override
    public VType compute(VType... args)  {
        if(VTypeHelper.isNumericArray(args[0]) && args[1] instanceof VNumber){
            VNumberArray array = (VNumberArray)args[0];
            VNumber exponent = (VNumber)args[1];
            return VNumberArray.of(
                    ListMath.pow(array.getData(), exponent.getValue().doubleValue()),
                    Alarm.none(),
                    Time.now(),
                    Display.none());
        }
        else{
            return DEFAULT_NAN_DOUBLE_ARRAY;
        }
    }
}
