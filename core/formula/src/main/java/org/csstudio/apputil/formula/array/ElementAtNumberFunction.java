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
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.List;

/**
 * Selects an element from a {@link VNumberArray} or {@link VStringArray} array.
 */
public class ElementAtNumberFunction extends BaseArrayFunction {
    @Override

    public String getName() {
        return "elementAt";
    }

    @Override
    public String getDescription() {
        return "Returns the element at the specified position of a numeric or string array.";
    }

    @Override
    public List<String> getArguments() {
        return List.of("<String | Number> array", "index");
    }

    /**
     * Selects the specified array element.
     * @param args Arguments, count will match <code>getArgumentCount()</code>
     * @return A {@link VNumber} or {@link VString} depending on the input array. If the
     * specified arguments are not of the supported types, {@link BaseArrayFunction#DEFAULT_NAN_DOUBLE} or
     * {@link BaseArrayFunction#DEFAULT_EMPTY_STRING} is returned.
     * @throws Exception if the index is invalid.
     */
    @Override
    public VType compute(VType... args) throws Exception {
        boolean isStringArray = args[0] instanceof VStringArray;
        if(VTypeHelper.isNumericArray(args[0]) && args[1] instanceof VNumber){
            VNumberArray numberArray = (VNumberArray)args[0];
            int index = ((VNumber)args[1]).getValue().intValue();
            if(index < 0 || index > numberArray.getData().size() - 1){
                throw new Exception(String.format("Array index %d invalid", index));
            }
            return VDouble.of(numberArray.getData().getDouble(index),
                    Alarm.none(),
                    Time.now(),
                    Display.none());
        }
        else if(isStringArray && args[1] instanceof VNumber){
            VStringArray stringArray = (VStringArray)args[0];
            int index = ((VDouble)args[1]).getValue().intValue();
            if(index < 0 || index > stringArray.getData().size() - 1){
                throw new Exception(String.format("Array index %d invalid", index));
            }
            return VString.of(stringArray.getData().get(index),
                    stringArray.getAlarm(),
                    stringArray.getTime());
        }
        else{
            return isStringArray ? DEFAULT_EMPTY_STRING : DEFAULT_NAN_DOUBLE;
        }
    }
}
