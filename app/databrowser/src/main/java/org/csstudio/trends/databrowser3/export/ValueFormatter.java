/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.export;

import org.csstudio.trends.databrowser3.Messages;
import org.epics.vtype.Display;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.archive.vtype.StringVTypeFormat;
import org.phoebus.archive.vtype.Style;
import org.phoebus.archive.vtype.VTypeFormat;
import org.phoebus.archive.vtype.VTypeHelper;

/** Format an IValue as default, decimal, ...
 *
 *  Shows all array elements for double and long data types
 *  _if_ decimal or exponential format is selected
 *
 *  @author Kay Kasemir
 */
public class ValueFormatter
{
    private boolean min_max_column = false;
    private VTypeFormat format = null;

    /** Initialize
     *  @param style Number style to use
     *  @param precision Precision
     */
    public ValueFormatter(final Style style, final int precision)
    {
        format = Style.getFormat(style, precision);
        format.setMaxArray(-1);
    }

    /** @param min_max_column Display min/max info in separate column? */
    public void useMinMaxColumn(final boolean min_max_column)
    {
        this.min_max_column = min_max_column;
    }

    /** @return Text for column headers */
    public String getHeader()
    {
        if (min_max_column)
            return Messages.ValueColumn +
                   Messages.Export_Delimiter + Messages.NegErrColumn +
                   Messages.Export_Delimiter + Messages.PosErrColumn;
        else
            return Messages.ValueColumn;
    }

    /** @return Value formatted into columns */
    public String format(final VType value)
    {
        final VTypeFormat format_for_this_value;
        if (value instanceof VString ||
            value instanceof VStringArray)
            format_for_this_value = new StringVTypeFormat();
        else
        {
            if (Double.isNaN(VTypeHelper.toDouble(value)))
            {
                if (min_max_column)
                    return Messages.Export_NoValueMarker +
                           Messages.Export_Delimiter + Messages.Export_NoValueMarker +
                           Messages.Export_Delimiter + Messages.Export_NoValueMarker;
                else
                    return Messages.Export_NoValueMarker;
            }
            format_for_this_value = format;
        }

        final VStatistics stats = (value instanceof VStatistics) ? (VStatistics) value : null;

        final StringBuilder buf = new StringBuilder();
        if (stats != null)
            // Show only the average, since min/max handled separately
            format_for_this_value.format(stats.getAverage(), Display.displayOf(stats), buf);
        else
            format_for_this_value.format(value, buf);
        // Optional min, max
        if (min_max_column)
        {
            buf.append(Messages.Export_Delimiter);
            if (stats != null)
            {   // Turn min..max into negative & positive error
                buf.append(stats.getAverage() - stats.getMin());
                buf.append(Messages.Export_Delimiter);
                buf.append(stats.getMax() - stats.getAverage());
            }
            else
            {
                buf.append(0);
                buf.append(Messages.Export_Delimiter);
                buf.append(0);
            }
        }
        return buf.toString();
    }

    @Override
    public String toString()
    {
        return format.toString();
    }
}
