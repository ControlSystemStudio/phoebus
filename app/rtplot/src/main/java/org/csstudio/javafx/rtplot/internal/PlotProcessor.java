/*******************************************************************************
 * Copyright (c) 2014-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Axis;
import org.csstudio.javafx.rtplot.AxisRange;
import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.javafx.rtplot.data.PlotDataSearch;
import org.csstudio.javafx.rtplot.data.ValueRange;
import org.csstudio.javafx.rtplot.internal.undo.AddAnnotationAction;
import org.csstudio.javafx.rtplot.internal.undo.ChangeAxisRanges;
import org.csstudio.javafx.rtplot.internal.util.Log10;

import javafx.geometry.Point2D;

/** Helper for processing traces of a plot
 *  in a thread pool to avoid blocking UI thread.
 *  @param <XTYPE> Data type of horizontal {@link Axis}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PlotProcessor<XTYPE extends Comparable<XTYPE>>
{
    final private static ExecutorService thread_pool = ForkJoinPool.commonPool();

    final private Plot<XTYPE> plot;

    /** @param plot Plot on which this processor operates */
    public PlotProcessor(final Plot<XTYPE> plot)
    {
        this.plot = plot;
    }

    /** Submit background job for determining the position range
     *  @param yaxes YAxes that have traces
     *  @return Position range or <code>null</code>
     *  @throws Exception on error
     */
    private AxisRange<XTYPE> determinePositionRange(final List<YAxisImpl<XTYPE>> yaxes) throws InterruptedException, Exception
    {
        XTYPE start = null, end = null;
        for (YAxisImpl<XTYPE> yaxis : yaxes)
        {
            for (Trace<XTYPE> trace : yaxis.getTraces())
            {
                if (trace.isVisible() == false ||
                    (trace.getType() == TraceType.NONE  &&  trace.getPointType() == PointType.NONE))
                    continue;
                final PlotDataProvider<XTYPE> data = trace.getData();
                if (! data.getLock().tryLock(10, TimeUnit.SECONDS))
                    throw new TimeoutException("Cannot lock data for " + trace + ": " + data);
                try
                {
                    // Position range tends to be ordered, which would allow to simply
                    // use position 0 and N-1, but order is not guaranteed...
                    final int N = data.size();
                    for (int i=0; i<N; ++i)
                    {
                        XTYPE pos = data.get(i).getPosition();
                        // If sample is Double (not Instant), AND NaN/inf, skip this trace
                        if ((pos instanceof Double)  &&  !Double.isFinite((Double) pos))
                            continue;
                        if (start == null  ||  start.compareTo(pos) > 0)
                            start = pos;
                        if (end == null  ||  end.compareTo(pos) < 0)
                            end = pos;
                    }
                }
                finally
                {
                    data.getLock().unlock();
                }
            }
        }
        if (start == null  ||  end == null)
            return null;
        return new AxisRange<>(start, end);
    }

    /** @param data {@link PlotDataProvider} with values
     *  @return <code>true</code> if the 'positions' are in order
     */
    private boolean isOrdered(final PlotDataProvider<XTYPE> data)
    {
        final int N = data.size();
        if (N <= 0)
            return false;
        XTYPE prev = data.get(0).getPosition();
        for (int i=1; i<N; ++i)
        {
            final XTYPE current = data.get(i).getPosition();
            if (prev.compareTo(current) > 0)
                return false;
            prev = current;
        }
        return true;
    }

    /** Submit background job to determine value range
     *  @param data {@link PlotDataProvider} with values
     *  @param position_range Range of positions to consider
     *  @return {@link Future} to {@link ValueRange}
     */
    public Future<ValueRange> determineValueRange(final PlotDataProvider<XTYPE> data, AxisRange<XTYPE> position_range)
    {
        return thread_pool.submit(new Callable<ValueRange>()
        {
            @Override
            public ValueRange call() throws Exception
            {
                double low = Double.MAX_VALUE;
                double high = -Double.MAX_VALUE;
                final PlotDataSearch<XTYPE> search = new PlotDataSearch<>();

                if (! data.getLock().tryLock(10, TimeUnit.SECONDS))
                    throw new TimeoutException("Cannot lock " + data);
                try
                {
                    if (data.size() > 0)
                    {
                        int start, stop;
                        if (isOrdered(data))
                        {
                            // Find start..stop indices from ordered positions to match axis range.
                            // Consider first sample at-or-before start
                            start = search.findSampleLessOrEqual(data, position_range.getLow());
                            if (start < 0)
                                start = 0;
                            // Last sample is the one just inside end of range.
                            stop = search.findSampleLessOrEqual(data, position_range.getHigh());
                            if (stop < 0)
                                stop = 0;
                            if (logger.isLoggable(Level.FINE))
                                logger.log(Level.FINE, "For " + data.size() + " samples, checking elements " + start + " .. " + stop +
                                           " which are positioned within " + position_range.getLow() + " .. " + position_range.getHigh());
                        }
                        else
                        {
                            // Data does not have ordered 'positions', so consider all samples
                            start = 0;
                            stop = data.size()-1;
                        }

                        // If data is completely outside the position_range,
                        // we end up using just data[0]
                        // Check [start .. stop], including stop
                        for (int idx = start; idx <= stop; idx++)
                        {
                            final PlotDataItem<XTYPE> item = data.get(idx);
                            final double value = item.getValue();
                            if (!Double.isFinite(value))
                                continue;
                            if (value < low)
                                low = value;
                            if (value > high)
                                high = value;
                            // Implies Double.isFinite(min), ..(max)
                            if (item.getMin() < low)
                                low = item.getMin();
                            if (item.getMax() > high)
                                high = item.getMax();
                        }
                    }
                }
                finally
                {
                    data.getLock().unlock();
                }
                return new ValueRange(low, high);
            }
        });
    }

    /** Submit background job to determine value range
     *  @param axis {@link YAxisImpl} for which to determine range
     *  @param position_range Range of positions to consider
     *  @return {@link Future} to {@link ValueRange}
     */
    public Future<ValueRange> determineValueRange(final YAxisImpl<XTYPE> axis, AxisRange<XTYPE> position_range)
    {
        return thread_pool.submit(new Callable<ValueRange>()
        {
            @Override
            public ValueRange call() throws Exception
            {
                // In parallel, determine range of all traces in this axis
                final List<Future<ValueRange>> ranges = new ArrayList<>();
                for (Trace<XTYPE> trace : axis.getTraces())
                    if (trace.isVisible() &&
                        (trace.getType() != TraceType.NONE  ||  trace.getPointType() != PointType.NONE))
                        ranges.add(determineValueRange(trace.getData(), position_range));

                // Merge the trace ranges into overall axis range
                double low = Double.MAX_VALUE;
                double high = -Double.MAX_VALUE;
                for (Future<ValueRange> result : ranges)
                {
                    final ValueRange range = result.get();
                    if (range.getLow() < low)
                        low = range.getLow();
                    if (range.getHigh() > high)
                        high = range.getHigh();
                }
                return new ValueRange(low, high);
            }
        });
    }

    /** Round value range up/down to add a little room above & below the exact range.
     *  This results in "locking" to a nice looking range for a while
     *  until a new sample outside of the rounded range is added.
     *
     *  @param low Low and
     *  @param high high end of value range
     *  @return Adjusted range
     */
    public ValueRange roundValueRange(final double low, final double high)
    {
        final double size = Math.abs(high-low);
        if (size > 0)
        {   // Add 2 digits to the 'tight' order of magnitude
            final double order_of_magnitude = Math.floor(Log10.log10(size))-2;
            final double round = Math.pow(10, order_of_magnitude);
            return new ValueRange(Math.floor(low / round) * round,
                                         Math.ceil(high / round) * round);
        }
        else
            return new ValueRange(low, high);
    }

    /** Stagger the range of axes */
    public void stagger()
    {
        thread_pool.execute(() ->
        {
            final double GAP = 0.1;

            // Arrange all axes so they don't overlap by assigning 1/Nth of
            // the vertical range to each one
            // Determine range of each axes' traces in parallel
            final List<YAxisImpl<XTYPE>> y_axes = new ArrayList<>();
            final List<AxisRange<Double>> original_ranges = new ArrayList<>();
            final List<AxisRange<Double>> new_ranges = new ArrayList<>();
            final List<Future<ValueRange>> ranges = new ArrayList<>();
            for (YAxisImpl<XTYPE> axis : plot.getYAxes())
            {
                y_axes.add(axis);
                // As fallback, assume that new range matches old range
                new_ranges.add(axis.getValueRange());
                original_ranges.add(axis.getValueRange());
                ranges.add(determineValueRange(axis, plot.getXAxis().getValueRange()));
            }
            final int N = y_axes.size();
            for (int i=0; i<N; ++i)
            {
                final YAxisImpl<XTYPE> axis = y_axes.get(i);
                // Does axis handle itself in another way?
                if (axis.isAutoscale())
                   continue;

                // Fetch range of values on this axis
                final ValueRange axis_range;
                try
                {
                    axis_range = ranges.get(i).get();
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Axis stagger error", ex);
                    continue;
                }

                // Skip axis which for some reason cannot determine its range
                double low = axis_range.getLow();
                double high = axis_range.getHigh();
                if (low > high)
                    continue;
                if (low == high)
                {   // Center trace with constant value (empty range)
                    final double half = Math.abs(low/2);
                    low -= half;
                    high += half;
                }
                if (axis.isLogarithmic())
                {   // Transition into log space
                    low = Log10.log10(low);
                    high = Log10.log10(high);
                }
                double span = high - low;
                // Make some extra space
                low -= GAP*span;
                high += GAP*span;
                span = high-low;

                // With N axes, assign 1/Nth of the vertical plot space to this axis
                // by shifting the span down according to the axis index,
                // using a total of N*range.
                low -= (N-i-1)*span;
                high += i*span;

                final ValueRange rounded = roundValueRange(low, high);
                low = rounded.getLow();
                high = rounded.getHigh();

                if (axis.isLogarithmic())
                {   // Revert from log space
                    low = Log10.pow10(low);
                    high = Log10.pow10(high);
                }

                // Sanity check for empty traces
                if (low < high  &&
                    !Double.isInfinite(low) && !Double.isInfinite(high))
                {
                	final AxisRange<Double> orig = original_ranges.get(i);
                	final boolean normal = orig.getLow() < orig.getHigh();
                	new_ranges.set(i, normal ? new AxisRange<>(low, high)
                			                 : new AxisRange<>(high, low));
                }
            }

            // 'Stagger' tends to be on-demand,
            // or executed infrequently as archived data arrives after a zoom operation
            // -> Use undo, which also notifies listeners
            plot.getUndoableActionManager().execute(new ChangeAxisRanges<>(plot, Messages.Zoom_Stagger, y_axes, original_ranges, new_ranges, null));
        });
    }

    /** @param plot Plot where annotation is added
     *  @param trace Trace to which a annotation should be added
     *  @param text Text for the annotation
     */
    public void createAnnotation(final Trace<XTYPE> trace, final String text)
    {
        final AxisPart<XTYPE> x_axis = plot.getXAxis();
        // Run in thread
        thread_pool.submit(() ->
        {
            final AxisRange<Integer> range = x_axis.getScreenRange();
            XTYPE location = x_axis.getValue((range.getLow() + range.getHigh())/2);
            final PlotDataSearch<XTYPE> search = new PlotDataSearch<>();
            final PlotDataProvider<XTYPE> data = trace.getData();
            double value= Double.NaN;
            if (! data.getLock().tryLock(10, TimeUnit.SECONDS))
                throw new TimeoutException("Cannot create annotation, no lock on " + data);
            try
            {
                final int index = search.findSampleGreaterOrEqual(data, location);
                if (index >= 0)
                {
                    location = data.get(index).getPosition();
                    value = data.get(index).getValue();
                }
                else
                    location = null;
            }
            finally
            {
                data.getLock().unlock();
            }
            if (location != null)
                plot.getUndoableActionManager().execute(
                    new AddAnnotationAction<>(plot,
                                                   new AnnotationImpl<>(false, trace, location, value,
                                                                             new Point2D(20, -20),
                                                                             text)));
            return null;
        });
    }

    /** Perform autoscale for all axes that are marked as such */
    public void autoscale()
    {
        logger.log(Level.FINE, "Autoscale...");
        // In low-memory situation, the following can fail because threads for determining
        // range cannot be created.
        // Catch any error to keep autoscale problem from totally skipping a plot update.
        try
        {
            final List<YAxisImpl<XTYPE>> all_y_axes = plot.getYAxes();

            // The value range is only checked for samples within the current X range.
            // So if X axis is auto-scaled, fetch its range first.
            if (plot.getXAxis().isAutoscale())
            {
                final AxisRange<XTYPE> range = determinePositionRange(all_y_axes);
                if (range != null)
                {
                    logger.log(Level.FINE, () -> plot.getXAxis().getName() + " range " + range.getLow() + " .. " + range.getHigh());
                    plot.getXAxis().setValueRange(range.getLow(), range.getHigh());
                    plot.fireXAxisChange();
                }
            }

            // Determine range of each axes' traces in parallel
            final List<YAxisImpl<XTYPE>> y_axes = new ArrayList<>();
            final List<Future<ValueRange>> ranges = new ArrayList<>();
            for (YAxisImpl<XTYPE> axis : all_y_axes)
                if (axis.isAutoscale())
                {
                    y_axes.add(axis);
                    ranges.add(determineValueRange(axis, plot.getXAxis().getValueRange()));
                }
                else
                    logger.log(Level.FINE, () -> axis.getName() + " skipped");

            final int N = y_axes.size();
            for (int i=0; i<N; ++i)
            {
                final YAxisImpl<XTYPE> axis = y_axes.get(i);
                try
                {
                    final ValueRange new_range = ranges.get(i).get();
                    double low = new_range.getLow(), high = new_range.getHigh();
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, axis.getName() + " range " + low + " .. " + high);
                    if (low > high)
                        continue;
                    if (low == high)
                    {   // Center trace with constant value (empty range)
                        final double half = Math.abs(low/2);
                        low -= half;
                        high += half;
                    }
                    if (axis.isLogarithmic())
                    {   // Perform adjustment in log space.
                        // But first, refuse to deal with <= 0
                        if (low <= 0.0)
                            low = 1;
                        if (high <= low)
                            high = low * 100.0;
                        low = Log10.log10(low);
                        high = Log10.log10(high);
                    }
                    final ValueRange rounded = roundValueRange(low, high);
                    low = rounded.getLow();
                    high = rounded.getHigh();
                    if (axis.isLogarithmic())
                    {
                        low = Log10.pow10(low);
                        high = Log10.pow10(high);
                    }
                    else
                    {   // Stretch range a little bit
                        // (but not for log scale, where low just above 0
                        //  could be stretched to <= 0)
                        final double headroom = (high - low) * 0.05;
                        low -= headroom;
                        high += headroom;

                    }
                    // Autoscale happens 'all the time'.
                    // Do not use undo, but notify listeners.
                    if (low != high)
                    {
                    	final AxisRange<Double> orig = axis.getValueRange();
                    	final boolean normal = orig.getLow() < orig.getHigh();
                    	final boolean changed = normal ? axis.setValueRange(low, high)
            										   : axis.setValueRange(high, low);
            			if (changed)
                            plot.fireYAxisChange(axis);
                    }
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Axis autorange error for " + axis, ex);
                }
            }
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Autorange error", ex);
        }
    }
}
