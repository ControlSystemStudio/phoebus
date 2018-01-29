/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets.plots;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetPointType;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.AxisWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.TraceWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.YAxisWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetTraceType;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget.MarkerProperty;
import org.csstudio.display.builder.representation.RepresentationUpdateThrottle;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.csstudio.javafx.rtplot.Axis;
import org.csstudio.javafx.rtplot.AxisRange;
import org.csstudio.javafx.rtplot.PlotMarker;
import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.RTPlotListener;
import org.csstudio.javafx.rtplot.RTValuePlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.YAxis;
import org.phoebus.util.array.ArrayDouble;
import org.phoebus.util.array.ListNumber;
import org.phoebus.vtype.VNumberArray;
import org.phoebus.vtype.VType;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XYPlotRepresentation extends RegionBaseRepresentation<Pane, XYPlotWidget>
{
    private final DirtyFlag dirty_position = new DirtyFlag();
    private final DirtyFlag dirty_range = new DirtyFlag();
    private final DirtyFlag dirty_config = new DirtyFlag();

    /** Prevent event loop when this code changes the range,
     *  and then receives the range-change-event
     */
    private volatile boolean changing_range = false;

    private final UntypedWidgetPropertyListener range_listener = (WidgetProperty<?> property, Object old_value, Object new_value) ->
    {
        if (changing_range)
            return;
        dirty_range.mark();
        toolkit.scheduleUpdate(this);
    };

    private final UntypedWidgetPropertyListener config_listener = (WidgetProperty<?> property, Object old_value, Object new_value) ->
    {
        dirty_config.mark();
        toolkit.scheduleUpdate(this);
    };

    /** Plot */
    private RTValuePlot plot;

    private volatile boolean changing_marker = false;

    private final RTPlotListener<Double> plot_listener = new RTPlotListener<Double>()
    {
        @Override
        public void changedPlotMarker(final int index)
        {
            if (changing_marker)
                return;
            final PlotMarker<Double> plot_marker = plot.getMarkers().get(index);
            final WidgetProperty<Double> model_marker = model_widget.propMarkers().getValue().get(index).value();
            final double position = plot_marker.getPosition();
            changing_marker = true;
            model_marker.setValue(position);
            // Was property change reverted (Runtime could not write PV, ..)?
            final double effective = model_marker.getValue();
            if (effective != position)
                plot_marker.setPosition(effective);
            changing_marker = false;
        }

        // When user interactively changes the plot,
        // update the model.
        @Override
        public void changedXAxis(final Axis<Double> x_axis)
        {
            updateModelAxis(model_widget.propXAxis(), x_axis);
        }

        @Override
        public void changedYAxis(final YAxis<Double> y_axis)
        {
            final int index = plot.getYAxes().indexOf(y_axis);
            if (index >= 0  &&  index < model_widget.propYAxes().size())
                updateModelAxis(model_widget.propYAxes().getElement(index), y_axis);
        }

        /** Invoked when auto scale is enabled or disabled by user interaction */
        @Override
        public void changedAutoScale(Axis<?> axis)
        {
            changing_range = true;
            try
            {
                if (axis == plot.getXAxis())
                    model_widget.propXAxis().autoscale().setValue(axis.isAutoscale());
                else
                {
                    final int index = plot.getYAxes().indexOf(axis);
                    if (index >= 0  &&  index < model_widget.propYAxes().size())
                        model_widget.propYAxes().getElement(index).autoscale().setValue(axis.isAutoscale());
                }
            }
            finally
            {
                changing_range = false;
            }
        }

        private void updateModelAxis(final AxisWidgetProperty model_axis, final Axis<Double> plot_axis)
        {
            final AxisRange<Double> range = plot_axis.getValueRange();
            if (changing_range)
            {
                logger.log(Level.WARNING, "Ignoring plot axis change for " + model_axis + " because of additional change");
                return;
            }
            changing_range = true;
            try
            {
                model_axis.minimum().setValue(range.getLow());
                model_axis.maximum().setValue(range.getHigh());
            }
            finally
            {
                changing_range = false;
            }
        }
    };

    /** Handler for one trace of the plot
     *
     *  <p>Updates the plot when the configuration of a trace
     *  or the associated X or Y value in the model changes.
     */
    private class TraceHandler
    {
        private final TraceWidgetProperty model_trace;
        private final UntypedWidgetPropertyListener trace_listener = this::traceChanged,
                                                    value_listener = this::valueChanged;
        private final Trace<Double> trace;

        /** Plot needs a consistent combination of X, Y[, Error]
         *
         *  <p>Assume we receive updates X1, Y1, then a little later X2 and finally Y2.
         *  Updates need to be handled on other thread.
         *  If posting the values to a thread pool, it might handle X2, Y2 _before_ XY1.
         *  Caching the most recent value avoids handling old data.
         */
        private final AtomicReference<XYVTypeDataProvider> latest_data = new AtomicReference<>();

        TraceHandler(final TraceWidgetProperty model_trace)
        {
            this.model_trace = model_trace;

            trace = plot.addTrace(model_trace.traceName().getValue(), "", new XYVTypeDataProvider(),
                                  JFXUtil.convert(model_trace.traceColor().getValue()),
                                  map(model_trace.traceType().getValue()),
                                  model_trace.traceWidth().getValue(),
                                  map(model_trace.tracePointType().getValue()),
                                  model_trace.tracePointSize().getValue(),
                                  model_trace.traceYAxis().getValue());

            model_trace.traceName().addUntypedPropertyListener(trace_listener);
            // Not tracking X and Error PVs. Only matter to runtime.
            // Y PV name is shown in legend, so track that for the editor.
            model_trace.traceYPV().addUntypedPropertyListener(trace_listener);
            model_trace.traceYAxis().addUntypedPropertyListener(trace_listener);
            model_trace.traceType().addUntypedPropertyListener(trace_listener);
            model_trace.traceColor().addUntypedPropertyListener(trace_listener);
            model_trace.traceWidth().addUntypedPropertyListener(trace_listener);
            model_trace.tracePointType().addUntypedPropertyListener(trace_listener);
            model_trace.tracePointSize().addUntypedPropertyListener(trace_listener);
            model_trace.traceXValue().addUntypedPropertyListener(value_listener);
            model_trace.traceYValue().addUntypedPropertyListener(value_listener);
            model_trace.traceErrorValue().addUntypedPropertyListener(value_listener);
        }

        private TraceType map(final PlotWidgetTraceType value)
        {
            // AREA* types create just a line if the input data is
            // a plain array, but will also handle VStatistics
            switch (value)
            {
            case NONE:          return TraceType.NONE;
            case STEP:          return TraceType.AREA;
            case ERRORBAR:      return TraceType.ERROR_BARS;
            case LINE_ERRORBAR: return TraceType.LINES_ERROR_BARS;
            case BARS:          return TraceType.BARS;
            case LINE:
            default:            return TraceType.AREA_DIRECT;
            }
        }

        private PointType map(final PlotWidgetPointType value)
        {   // For now the ordinals match,
            // only different types to keep the Model separate from the Representation
            return PointType.values()[value.ordinal()];
        }

        private void traceChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
        {
            trace.setName(model_trace.traceName().getValue());
            trace.setType(map(model_trace.traceType().getValue()));
            trace.setColor(JFXUtil.convert(model_trace.traceColor().getValue()));
            trace.setWidth(model_trace.traceWidth().getValue());
            trace.setPointType(map(model_trace.tracePointType().getValue()));
            trace.setPointSize(model_trace.tracePointSize().getValue());

            final int desired = model_trace.traceYAxis().getValue();
            if (desired != trace.getYAxis())
                plot.moveTrace(trace, desired);
            plot.requestLayout();
        };

        // PV changed value -> runtime updated X/Y value property -> valueChanged()
        private void valueChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
        {
            final ListNumber x_data, y_data, error;
            final VType y_value = model_trace.traceYValue().getValue();

            if (y_value instanceof VNumberArray)
            {
                final VType x_value = model_trace.traceXValue().getValue();
                x_data = (x_value instanceof VNumberArray) ? ((VNumberArray)x_value).getData() : null;

                final VNumberArray y_array = (VNumberArray)y_value;
                trace.setUnits(y_array.getUnits());
                y_data = y_array.getData();

                final VType error_value = model_trace.traceErrorValue().getValue();
                if (error_value == null)
                    error = null;
                else if (error_value instanceof VNumberArray)
                    error = ((VNumberArray)error_value).getData();
                else
                    error = new ArrayDouble(VTypeUtil.getValueNumber(error_value).doubleValue());
            }
            else // Clear all unless there's Y data
                x_data = y_data = error = XYVTypeDataProvider.EMPTY;

            // Decouple from CAJ's PV thread
            latest_data.set(new XYVTypeDataProvider(x_data, y_data, error));
            toolkit.submit(this::updateData);
        }

        // Update XYPlot data on different thread, not from CAJ callback.
        // Void to be usable as Callable(..) with Exception on error
        private Void updateData() throws Exception
        {
            // Flurry of N updates will schedule N updateData() calls.
            // The first one that actually runs will handle the most
            // recent data and the rest can then return with nothing else to do.
            final XYVTypeDataProvider latest = latest_data.getAndSet(null);
            if (latest != null)
            {
                trace.updateData(latest);
                plot.requestUpdate();
            }
            return null;
        }

        void dispose()
        {
            model_trace.traceName().removePropertyListener(trace_listener);
            model_trace.traceYPV().removePropertyListener(trace_listener);
            model_trace.traceYAxis().removePropertyListener(trace_listener);
            model_trace.traceType().removePropertyListener(trace_listener);
            model_trace.traceColor().removePropertyListener(trace_listener);
            model_trace.traceWidth().removePropertyListener(trace_listener);

            model_trace.tracePointType().removePropertyListener(trace_listener);
            model_trace.tracePointSize().removePropertyListener(trace_listener);
            model_trace.traceXValue().removePropertyListener(value_listener);
            model_trace.traceYValue().removePropertyListener(value_listener);
            model_trace.traceErrorValue().removePropertyListener(value_listener);
            plot.removeTrace(trace);
        }
    };

    private final List<TraceHandler> trace_handlers = new CopyOnWriteArrayList<>();


    @Override
    public Pane createJFXNode() throws Exception
    {
        // Plot is only active in runtime mode, not edit mode
        plot = new RTValuePlot(! toolkit.isEditMode());
        plot.setUpdateThrottle(RepresentationUpdateThrottle.plot_update_delay, TimeUnit.MILLISECONDS);
        plot.showToolbar(false);
        plot.showCrosshair(false);

        // Create PlotMarkers once. Not allowing adding/removing them at runtime
        if (! toolkit.isEditMode())
            for (MarkerProperty marker : model_widget.propMarkers().getValue())
                createMarker(marker);

        return plot;
    }

    private void createMarker(final MarkerProperty model_marker)
    {
        final PlotMarker<Double> plot_marker = plot.addMarker(JFXUtil.convert(model_marker.color().getValue()),
                                                      model_marker.interactive().getValue(),
                                                      model_marker.value().getValue());

        // For now _not_ listening to runtime changes of model_marker.interactive()

        // Listen to model_marker.value(), .. and update plot_marker
        final WidgetPropertyListener<Double> model_marker_listener = (o, old, value) ->
        {
            if (changing_marker)
                return;
            changing_marker = true;
            plot_marker.setPosition(model_marker.value().getValue());
            changing_marker = false;
            plot.requestUpdate();
        };
        model_marker.value().addPropertyListener(model_marker_listener);
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();

        model_widget.propBackground().addUntypedPropertyListener(config_listener);
        model_widget.propForeground().addUntypedPropertyListener(config_listener);
        model_widget.propGridColor().addUntypedPropertyListener(config_listener);
        model_widget.propTitle().addUntypedPropertyListener(config_listener);
        model_widget.propTitleFont().addUntypedPropertyListener(config_listener);
        model_widget.propToolbar().addUntypedPropertyListener(config_listener);
        model_widget.propLegend().addUntypedPropertyListener(config_listener);

        trackAxisChanges(model_widget.propXAxis());

        // Track initial Y axis
        final List<YAxisWidgetProperty> y_axes = model_widget.propYAxes().getValue();
        trackAxisChanges(y_axes.get(0));
        // Create additional Y axes from model
        if (y_axes.size() > 1)
            yAxesChanged(model_widget.propYAxes(), null, y_axes.subList(1, y_axes.size()));
        // Track added/remove Y axes
        model_widget.propYAxes().addPropertyListener(this::yAxesChanged);

        final UntypedWidgetPropertyListener position_listener = this::positionChanged;
        model_widget.propWidth().addUntypedPropertyListener(position_listener);
        model_widget.propHeight().addUntypedPropertyListener(position_listener);

        tracesChanged(model_widget.propTraces(), null, model_widget.propTraces().getValue());
        model_widget.propTraces().addPropertyListener(this::tracesChanged);

        model_widget.runtimePropConfigure().addPropertyListener((p, o, n) -> plot.showConfigurationDialog());

        plot.addListener(plot_listener);
    }

    /** Listen to changed axis properties
     *  @param axis X or Y axis
     */
    private void trackAxisChanges(final AxisWidgetProperty axis)
    {
        axis.title().addUntypedPropertyListener(config_listener);
        axis.autoscale().addUntypedPropertyListener(range_listener);
        axis.minimum().addUntypedPropertyListener(range_listener);
        axis.maximum().addUntypedPropertyListener(range_listener);
        axis.grid().addUntypedPropertyListener(config_listener);
        axis.titleFont().addUntypedPropertyListener(config_listener);
        axis.scaleFont().addUntypedPropertyListener(config_listener);
        if (axis instanceof YAxisWidgetProperty)
        {
            final YAxisWidgetProperty yaxis = (YAxisWidgetProperty) axis;
            yaxis.logscale().addUntypedPropertyListener(config_listener);
            yaxis.visible().addUntypedPropertyListener(config_listener);
        }
    }

    /** Ignore changed axis properties
     *  @param axis X or Y axis
     */
    private void ignoreAxisChanges(final AxisWidgetProperty axis)
    {
        axis.title().removePropertyListener(config_listener);
        axis.autoscale().removePropertyListener(range_listener);
        axis.minimum().removePropertyListener(range_listener);
        axis.maximum().removePropertyListener(range_listener);
        axis.grid().removePropertyListener(config_listener);
        axis.titleFont().removePropertyListener(config_listener);
        axis.scaleFont().removePropertyListener(config_listener);
        if (axis instanceof YAxisWidgetProperty)
            ((YAxisWidgetProperty)axis).logscale().removePropertyListener(config_listener);
    }

    private void yAxesChanged(final WidgetProperty<List<YAxisWidgetProperty>> property,
                              final List<YAxisWidgetProperty> removed, final List<YAxisWidgetProperty> added)
    {
        // Remove axis
        if (removed != null)
        {   // Notification holds the one removed axis, which was the last one
            final AxisWidgetProperty axis = removed.get(0);
            final int index = plot.getYAxes().size()-1;
            ignoreAxisChanges(axis);
            plot.removeYAxis(index);
        }

        // Add missing axes
        // Notification will hold the one added axis,
        // but initial call from registerListeners() will hold all axes to add
        if (added != null)
            for (AxisWidgetProperty axis : added)
            {
                plot.addYAxis(axis.title().getValue());
                trackAxisChanges(axis);
            }
        // Update axis detail: range, ..
        config_listener.propertyChanged(property, removed, added);
    }

    private void positionChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_position.mark();
        toolkit.scheduleUpdate(this);
    }

    private void tracesChanged(final WidgetProperty<List<TraceWidgetProperty>> property,
                               final List<TraceWidgetProperty> removed, final List<TraceWidgetProperty> added)
    {
        final List<TraceWidgetProperty> model_traces = property.getValue();
        int count = trace_handlers.size();
        // Remove extra traces
        while (count > model_traces.size())
            trace_handlers.remove(--count).dispose();
        // Add missing traces
        while (count < model_traces.size())
            trace_handlers.add(new TraceHandler(model_traces.get(count++)));
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_config.checkAndClear())
            updateConfig();
        if (dirty_range.checkAndClear())
            updateRanges();
        if (dirty_position.checkAndClear())
        {
            final int w = model_widget.propWidth().getValue();
            final int h = model_widget.propHeight().getValue();
            plot.setPrefWidth(w);
            plot.setPrefHeight(h);
        }
        plot.requestUpdate();
    }

    private void updateConfig()
    {
        final Color foreground = JFXUtil.convert(model_widget.propForeground().getValue());
        plot.setForeground(foreground);
        plot.getXAxis().setColor(foreground);
        plot.setBackground(JFXUtil.convert(model_widget.propBackground().getValue()));
        plot.setGridColor(JFXUtil.convert(model_widget.propGridColor().getValue()));
        plot.setTitleFont(JFXUtil.convert(model_widget.propTitleFont().getValue()));
        plot.setTitle(model_widget.propTitle().getValue());

        plot.showToolbar(model_widget.propToolbar().getValue());

        // Show trace names either in legend or on axis
        final boolean legend = model_widget.propLegend().getValue();
        plot.showLegend(legend);
        for (YAxis<Double> axis : plot.getYAxes())
            axis.useTraceNames(!legend);

        // Update X Axis
        updateAxisConfig(plot.getXAxis(), model_widget.propXAxis());
        // Use X axis font for legend
        plot.setLegendFont(JFXUtil.convert(model_widget.propXAxis().titleFont().getValue()));

        // Update Y Axes
        final List<YAxisWidgetProperty> model_y = model_widget.propYAxes().getValue();
        if (plot.getYAxes().size() != model_y.size())
        {
            logger.log(Level.WARNING, "Plot has " + plot.getYAxes().size() + " while model has " + model_y.size() + " Y axes");
            return;
        }
        for (int i=0;  i<model_y.size();  ++i)
            updateYAxisConfig(i, model_y.get(i));
    }

    private void updateYAxisConfig(final int index, final YAxisWidgetProperty model_axis)
    {
        final YAxis<Double> plot_axis = plot.getYAxes().get(index);
        updateAxisConfig(plot_axis, model_axis);
        plot_axis.setLogarithmic(model_axis.logscale().getValue());

        // Make axis and all its traces visible resp. not
        final Boolean visible = model_axis.visible().getValue();
        for (Trace<?> trace : plot.getTraces())
            if (trace.getYAxis() == index)
                trace.setVisible(visible);
        final Color foreground = JFXUtil.convert(model_widget.propForeground().getValue());
        plot_axis.setColor(foreground);
        plot_axis.setVisible(visible);
    }

    private void updateAxisConfig(final Axis<Double> plot_axis, final AxisWidgetProperty model_axis)
    {
        plot_axis.setName(model_axis.title().getValue());
        plot_axis.setGridVisible(model_axis.grid().getValue());
        plot_axis.setLabelFont(JFXUtil.convert(model_axis.titleFont().getValue()));
        plot_axis.setScaleFont(JFXUtil.convert(model_axis.scaleFont().getValue()));
    }

    private void updateRanges()
    {
        // Update X Axis
        updateAxisRange(plot.getXAxis(), model_widget.propXAxis());

        // Update Y Axes
        final List<YAxisWidgetProperty> model_y = model_widget.propYAxes().getValue();
        if (plot.getYAxes().size() != model_y.size())
        {
            logger.log(Level.WARNING, "Plot has " + plot.getYAxes().size() + " while model has " + model_y.size() + " Y axes");
            return;
        }
        for (int i=0;  i<model_y.size();  ++i)
            updateAxisRange(plot.getYAxes().get(i), model_y.get(i));
    }

    private void updateAxisRange(final Axis<Double> plot_axis, final AxisWidgetProperty model_axis)
    {
        if (model_axis.autoscale().getValue())
        {   // In auto-scale mode, don't update the value range because that would
            // result in flicker when both we and the auto-scaling adjust the range
            plot_axis.setAutoscale(true);
        }
        else
        {   // No auto-scale requested.
            if (plot_axis.setAutoscale(false))
            {   // Autoscale was on.
                // Turn off, and update model to the last range used by the plot
                final AxisRange<Double> range = plot_axis.getValueRange();
                changing_range = true;
                model_axis.minimum().setValue(range.getLow());
                model_axis.maximum().setValue(range.getHigh());
                changing_range = false;
            }
            else
                plot_axis.setValueRange(model_axis.minimum().getValue(), model_axis.maximum().getValue());
        }
    }

    @Override
    public void dispose()
    {
        plot.dispose();
        super.dispose();
    }
}
