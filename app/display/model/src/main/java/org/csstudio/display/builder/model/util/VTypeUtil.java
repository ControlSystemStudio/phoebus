/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;

import org.epics.util.array.ListNumber;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.ui.vtype.FormatOption;
import org.phoebus.ui.vtype.FormatOptionHandler;

/** Utility for displaying VType data.
 *  @author Kay Kasemir
 */
public class VTypeUtil
{
    /** Format a value as text.
     *
     *  <p>Byte arrays are treated as long strings.
     *
     *  @param value VType
     *  @param with_units Add units?
     *  @return Text for value (without timestamp, alarm, ..)
     */
    public static String getValueString(final VType value, final boolean with_units)
    {
        if (value instanceof VByteArray)
            return FormatOptionHandler.format(value, FormatOption.STRING, -1, false);
        return FormatOptionHandler.format(value, FormatOption.DEFAULT, -1, with_units);
    }

    /** Obtain numeric value
     *  @param value VType
     *  @return Number for value.
     *          <code>Double.NaN</code> in case the value type
     *          does not decode into a number.
     */
    public static Number getValueNumber(final VType value)
    {
        if (value instanceof VNumber)
        {
            final VNumber cast = (VNumber) value;
            return cast.getValue();
        }
        if (value instanceof VEnum)
            return ((VEnum)value).getIndex();
        if (value instanceof VBoolean)
            return ((VBoolean)value).getValue() ? 1 : 0;
        // For arrays, return first element
        if (value instanceof VNumberArray)
        {
            final ListNumber array = ((VNumberArray)value).getData();
            if (array.size() > 0)
                return array.getDouble(0);
        }
        if (value instanceof VEnumArray)
        {
            final ListNumber array = ((VEnumArray)value).getIndexes();
            if (array.size() > 0)
                return array.getInt(0);
        }
        return Double.valueOf(Double.NaN);
    }
}
