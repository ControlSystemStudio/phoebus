/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.javafx.rtplot.data.PlotDataItem;

import javafx.scene.paint.Color;

/** Used by the {@link YAxisImpl} to obtain labels.
 *
 *  <p>The number of traces on a Y Axis and thus
 *  the number of labels can change dynamically.
 *  This provides a snapshot of the labels and
 *  their color that is updated whenever calling
 *  <code>start</code>, then treating it like
 *  an iterator:
 *
 *  <pre>
 *  start();
 *  while (hasNext())
 *  {
 *      .. use getLabel(), getColor()
 *  }
 *  </pre>
 *
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AxisLabelProvider<XTYPE extends Comparable<XTYPE>>
{
    final private YAxisImpl<XTYPE> axis;
    final private List<String> labels = new ArrayList<>();
    final private List<Color> colors = new ArrayList<>();
    private int index = -1;
    private boolean use_axis_name = true;
    private boolean use_trace_names = true;

    /** @param axis Axis for which to provide labels */
    public AxisLabelProvider(final YAxisImpl<XTYPE> axis)
    {
        this.axis = axis;
    }

    /** @return <code>true</code>if axis name is used */
    public boolean isUsingAxisName()
    {
        return use_axis_name;
    }

    /** @param use_axis_name If <code>true</code>, show axis name */
    public void useAxisName(final boolean use_axis_name)
    {
        this.use_axis_name = use_axis_name;
    }

    /** @return <code>true</code>if axis uses
     *          the names of traces instead of its
     *          own name for a label
     */
    public boolean isUsingTraceNames()
    {
        return use_trace_names;
    }

    /** @param use_trace_names If <code>true</code>, axis uses
     *                         the names of traces instead of its
     *                         own name for a label
     */
    public void useTraceNames(final boolean use_trace_names)
    {
        this.use_trace_names = use_trace_names;
    }


    /** Start another iteration of current labels */
    public void start()
    {
        index = -1;
        labels.clear();
        colors.clear();
        if (use_axis_name)
        {
            final String name = axis.getName();
            if (name != null  &&  !name.isEmpty())
            {
                labels.add(name);
                colors.add(axis.getColor());
            }
        }
        if (use_trace_names)
            for (TraceImpl<XTYPE> trace : axis.getTraces())
            {
                labels.add(trace.getLabel());
                colors.add(trace.getColor());
            }
    }

    /** @return <code>true</code> if there is one more label */
    public boolean hasNext()
    {
        if (index >= labels.size()-1)
            return false;
        ++index;
        return true;
    }

    /** @return Text of the label */
    public String getLabel()
    {
        return labels.get(index);
    }

    /** @return Text of the separator */
    public String getSeparator()
    {
        return (use_axis_name && index == 1) ? ": " : ", ";
    }

    /** @return Color of the label */
    public Color getColor()
    {
        return colors.get(index);
    }
}
