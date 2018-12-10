/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.waveformview;

import java.util.List;

import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.RTPlot;
import org.csstudio.javafx.rtplot.RTValuePlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.trends.databrowser3.Messages;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Menu item to use lines or points
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ToggleLinesMenuItem extends MenuItem
{
    public ToggleLinesMenuItem(final RTValuePlot plot, final List<Trace<Double>> traces)
    {
        if (traces.get(0).getPointType() == PointType.NONE)
        {
            setText(Messages.UsePoints);
            setGraphic(ImageCache.getImageView(RTPlot.class, "/icons/points.png"));
            setOnAction(event ->
            {
                for (Trace<Double> trace : traces)
                {
                    trace.setPointType(PointType.CIRCLES);
                    trace.setType(TraceType.NONE);
                }
                plot.requestUpdate();
            });
        }
        else
        {
            setText(Messages.UseLines);
            setGraphic(ImageCache.getImageView(RTPlot.class, "/icons/lines.png"));
            setOnAction(event ->
            {
                for (Trace<Double> trace : traces)
                {
                    trace.setPointType(PointType.NONE);
                    trace.setType(TraceType.LINES_DIRECT);
                }
                plot.requestUpdate();
            });
        }
    }
}
