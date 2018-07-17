/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

/** How a {@link Trace} is drawn sample to sample
 *  @author Kay Kasemir
 */
public enum TraceType
{
    /** No line/area between points */
    NONE(Messages.Type_None),

    /** Trace with area for min/max */
    AREA(Messages.TraceType_Area),

    /** Trace with area for min/max, using direct line */
    AREA_DIRECT(Messages.TraceType_AreaDirect),

    /** Lines for value, min/max */
    LINES(Messages.TraceType_Lines),

    /** Lines for value, min/max, using direct line */
    LINES_DIRECT(Messages.TraceType_LinesDirect),

    /** Single line, no min/max */
    SINGLE_LINE(Messages.TraceType_SingleLine),

    /** Single line, no min/max, using direct line */
    SINGLE_LINE_DIRECT(Messages.TraceType_SingleLineDirect),

    /** Error bars, using direct line */
    LINES_ERROR_BARS(Messages.TraceType_ErrorBars),

    /** Error bars */
    ERROR_BARS(Messages.TraceType_LinesErrorBars),

    /** Bars */
    BARS(Messages.TraceType_Bars);

    final private String name;

    private TraceType(final String name)
    {
        this.name = name;
    }

    /** @return Array of display names for all trace types */
    public static String[] getDisplayNames()
    {
        final TraceType types[] = TraceType.values();
        final String names[] = new String[types.length];
        for (int i=0; i<names.length; ++i)
            names[i] = types[i].name;
        return names;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
