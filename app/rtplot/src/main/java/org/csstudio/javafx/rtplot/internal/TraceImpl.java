/*******************************************************************************
 * Copyright (c) 2014-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import java.util.Objects;
import java.util.Optional;

import org.csstudio.javafx.rtplot.LineStyle;
import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.RTPlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;

import javafx.scene.paint.Color;

/** Trace, i.e. data to be displayed on an axis.
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TraceImpl<XTYPE extends Comparable<XTYPE>> implements Trace<XTYPE>
{
    private volatile PlotDataProvider<XTYPE> data;

    private volatile boolean visible = true;

    private volatile String name;

    private volatile String units;

    private volatile Color color;

    private volatile TraceType type;

    private volatile int width;

    private volatile LineStyle line_style;

    private volatile PointType point_type;

    private volatile int size;

    private volatile int y_axis;

    private volatile Optional<PlotDataItem<XTYPE>> selected_sample = Optional.empty();


    public TraceImpl(final String name,
            final String units,
            final PlotDataProvider<XTYPE> data,
            final Color color,
            final TraceType type, final int width,
            final LineStyle line_style,
            final PointType point_type, final int size,
            final int y_axis)
    {
        this.name = Objects.requireNonNull(name);
        this.units = units == null ? "" : units;
        this.data = Objects.requireNonNull(data);
        this.color = Objects.requireNonNull(color);
        this.type = Objects.requireNonNull(type);
        this.width = width;
        this.line_style = line_style;
        this.point_type = Objects.requireNonNull(point_type);
        this.size = size;
        this.y_axis = y_axis;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isVisible()
    {
        return visible;
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(final boolean visible)
    {
        this.visible = visible;
    }

    /** {@inheritDoc} */
    @Override
    public String getName()
    {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public boolean setName(final String name)
    {
        if (this.name.equals(Objects.requireNonNull(name)))
            return false;
        this.name = name;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getUnits()
    {
        if (name.isEmpty())
            return "";
        return units;
    }

    /** {@inheritDoc} */
    @Override
    public void setUnits(final String units)
    {
        this.units = units == null ? "" : units;
    }

    /** {@inheritDoc} */
    @Override
    public void updateData(final PlotDataProvider<XTYPE> data)
    {
        this.data = data;
    }

    /** {@inheritDoc} */
    @Override
    public PlotDataProvider<XTYPE> getData()
    {
        return data;
    }

    /** {@inheritDoc} */
    @Override
    public Color getColor()
    {
        return color;
    }

    /** {@inheritDoc} */
    @Override
    public void setColor(final Color color)
    {
        this.color = Objects.requireNonNull(color);
    }

    /** {@inheritDoc} */
    @Override
    public TraceType getType()
    {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public void setType(final TraceType type)
    {
        this.type = Objects.requireNonNull(type);
    }

    /** {@inheritDoc} */
    @Override
    public int getWidth()
    {
        return width;
    }

    /** {@inheritDoc} */
    @Override
    public void setWidth(final int width)
    {
        this.width = width;
    }

    /** {@inheritDoc} */
    @Override
    public LineStyle getLineStyle()
    {
        return line_style;
    }

    /** {@inheritDoc} */
    @Override
    public void setLineStyle(final LineStyle line_style)
    {
        this.line_style = line_style;
    }

    /** {@inheritDoc} */
    @Override
    public PointType getPointType()
    {
        return point_type;
    }

    /** {@inheritDoc} */
    @Override
    public void setPointType(final PointType type)
    {
        this.point_type = Objects.requireNonNull(type);
    }

    /** {@inheritDoc} */
    @Override
    public int getPointSize()
    {
        return size;
    }

    /** {@inheritDoc} */
    @Override
    public void setPointSize(final int size)
    {
        this.size = size;
    }

    /** {@inheritDoc} */
    @Override
    public int getYAxis()
    {
        return y_axis;
    }

    /** Set the Y Axis index.
     *
     *  @param y_axis Y axis index
     *  @see RTPlot#moveTrace()
     */
    public void setYAxis(final int y_axis)
    {
        this.y_axis = y_axis;
    }

    /** @param sample Sample under cursor or <code>null</code> */
    public void selectSample(final PlotDataItem<XTYPE> sample)
    {
        selected_sample = Optional.ofNullable(sample);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<PlotDataItem<XTYPE>> getSelectedSample()
    {
        return selected_sample;
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return "Trace " + name;
    }
}
