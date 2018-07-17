/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets.plots;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.csstudio.javafx.rtplot.data.InstrumentedReadWriteLock;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.javafx.rtplot.data.SimpleDataItem;
import org.phoebus.util.array.ArrayDouble;
import org.phoebus.util.array.ListNumber;

/** Data provider for RTPlot
 *
 *  <p>Adapts waveforms received from PV
 *  into samples for a trace in the RTPlot.
 *  <ul>
 *  <li>X and Y waveform: Plots Y over X
 *  <li>Y waveform with <code>null</code> for X: Plots Y over array index
 *  </ul>
 *
 *  Error data may be
 *  <ul>
 *  <li><code>null</code> or zero elements: No error bars
 *  <li>One element: Use that error for all samples
 *  <li>One element per sample: Error bar for each sample
 *  </ul>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XYVTypeDataProvider implements PlotDataProvider<Double>
{
    public static final ListNumber EMPTY = new ArrayDouble(new double[0], true);

    /** Sharing the _read_ half of just one lock.
     *  Never using the _write_ half, since this class is immutable
     */
    private static final ReadWriteLock lock = new InstrumentedReadWriteLock();

    private final ListNumber x_data, y_data, error_data;
    private final int size;

    /** Set the plot's data
     *  @param x_data X data, may be <code>null</code>
     *  @param y_data Y data
     *  @param error_data Error data
     */
    public XYVTypeDataProvider(final ListNumber x_data, final ListNumber y_data, final ListNumber error_data)
    {
        // In principle, error_data should have 1 element or same size as X and Y..
        this.x_data = x_data;
        this.y_data = y_data;
        size = x_data == null ? y_data.size() : Math.min(x_data.size(), y_data.size());
        this.error_data = error_data == null ? EMPTY : error_data;
    }

    public XYVTypeDataProvider()
    {
        this(EMPTY, EMPTY, EMPTY);
    }

    @Override
    public Lock getLock()
    {
        return lock.readLock();
    }

    @Override
    public int size()
    {
        return size;
    }

    @Override
    public PlotDataItem<Double> get(final int index)
    {
        final double x = x_data == null ? index : x_data.getDouble(index);
        final double y = y_data.getDouble(index);

        final double min, max;
        if (error_data.size() <= 0)
            min = max = Double.NaN; // No error data
        else
        {   // Use corresponding array element, or [0] for scalar error info
            // (silently treating size(error) < size(Y) as a mix of error array and scalar)
            final double error = (error_data.size() > index) ? error_data.getDouble(index) : error_data.getDouble(0);
            min = y - error;
            max = y + error;
        }
        return new SimpleDataItem<Double>(x, y, Double.NaN, min, max, null);
    }

    @Override
    public String toString()
    {
        return "XYVTypeDataProvider, lock: " + lock.toString();
    }
}
