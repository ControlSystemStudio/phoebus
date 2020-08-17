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
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;

import java.util.List;

/**
 * Computes an output array where each element is the product
 * of elements in the input arrays, which must be of equal length.
 */
public class ArrayMultiplicationFunction extends BaseArrayFunction{

    @Override
    public String getName() {
        return "arrayMult";
    }

    @Override
    public String getDescription() {
        return "Result[x] = array1[x], array2[x].";
    }

    @Override
    public List<String> getArguments() {
        return List.of("array", "array");
    }

    /**
     *
     * @param args Arguments, count will match <code>getArgumentCount()</code>
     * @return An array where each element is the product of the elements
     * in the input arrays, which must be of equal length. If the input arrays
     * are not numerical, {@link BaseArrayFunction#DEFAULT_NAN_DOUBLE_ARRAY} is
     * returned.
     * @throws Exception If the input arrays are not of equal length.
     */
    @Override
    public VType compute(VType... args) throws Exception {
        if(args[0] instanceof VNumberArray && args[1] instanceof VNumberArray){
            VNumberArray array1 = (VNumberArray)args[0];
            VNumberArray array2 = (VNumberArray)args[1];
            if(array1.getData().size() != array2.getData().size()){
                throw new Exception(String.format("Function %s cannot compute as specified arrays are of different length",
                        getName()));
            }
            return VNumberArray.of(
                    ListMath.multiply(array1.getData(), array2.getData()),
                    Alarm.none(),
                    Time.now(),
                    Display.none());
        }
        else{
            return DEFAULT_NAN_DOUBLE_ARRAY;
        }
    }
}
