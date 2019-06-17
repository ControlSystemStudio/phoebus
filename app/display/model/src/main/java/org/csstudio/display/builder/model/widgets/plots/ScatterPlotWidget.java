/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets.plots;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;

/** Widget that displays X/Y scalars by filling a circular buffer
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScatterPlotWidget extends XYPlotWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("scatterplot", WidgetCategory.PLOT,
            Messages.ScatterPlot_Name,
            "/icons/xyplot.png",
            Messages.ScatterPlot_Description)
    {
        @Override
        public Widget createWidget()
        {
            return new ScatterPlotWidget();
        }
    };

    public ScatterPlotWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }
}
