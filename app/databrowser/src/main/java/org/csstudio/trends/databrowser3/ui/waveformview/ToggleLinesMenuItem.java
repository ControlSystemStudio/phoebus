/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.waveformview;

import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.RTPlot;
import org.csstudio.javafx.rtplot.RTValuePlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Menu item to use lines or points
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ToggleLinesMenuItem extends MenuItem
{
    public ToggleLinesMenuItem(RTValuePlot plot, final Trace<Double> trace)
    {
        setGraphic(ImageCache.getImageView(RTPlot.class, "/icons/toolbar.png"));

        if (trace.getPointType() == PointType.NONE)
        {
            setText("Use points");
            setOnAction(event ->
            {
                trace.setPointType(PointType.CIRCLES);
                trace.setType(TraceType.NONE);
                plot.requestUpdate();
            });
        }
        else
        {
            setText("Use lines");
            setOnAction(event ->
            {
                trace.setPointType(PointType.NONE);
                trace.setType(TraceType.LINES_DIRECT);
                plot.requestUpdate();
            });
        }
    }
}
