/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.plot;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Annotation;
import org.csstudio.javafx.rtplot.Axis;
import org.csstudio.javafx.rtplot.AxisRange;
import org.csstudio.javafx.rtplot.RTPlotListener;
import org.csstudio.javafx.rtplot.RTTimePlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.YAxis;
import org.csstudio.javafx.rtplot.data.InstrumentedReadWriteLock;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AnnotationInfo;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.preferences.Preferences;

import javafx.application.Platform;
import javafx.scene.control.Button;

/** Data Browser 'Plot' that displays the samples in a {@link Model}.
 *  <p>
 *  Links the underlying {@link RTTimePlot} to the {@link Model}.
 *
 *  @author Kay Kasemir
 *  @author Laurent PHILIPPE Modify addListener method to add property changed event capability
 *  @author Megan Grodowitz Remove SWT components
 */
@SuppressWarnings("nls")
public class ModelBasedPlot
{
    /** Plot Listener */
    protected Optional<PlotListener> listener = Optional.empty();

    /** Plot widget/figure */
    protected RTTimePlot plot;

    private final Map<Trace<Instant>, ModelItem> items_by_trace = new ConcurrentHashMap<>();

    /** plot.getTraces() is thread safe in the sense that one may
     *  always call it to get the current list of traces.
     *  It is simply called that way to update annotations.
     *
     *  There is, however, a race when PVs are (initially) added.
     *  For that, all traces of the plot are removed and then added
     *  as the list of PVs changes resp. grows.
     *  Concurrently, data with 'units' may arrive from PV,
     *  requiring to read the trace list and then update its units.
     *  This lock is used when updating the trace list (write lock)
     *  and when finding a trace (read lock).
     */
    private final ReentrantReadWriteLock trace_lock = new InstrumentedReadWriteLock();

    /** Initialize plot
     *  @param parent Parent widget
     *  @throws Exception
     */
    public ModelBasedPlot(final boolean active)
    {
        plot = new RTTimePlot(active);
        plot.setOpacity(Preferences.opacity);
        plot.showLegend(false);

        final Button time_config_button =
            plot.addToolItem(Activator.getIcon("time_range"), Messages.StartEndDialogTT);
        if (active)
            time_config_button.setOnAction(e -> listener.ifPresent((l) -> l.timeConfigRequested()));

        // Configure axes
        final Axis<Instant> time_axis = plot.getXAxis();
        time_axis.setName(Messages.Plot_TimeAxisName);
        final YAxis<Instant> value_axis = plot.getYAxes().get(0);
        value_axis.setName(Messages.Plot_ValueAxisName);

        // Forward user changes to plot to model
        plot.addListener(new RTPlotListener<Instant>()
        {
            @Override
            public void changedXAxis(final Axis<Instant> x_axis)
            {
                final AxisRange<Instant> range = x_axis.getValueRange();
                listener.ifPresent(l -> l.timeAxisChanged(plot.isScrolling(), range.getLow(), range.getHigh()));
            }

            @Override
            public void changedYAxis(final YAxis<Instant> y_axis)
            {
                final int index = plot.getYAxes().indexOf(y_axis);
                final AxisRange<Double> range = y_axis.getValueRange();
                listener.ifPresent(l -> l.valueAxisChanged(index, range.getLow(), range.getHigh()));
            }

            @Override
            public void changedAutoScale(final Axis<?> y_axis)
            {
                final int index = plot.getYAxes().indexOf(y_axis);
                listener.ifPresent(l -> l.autoScaleChanged(index, y_axis.isAutoscale()));
            }

            @Override
            public void changedGrid(final Axis<?> axis)
            {
                final int index = axis == plot.getXAxis()
                                ? -1
                                : plot.getYAxes().indexOf(axis);
                listener.ifPresent(l -> l.gridChanged(index, axis.isGridVisible()));
            }

            @Override
            public void changedLogarithmic(final Axis<?> axis)
            {
                final int index = plot.getYAxes().indexOf(axis);
                listener.ifPresent(l -> l.logarithmicChanged(index, ((YAxis<?>)axis).isLogarithmic()));
            }

            @Override
            public void changedAnnotations()
            {
                final List<AnnotationInfo> annotations = new ArrayList<>();
                final List<Trace<Instant>> traces = new ArrayList<>();
                for (Trace<Instant> trace : plot.getTraces())
                    traces.add(trace);
                for (Annotation<Instant> annotation : plot.getAnnotations())
                {
                    final int item_index = traces.indexOf(annotation.getTrace());
                    annotations.add(new AnnotationInfo(annotation.isInternal(),
                                                       item_index,
                                                       annotation.getPosition(), annotation.getValue(),
                                                       annotation.getOffset(), annotation.getText()));
                }
                listener.ifPresent(l -> l.changedAnnotations(annotations));
            }

            @Override
            public void changedCursors()
            {
                for (Trace<Instant> trace : plot.getTraces())
                    findModelItem(trace).setSelectedSample(trace.getSelectedSample());
                listener.ifPresent(l -> l.selectedSamplesChanged());
            }

            @Override
            public void changedToolbar(final boolean visible)
            {
                listener.ifPresent(l -> l.changedToolbar(visible));
            }

            @Override
            public void changedLegend(final boolean visible)
            {
                listener.ifPresent(l -> l.changedLegend(visible));
            }
        });
    }

