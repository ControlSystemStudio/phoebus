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
 * Computes scaled elements of an input array, with optinal offset.
 */
public class ScaleArrayFormulaFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "scale";
    }

    @Override
    public String getDescription() {
        return "Rescale an array using the factor and optionally an offset";
    }

    @Override
    public List<String> getArguments() {
        return List.of("array", "factor", "[offset]");
    }

    @Override
    public boolean isVarArgs() {
        return true;
    }

    /**
     *
     * @param args Arguments, count will match <code>getArgumentCount()</code>
     * @return A {@link VNumberArray} where each element is multiplied by the
     * factor specified as the second argument. Optionally the result is offset by
     * the third argument, which may be positive or negative. If the input array is
     * not numerical, {@link BaseArrayFunction#DEFAULT_NAN_DOUBLE_ARRAY} is returned.
     * @throws Exception
     */
    @Override
    public VType compute(VType... args) throws Exception{
        if(args.length != 2 && args.length != 3){
            throw new Exception(String.format("Function %s takes 2 or 3 aruments, got %d", getName(), args.length));
        }
        if(VTypeHelper.isNumericArray(args[0])){
            VNumberArray array = (VNumberArray)args[0];
            VNumber factor = (VNumber) args[1];
            double offset = args.length == 3 ? ((VNumber)args[2]).getValue().doubleValue() : 0.0;

            return VNumberArray.of(
                    ListMath.rescale(array.getData(),
                            factor.getValue().doubleValue(),
                            offset),
                    Alarm.none(),
                    Time.now(),
                    Display.none());
        }
        else{
            return DEFAULT_NAN_DOUBLE_ARRAY;
        }
    }
}
