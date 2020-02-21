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
import org.epics.vtype.*;

import java.util.Arrays;
import java.util.List;

public class RescaleArrayFormulaFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "rescale";
    }

    @Override
    public String getDescription() {
        return "Rescale an array using the factor and offset ";
    }

    @Override
    public List<String> getArguments() {
        return List.of("VNumberArray", "VNumber", "VNumber");
    }

    @Override
    public VType compute(VType... args) throws Exception {
        for(VType arg : args){
            if(arg == null){
                throw new Exception(String.format("Function %s encountered a null arugment", getName()));
            }
        }

        VNumberArray arg1 = (VNumberArray) args[0];
        VNumber arg2 = (VNumber) args[1];
        VNumber arg3 = (VNumber) args[2];

        return VNumberArray.of(
                ListMath.rescale(arg1.getData(), arg2.getValue().doubleValue(), arg3.getValue().doubleValue()),
                Alarm.highestAlarmOf(Arrays.asList(args), false),
                Time.now(),
                Display.none());
    }
}