    /** @return RTTimePlot */
    public RTTimePlot getPlot()
    {
        return plot;
    }

    /** Add a listener (currently only one supported) */
    public void addListener(final PlotListener listener)
    {
        if (this.listener.isPresent())
            throw new IllegalStateException("Only one listener supported");
        this.listener = Optional.of(listener);
    }

    public PlotListener getListener()
    {
        return listener.orElse(null);
    }

    /** Adding or removing traces requires a write lock */
    public boolean lockTracesForWriting()
    {
        try
        {
            if (trace_lock.writeLock().tryLock(5, TimeUnit.SECONDS))
                return true;
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot lock plot traces", ex);
        }
        return false;
    }

    /** Release traces write lock */
    public void unlockTracesForWriting()
    {
        trace_lock.writeLock().unlock();;
    }

    /** Read lock */
    public boolean lockTraces()
    {
        try
        {
            if (trace_lock.readLock().tryLock(5, TimeUnit.SECONDS))
                return true;
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot lock plot traces", ex);
        }
        return false;
    }

    /** Release traces read lock */
    public void unlockTraces()
    {
        trace_lock.readLock().unlock();
    }

    /** Remove all axes and traces */
    public void removeAll()
    {
        if (! trace_lock.isWriteLockedByCurrentThread())
            logger.log(Level.WARNING, "Traces not locked", new Exception("Call stack"));
        items_by_trace.clear();
        // Remove all traces
        for (Trace<Instant> trace : plot.getTraces())
            plot.removeTrace(trace);
        // Now that Y axes are unused, remove all except for primary
        int N = plot.getYAxes().size();
        while (N > 1)
            plot.removeYAxis(--N);
    }

    /** @param index
     *             Index of Y axis. If it doesn't exist, it will be created.
     *  @return Y Axis
     */
    private YAxis<Instant> getYAxis(final int index)
    {
        // Get Y Axis, creating new ones if needed
        int N = plot.getYAxes().size();
        while (N <= index)
        {
            plot.addYAxis(MessageFormat.format(Messages.Plot_ValueAxisNameFMT, N));
            N = plot.getYAxes().size();
        }
        return plot.getYAxes().get(index);
    }

    /** Get number of axes (includes xaxis)
     *
     * @return number of y axes plus 1 for x axis
     */
    public int getTotalAxesCount ()
    {
        return (plot.getYAxes().size() + 1);
    }

    public Axis<?> getPlotAxis(final int index)
    {
        if (index < plot.getYAxes().size())
            return plot.getYAxes().get(index);

        if (index == plot.getYAxes().size())
            return plot.getXAxis();

        return null;
    }

    /** Update value axis from model
     *  @param index Axis index. Y axes will be created as needed.
     *  @param config Desired axis configuration
     */
    public void updateAxis(final int index, final AxisConfig config)
    {
        final YAxis<Instant> axis = getYAxis(index);
        axis.setName(config.getResolvedName());
        axis.useAxisName(config.isUsingAxisName());
        axis.useTraceNames(config.isUsingTraceNames());
        axis.setColor(config.getPaintColor());
        axis.setLogarithmic(config.isLogScale());
        axis.setGridVisible(config.isGridVisible());
        axis.setAutoscale(config.isAutoScale());
        axis.setValueRange(config.getMin(), config.getMax());
        axis.setVisible(config.isVisible());
        axis.setOnRight(config.isOnRight());
    }

    /** Add a trace to the plot
     *  @param item ModelItem for which to add a trace
     *  @author Laurent PHILIPPE
     */
    public void addTrace(final ModelItem item)
    {
        if (! trace_lock.isWriteLockedByCurrentThread())
            logger.log(Level.WARNING, "Traces not locked", new Exception("Call stack"));
        final Trace<Instant> trace = plot.addTrace(item.getResolvedDisplayName(),
                item.getUnits(),
                item.getSamples(),
                item.getPaintColor(),
                item.getTraceType(), item.getLineWidth(),
                item.getLineStyle(),
                item.getPointType(), item.getPointSize(),
                item.getAxisIndex());
        items_by_trace.put(trace, item);
    }

