/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;

import org.csstudio.display.builder.model.properties.FormatOption;
import org.phoebus.util.array.ListInt;
import org.phoebus.util.array.ListNumber;
import org.phoebus.vtype.VByteArray;
import org.phoebus.vtype.VEnum;
import org.phoebus.vtype.VEnumArray;
import org.phoebus.vtype.VNumber;
import org.phoebus.vtype.VNumberArray;
import org.phoebus.vtype.VType;

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
        // For arrays, return first element
        if (value instanceof VNumberArray)
        {
            final ListNumber array = ((VNumberArray)value).getData();
            if (array.size() > 0)
                return array.getDouble(0);
        }
        if (value instanceof VEnumArray)
        {
            final ListInt array = ((VEnumArray)value).getIndexes();
            if (array.size() > 0)
                return array.getInt(0);
        }
        return Double.valueOf(Double.NaN);
    }
}
