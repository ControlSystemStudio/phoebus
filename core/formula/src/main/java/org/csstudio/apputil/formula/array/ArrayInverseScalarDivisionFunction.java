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
 * Computes element at index i in the output array by dividing the constant nominator by
 * the element at index i of the input array.
 * For the inverse operation, see {@link ArrayScalarDivisionFunction}
 */
public class ArrayInverseScalarDivisionFunction extends BaseArrayFunction{

    @Override
    public String getName() {
        return "arrayDivScalarInv";
    }

    @Override
    public String getDescription() {
        return "Result[x] = nominator / array[x].";
    }

    @Override
    public List<String> getArguments() {
        return List.of("nominator", "array");
    }

    /**
     *
     * @param args Arguments, count will match <code>getArgumentCount()</code>
     * @return An array where each element is calculated by dividing the the first argument
     * by the element at same index in the second argument. If the input arguments are of wrong type,
     * {@link BaseArrayFunction#DEFAULT_NAN_DOUBLE_ARRAY} is returned.
     */
    @Override
    public VType compute(VType... args) {
        if(args[0] instanceof VNumber && VTypeHelper.isNumericArray(args[1])){
            VNumberArray array = (VNumberArray)args[1];
            VNumber factor = (VNumber) args[0];

            return VNumberArray.of(
                    ListMath.inverseRescale(array.getData(), factor.getValue().doubleValue(), 0),
                    Alarm.none(),
                    Time.now(),
                    Display.none());
        }
        else{
            return DEFAULT_NAN_DOUBLE_ARRAY;
        }
    }
}
