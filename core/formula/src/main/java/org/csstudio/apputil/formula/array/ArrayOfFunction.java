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

import org.epics.util.array.ListDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Constructs an array from the list of input arguments.
 */
public class ArrayOfFunction extends BaseArrayFunction{

    @Override
    public String getName() {
        return "arrayOf";
    }

    @Override
    public String getDescription() {
        return "Constructs an array of strings or numbers.";
    }

    @Override
    public List<String> getArguments() {
        return List.of("<String | Number>...");
    }

    @Override
    public boolean isVarArgs() {
        return true;
    }

    /**
     * @param args Arguments, count will match <code>getArgumentCount()</code>
     * @return A {@link VStringArray} or {@link VNumberArray}, depending on the input type. All elements of
     * the argument list must be either {@link VString} or {@link VNumber}, otherwise
     * {@link BaseArrayFunction#DEFAULT_NAN_DOUBLE_ARRAY} is returned.
     */
    @Override
    public VType compute(VType... args) {
        if(args[0] instanceof VString){ // Just checking first argument
            List<String> data = new ArrayList<>();
            for (Object arg : args) {
                if(arg == null){
                    data.add(null);
                }
                else {
                    data.add(((VString)arg).getValue());
                }
            }
            return VStringArray.of(data, Alarm.none(), Time.now());
        }
        else if(args[0] instanceof VNumber){
            List<VNumber> elements =
                    Arrays.asList(args).stream().map(arg -> (VNumber)arg).collect(Collectors.toList());
            ListDouble data = new ListDouble() {
                @Override
                public double getDouble(int index) {
                    VNumber number = elements.get(index);
                    if (number == null || number.getValue() == null)
                        return Double.NaN;
                    else
                        return number.getValue().doubleValue();
                }

                @Override
                public int size() {
                    return elements.size();
                }
            };
            return VNumberArray.of(data, Alarm.none(), Time.now(), Display.none());
        }
        else{
            return DEFAULT_NAN_DOUBLE_ARRAY;
        }
    }
}
