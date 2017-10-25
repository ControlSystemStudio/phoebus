/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.data;

/** Data item that holds all the elements.
 *
 *  <p>For use cases that don't already have a data model
 *  that can be interfaced to the {@link PlotDataItem}
 *
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimpleDataItem<XTYPE extends Comparable<XTYPE>> implements PlotDataItem<XTYPE>
{
    final private XTYPE position;
    final private double value;
    final private double stddev;
    final private double min;
    final private double max;
    final private String info;

    public SimpleDataItem(final XTYPE position, final double value)
    {
        this(position, value, Double.NaN, Double.NaN, Double.NaN, null);
    }

    public SimpleDataItem(final XTYPE time, final double value,
            final double stddev,
            final double min, final double max, final String info)
    {
        this.position = time;
        this.value = value;
        this.stddev = stddev;
        this.min = min;
        this.max = max;
        this.info = info;
    }

    @Override
    public XTYPE getPosition()
    {
        return position;
    }

    @Override
    public double getValue()
    {
        return value;
    }

    @Override
    public double getStdDev()
    {
        return stddev;
    }

    @Override
    public double getMin()
    {
        return min;
    }

    @Override
    public double getMax()
    {
        return max;
    }

    @Override
    public String getInfo()
    {
        if (info == null)
            return Double.toString(value);
        return info;
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(position).append(" ").append(value);
        if (Double.isFinite(stddev))
            buf.append(" stdDev ").append(stddev);
        if (Double.isFinite(min) || Double.isFinite(max))
            buf.append(" [").append(min).append(", ").append(max).append("]");
        return buf.toString();
    }
}
