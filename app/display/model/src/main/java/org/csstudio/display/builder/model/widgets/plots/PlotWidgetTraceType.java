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
    NONE(Messages.TraceType_None),
    LINE(Messages.TraceType_Line),
    STEP(Messages.TraceType_Step),
    ERRORBAR(Messages.TraceType_Errorbar),
    LINE_ERRORBAR(Messages.TraceType_LineErrorbar),
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