    /** @param item ModelItem to remove from plot */
    public void removeTrace(final ModelItem item)
    {
        if (! trace_lock.isWriteLockedByCurrentThread())
            logger.log(Level.WARNING, "Traces not locked", new Exception("Call stack"));
        final Trace<Instant> trace;
        try
        {
            trace = findTrace(item);
        }
        catch (IllegalArgumentException ex)
        {   // Could be called with a trace that was not visible,
            // so it was never in the plot,
            // and now gets removed.
            // --> No error, because trace to be removed is already
            //     absent from plot
            return;
        }
        plot.removeTrace(trace);
        items_by_trace.remove(trace);
    }

    /** Update the configuration of a trace from Model Item
     *  @param item Item that was previously added to the Plot
     */
    public void updateTrace(final ModelItem item)
    {
        // Invisible items have no trace, nothing to update,
        // and findTrace() would throw an exception
        if (! item.isVisible())
            return;
        final Trace<Instant> trace = findTrace(item);
        // Update Trace with item's configuration
        final String name = item.getResolvedDisplayName();
        if (!trace.getName().equals(name))
            trace.setName(name);
        trace.setUnits(item.getUnits());
        // These happen to not cause an immediate redraw, so
        // set even if no change
        trace.setColor(item.getPaintColor());
        trace.setType(item.getTraceType());
        trace.setWidth(item.getLineWidth());
        trace.setLineStyle(item.getLineStyle());
        trace.setPointType(item.getPointType());
        trace.setPointSize(item.getPointSize());

        // Locate index of current Y Axis
        if (trace.getYAxis() != item.getAxisIndex())
            plot.moveTrace(trace, item.getAxisIndex());
        plot.requestUpdate();
    }

    /** @param item {@link ModelItem} for which to locate the {@link Trace}
     *  @return Trace
     *  @throws RuntimeException When trace not found
     */
    private Trace<Instant> findTrace(final ModelItem item)
    {
        if (! lockTraces())
            throw new IllegalArgumentException("Cannot lock traces to locate trace for " + item);
        try
        {
            for (Trace<Instant> trace : plot.getTraces())
                if (trace.getData() == item.getSamples())
                    return trace;
            throw new IllegalArgumentException("Cannot locate trace for " + item);
        }
        finally
        {
            unlockTraces();
        }
    }

    /** @param trace {@link Trace} for which to locate the {@link ModelItem}
     *  @return ModelItem
     *  @throws RuntimeException When not found
     */
    private ModelItem findModelItem(final Trace<Instant> trace)
    {
        try
        {
            return items_by_trace.get(trace);
        }
        catch (Throwable ex)
        {
            throw new RuntimeException("Cannot locate item for " + trace, ex);
        }
    }

    /** Update plot to given time range.
     *  Can be called from any thread.
     *  @param scroll
     *  @param start
     *  @param end
     */
    public void setTimeRange(final boolean scroll, final Instant start, final Instant end)
    {
        Platform.runLater(() ->
        {
            plot.setScrolling(scroll);
            plot.getXAxis().setValueRange(start, end);
        });
    }

    /** Set annotations in plot to match model's annotations
     *  @param newAnnotations Annotations to show in plot
     */
    public void setAnnotations(final Collection<AnnotationInfo> newAnnotations)
    {
        final List<Trace<Instant>> traces = new ArrayList<>();
        for (Trace<Instant> trace : plot.getTraces())
            traces.add(trace);

        // Remove old annotations from plot
        final List<Annotation<Instant>> plot_annotations = new ArrayList<>(plot.getAnnotations());
        for (Annotation<Instant> old : plot_annotations)
            plot.removeAnnotation(old);

        // Set new annotations in plot
        for (AnnotationInfo annotation : newAnnotations)
            plot.addAnnotation(new Annotation<>(annotation.isInternal(),
                                                traces.get(annotation.getItemIndex()),
                                                annotation.getTime(),
                                                annotation.getValue(),
                                                annotation.getOffset(),
                                                annotation.getText()));
    }

    /** Refresh the plot because the data has changed */
    public void redrawTraces()
    {
        plot.requestUpdate();
    }

    /** Must be called to release resources (update threads, ...) */
    public void dispose()
    {
        plot.dispose();
    }
}
