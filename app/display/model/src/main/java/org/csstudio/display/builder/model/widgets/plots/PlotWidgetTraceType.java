/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.model.widgets.plots;

import org.csstudio.display.builder.model.Messages;

/** How a trace is drawn
 *  @author Kay Kasemir
 */
public enum PlotWidgetTraceType
{
    /** No trace */
    NONE(Messages.TraceType_None),
    /** Line between points */
    LINE(Messages.TraceType_Line),
    /** 'Stair steps' between points */
    STEP(Messages.TraceType_Step),
    /** Error bar */
    ERRORBAR(Messages.TraceType_Errorbar),
    /** Line with error bar */
    LINE_ERRORBAR(Messages.TraceType_LineErrorbar),
    /** Bar graph */
    BARS(Messages.TraceType_Bars);

    final private String name;

    private PlotWidgetTraceType(final String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
