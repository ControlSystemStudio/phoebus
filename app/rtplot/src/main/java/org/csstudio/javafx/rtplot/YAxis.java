/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import org.csstudio.javafx.rtplot.data.PlotDataItem;

/** Public Interface for Y axes.
 *
 *  @param <XTYPE> Data type of the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
public interface YAxis<XTYPE extends Comparable<XTYPE>> extends Axis<Double>
{
    /** @return <code>true</code>if axis name is used */
    public boolean isUsingAxisName();

    /** @param use_axis_name If <code>true</code>, show axis name */
    public void useAxisName(boolean use_axis_name);

    /** @return <code>true</code>if axis uses
     *          the names of traces instead of its
     *          own name for a label
     */
    public boolean isUsingTraceNames();

    /** @param use_trace_names If <code>true</code>, axis uses
     *                         the names of traces instead of its
     *                         own name for a label
     */
    public void useTraceNames(boolean use_trace_names);

    /** Configure the Y-Axis to use a log. scale or not.
     *  <p>
     *  Initial default is <code>false</code>, i.e. linear scale.
     */
    public void setLogarithmic(boolean use_log);

    /** @return <code>true</code> if the axis is logarithmic. */
    public boolean isLogarithmic();

    /** Can only be called for non-logarithmic axis,
     *  and value will be lost when switching to logarithmic axis.
     *
     *  @param order_of_magnitude If value range exceeds this threshold, use exponential notation
     */
    public void setExponentialThreshold(long order_of_magnitude);

    /** @return <code>true</code> if axis is shown on right instead of left side */
    public boolean isOnRight();

    /** @param right Set <code>true</code> if axis should shown on right instead of left side */
    public void setOnRight(boolean right);
}
