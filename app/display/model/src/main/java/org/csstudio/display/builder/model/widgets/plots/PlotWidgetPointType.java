/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.model.widgets.plots;

import org.csstudio.display.builder.model.Messages;

/** How points of a trace are drawn
 *  @author Kay Kasemir
 */
public enum PlotWidgetPointType
{
    // Items match org.csstudio.javafx.rtplot.PointType.
    // They are duplicated here to separate model and representation.
    /** No points */
    NONE(Messages.PointType_None),
    /** Squares */
    SQUARES(Messages.PointType_Squares),
    /** Circles */
    CIRCLES(Messages.PointType_Circles),
    /** Diamonds */
    DIAMONDS(Messages.PointType_Diamonds),
    /** 'X' markers */
    XMARKS(Messages.PointType_X),
    /** Triangles */
    TRIANGLES(Messages.PointType_Triangles);

    final private String name;

    private PlotWidgetPointType(final String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
