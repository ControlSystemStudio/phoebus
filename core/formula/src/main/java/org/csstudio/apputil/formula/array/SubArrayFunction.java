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
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import java.util.List;

/**
 * Constructs a sub-array of the input array and the specified
 * from-index and to-index.
 */
public class SubArrayFunction extends BaseArrayFunction {

    @Override
    public String getName() {
        return "subArray";
    }

    @Override
    public String getDescription() {
        return "Result[] = array[fromIndex], ..., array[toIndex - 1]";
    }

    @Override
    public List<String> getArguments() {
        return List.of("<String | Number> array", "fromIndex", "toIndex");
    }

    /**
     *
     * @param args Arguments, count will match <code>getArgumentCount()</code>
     * @return An array containing elements from the index defined by arg[1] to the index
     * defined by arg[2] - 1. If the input arguments are of wrong type,
     * {@link BaseArrayFunction#DEFAULT_NAN_DOUBLE_ARRAY} is returned.
     * @throws Exception If the specified from-index and/or to-index is invalid.
     */
    @Override
    public VType compute(VType... args) throws Exception {

        VNumber fromIndex = (VNumber) args[1];
        VNumber toIndex = (VNumber) args[2];

        if(VTypeHelper.isNumericArray(args[0])){
            VNumberArray array = (VNumberArray)args[0];
            if(fromIndex.getValue().intValue() < 0 ||
                    (fromIndex.getValue().intValue() > toIndex.getValue().intValue()) ||
                    (toIndex.getValue().intValue() - fromIndex.getValue().intValue() > array.getData().size())){
                throw new Exception("Limits for sub array invalid");
            }
            return VNumberArray.of(
                    array.getData().subList(fromIndex.getValue().intValue(), toIndex.getValue().intValue()),
                    Alarm.none(),
                    Time.now(),
                    Display.none());
        }
        else  if(args[0] instanceof VStringArray){
            VStringArray array = (VStringArray)args[0];
            if(fromIndex.getValue().intValue() < 0 ||
                    (fromIndex.getValue().intValue() > toIndex.getValue().intValue()) ||
                    (toIndex.getValue().intValue() - fromIndex.getValue().intValue() > array.getData().size())){
                throw new Exception("Limits for sub array invalid");
            }
            return VStringArray.of(
                    array.getData().subList(fromIndex.getValue().intValue(), toIndex.getValue().intValue()),
                    Alarm.none(),
                    Time.now());
        }
        else{
            return DEFAULT_NAN_DOUBLE_ARRAY;
        }
    }
}
