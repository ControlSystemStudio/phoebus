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

import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.util.array.ArrayDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VString;

/**
 * Abstract base class taking care of returning the category identifier for
 * all sub-classes.
 */
public abstract class BaseArrayFunction implements FormulaFunction {

    protected static VDouble DEFAULT_NAN_DOUBLE = VDouble.of(Double.NaN,
            Alarm.none(),
            Time.now(),
            Display.none());

    protected static VDoubleArray DEFAULT_NAN_DOUBLE_ARRAY = VDoubleArray.of(ArrayDouble.of(Double.NaN),
            Alarm.none(),
            Time.now(),
            Display.none());

    protected static VString DEFAULT_EMPTY_STRING =
            VString.of("", Alarm.none(), Time.now());

    @Override
    public String getCategory() {
        return "array";
    }
}
