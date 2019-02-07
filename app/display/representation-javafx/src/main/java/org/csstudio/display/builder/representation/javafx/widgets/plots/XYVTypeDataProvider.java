/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
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
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ListNumber;

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
    public static final ListNumber EMPTY = ArrayDouble.of(new double[0]);

    /** Sharing the _read_ half of just one lock.
     *  Never using the _write_ half, since this class is immutable
     */
    private static final ReadWriteLock lock = new InstrumentedReadWriteLock();


    private final PlotDataItem<Double>[] items;

    /** Set the plot's data
     *  @param x_data X data, may be <code>null</code>
     *  @param y_data Y data, may be <code>null</code>,
     *                but at least one of x or y data must be non-<code>null</code>
     *  @param error_data Error data
     */
    @SuppressWarnings("unchecked")
    public XYVTypeDataProvider(ListNumber x_data, ListNumber y_data, ListNumber error_data)
    {
        // In principle, error_data should have 1 element or same size as X and Y..
        if (error_data == null)
            error_data = EMPTY;

        // Could create each PlotDataItem lazily in get(),
        // but create array of PlotDataItems right now because
        // plot will likely iterate over the data elements at least once to plot value,
        // maybe again to plot outline, find value at cursor etc.
        final int size;
        if (x_data == null)
            size = y_data.size();
        else if (y_data == null)
            size = x_data.size();
        else
            size = Math.min(x_data.size(), y_data.size());

        items = new PlotDataItem[size];
        for (int index=0; index < size; ++index)
        {
            final double x = x_data == null ? index : x_data.getDouble(index);
            final double y = y_data == null ? index : y_data.getDouble(index);

            if (error_data.size() <= 0) // No error data
                items[index] = new SimpleDataItem<>(x, y, Double.NaN, Double.NaN, Double.NaN, null);
            else
            {   // Use corresponding array element, or [0] for scalar error info
                // (silently treating size(error) < size(Y) as a mix of error array and scalar)
                final double error = (error_data.size() > index) ? error_data.getDouble(index) : error_data.getDouble(0);
                items[index] = new SimpleDataItem<>(x, y, Double.NaN, y - error, y + error, null);
            }
        }
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
        return items.length;
    }

    @Override
    public PlotDataItem<Double> get(final int index)
    {
        return items[index];
    }

    @Override
    public String toString()
    {
        return "XYVTypeDataProvider, " + items.length + " items, lock: " + lock.toString();
    }
}
