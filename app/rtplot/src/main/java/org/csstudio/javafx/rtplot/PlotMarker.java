/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import org.csstudio.javafx.rtplot.data.PlotDataItem;

import javafx.scene.paint.Color;

/** Plot Marker, marks position in {@link RTPlot}
 *
 *  <p>Cursor-type marker, vertical line that can
 *  be moved interactively.
 *
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PlotMarker<XTYPE extends Comparable<XTYPE>>
{
    private final Color color;
    private volatile boolean interactive;
    private volatile XTYPE position;

    /** Not meant to be called by user,
     *  call {@link RTPlot#addMarker()} to create a marker.
     */
    public PlotMarker(final Color color,
                      final boolean interactive,
                      final XTYPE position)
    {
        this.color = color;
        this.interactive = interactive;
        this.position = position;
    }

    /** @return Color of the marker */
    public Color getColor()
    {
        return color;
    }

    /** @return Is marker interactive? */
    public boolean isInteractive()
    {
        return interactive;
    }

    /** @param interactive Should marker be interactive? */
    public void setInteractive(final boolean interactive)
    {
        this.interactive = interactive;
    }

    /** @return Position within plot */
    public XTYPE getPosition()
    {
        return position;
    }

    /** @param position Marker position within plot */
    public void setPosition(final XTYPE position)
    {
        this.position = position;
        // Caller needs to request update of plot
    }

    @Override
    public String toString()
    {
        return "PlotMarker @ " + position;
    }
}
