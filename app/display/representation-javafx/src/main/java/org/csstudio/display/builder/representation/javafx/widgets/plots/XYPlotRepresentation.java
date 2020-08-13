/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets.plots;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetPointType;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.AxisWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.TraceWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetTraceType;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget.MarkerProperty;
import org.csstudio.display.builder.representation.Preferences;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.Messages;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.csstudio.javafx.rtplot.Activator;
import org.csstudio.javafx.rtplot.Axis;
import org.csstudio.javafx.rtplot.AxisRange;
import org.csstudio.javafx.rtplot.LineStyle;
import org.csstudio.javafx.rtplot.PlotMarker;
import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.RTPlotListener;
import org.csstudio.javafx.rtplot.RTValuePlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.YAxis;
import org.csstudio.javafx.rtplot.internal.NumericAxis;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ListNumber;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.ui.javafx.UpdateThrottle;

import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XYPlotRepresentation extends RegionBaseRepresentation<Pane, XYPlotWidget>
{
    /** Plot */
    private RTValuePlot plot;

    private final DirtyFlag dirty_position = new DirtyFlag();
    private final DirtyFlag dirty_range = new DirtyFlag();
    private final DirtyFlag dirty_config = new DirtyFlag();
    private final WidgetPropertyListener<List<AxisWidgetProperty>> yaxes_listener = this::yAxesChanged;
    private final UntypedWidgetPropertyListener position_listener = this::positionChanged;
    private final WidgetPropertyListener<List<TraceWidgetProperty>> traces_listener = this::tracesChanged;
    private final WidgetPropertyListener<Instant> configure_listener = (p, o, n) -> plot.showConfigurationDialog();

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

    private volatile boolean changing_marker = false;

    static TraceType map(final PlotWidgetTraceType value)
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

    static PointType map(final PlotWidgetPointType value)
    {   // For now the ordinals match,
        // only different types to keep the Model separate from the Representation
        return PointType.values()[value.ordinal()];
    }

    static LineStyle map(final org.csstudio.display.builder.model.properties.LineStyle value)
    {   // For now the ordinals match,
        // only different types to keep the Model separate from the Representation
        return LineStyle.values()[value.ordinal()];
    }

    private final RTPlotListener<Double> plot_listener = new RTPlotListener<>()
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

        public void changedLogarithmic(final Axis<?> y_axis)
        {
            final int index = plot.getYAxes().indexOf(y_axis);
            if (index >= 0  &&  index < model_widget.propYAxes().size())
                model_widget.propYAxes().getElement(index).logscale().setValue(((YAxis<?>)y_axis).isLogarithmic());
        };

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
        // Throttle this trace's x, y, error value changes
        private final UpdateThrottle throttle = new UpdateThrottle(Preferences.plot_update_delay, TimeUnit.MILLISECONDS, this::computeTrace,  Activator.thread_pool);

        TraceHandler(final TraceWidgetProperty model_trace)
        {
            this.model_trace = model_trace;

            trace = plot.addTrace(model_trace.traceName().getValue(), "", new XYVTypeDataProvider(),
                                  JFXUtil.convert(model_trace.traceColor().getValue()),
                                  map(model_trace.traceType().getValue()),
                                  model_trace.traceWidth().getValue(),
                                  map(model_trace.traceLineStyle().getValue()),
                                  map(model_trace.tracePointType().getValue()),
                                  model_trace.tracePointSize().getValue(),
                                  model_trace.traceYAxis().getValue());
            trace.setVisible(model_trace.traceVisible().getValue());

            model_trace.traceName().addUntypedPropertyListener(trace_listener);
            // Not tracking X and Error PVs. Only matter to runtime.
            // Y PV name is shown in legend, so track that for the editor.
            model_trace.traceYPV().addUntypedPropertyListener(trace_listener);
            model_trace.traceYAxis().addUntypedPropertyListener(trace_listener);
            model_trace.traceType().addUntypedPropertyListener(trace_listener);
            model_trace.traceColor().addUntypedPropertyListener(trace_listener);
            model_trace.traceWidth().addUntypedPropertyListener(trace_listener);
            model_trace.traceLineStyle().addUntypedPropertyListener(trace_listener);
            model_trace.tracePointType().addUntypedPropertyListener(trace_listener);
            model_trace.tracePointSize().addUntypedPropertyListener(trace_listener);
            model_trace.traceXValue().addUntypedPropertyListener(value_listener);
            model_trace.traceYValue().addUntypedPropertyListener(value_listener);
            model_trace.traceErrorValue().addUntypedPropertyListener(value_listener);
            model_trace.traceVisible().addUntypedPropertyListener(trace_listener);
        }

        private void traceChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
        {
            // Changed trace name requires layout of axes and legend
            boolean need_layout = trace.setName(model_trace.traceName().getValue());
            trace.setType(map(model_trace.traceType().getValue()));
            trace.setColor(JFXUtil.convert(model_trace.traceColor().getValue()));
            trace.setWidth(model_trace.traceWidth().getValue());
            trace.setLineStyle(map(model_trace.traceLineStyle().getValue()));
            trace.setPointType(map(model_trace.tracePointType().getValue()));
            trace.setPointSize(model_trace.tracePointSize().getValue());
            trace.setVisible(model_trace.traceVisible().getValue());

            final int desired = model_trace.traceYAxis().getValue();
            if (desired != trace.getYAxis())
                plot.moveTrace(trace, desired);
            if (need_layout)
                plot.requestCompleteUpdate();
            else
                plot.requestUpdate();
        }

        // PV changed value -> runtime updated X/Y value property -> valueChanged()
        private void valueChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
        {
            logger.log(Level.FINE, () -> model_widget.getName() + " " + property.getName() + " on " + Thread.currentThread());
            // Trigger computeTrace()
            throttle.trigger();
        }

        // Called by throttle
        private void computeTrace()
        {
            // Already disposed?
            if (model_widget == null)
                return;

            final ListNumber x_data, y_data, error;
            final VType y_value = model_trace.traceYValue().getValue();

            if (y_value instanceof VNumberArray)
            {
                final VType x_value = model_trace.traceXValue().getValue();
                x_data = (x_value instanceof VNumberArray) ? ((VNumberArray)x_value).getData() : null;

                final VNumberArray y_array = (VNumberArray)y_value;
                trace.setUnits(y_array.getDisplay().getUnit());
                y_data = y_array.getData();

                final VType error_value = model_trace.traceErrorValue().getValue();
                if (error_value == null)
                    error = null;
                else if (error_value instanceof VNumberArray)
                    error = ((VNumberArray)error_value).getData();
                else
                    error = ArrayDouble.of(VTypeUtil.getValueNumber(error_value).doubleValue());
            }
            else if (y_value instanceof VNumber)
            {
                final VType x_value = model_trace.traceXValue().getValue();
                x_data = (x_value instanceof VNumber) ? ArrayDouble.of(((VNumber)x_value).getValue().doubleValue()) : null;

                final VNumber y_array = (VNumber)y_value;
                trace.setUnits(y_array.getDisplay().getUnit());
                y_data = ArrayDouble.of(y_array.getValue().doubleValue());

                final VType error_value = model_trace.traceErrorValue().getValue();
                if (error_value == null)
                    error = null;
                else
                    error = ArrayDouble.of(VTypeUtil.getValueNumber(error_value).doubleValue());
            }
            else
            {   // No Y Data.
                // Do we have X data?
                final VType x_value = model_trace.traceXValue().getValue();
                if (x_value instanceof VNumberArray)
                {
                    x_data = ((VNumberArray)x_value).getData();
                    y_data = error = null;
                }
                else if (x_value instanceof VNumber)
                {
                    x_data = ArrayDouble.of(((VNumber)x_value).getValue().doubleValue());
                    y_data = error = null;
                }
                else
                {   // Neither X nor Y data
                    x_data = y_data = error = XYVTypeDataProvider.EMPTY;
                }
            }

            logger.log(Level.FINE, () ->
            {
                final StringBuilder buf = new StringBuilder();
                buf.append(model_widget.getName()).append(" update ");
                buf.append("X: ");
                describeData(buf, x_data);
                buf.append(", Y: ");
                describeData(buf, y_data);
                return buf.toString();
            });

            // Wrap as PlotDataProvider
            final XYVTypeDataProvider latest = new XYVTypeDataProvider(x_data, y_data, error);
            trace.updateData(latest);
            plot.requestUpdate();
        }

        private void describeData(final StringBuilder buf, final ListNumber array)
        {
            if (array == null)
                buf.append("null");
            else
            {
                final int N = array.size();
                double min = Double.NaN, max = Double.NaN;
                buf.append(N).append(" samples ");
                for (int i=0; i<N; ++i)
                {
                    final double val = array.getDouble(i);
                    if (i==0 || val < min)
                        min = val;
                    if (i==0 || val > max)
                        max = val;
                }
                buf.append(min).append(" to ").append(max);
            }
        }

        void dispose()
        {
            throttle.dispose();
            model_trace.traceName().removePropertyListener(trace_listener);
            model_trace.traceYPV().removePropertyListener(trace_listener);
            model_trace.traceYAxis().removePropertyListener(trace_listener);
            model_trace.traceType().removePropertyListener(trace_listener);
            model_trace.traceColor().removePropertyListener(trace_listener);
            model_trace.traceWidth().removePropertyListener(trace_listener);
            model_trace.traceLineStyle().removePropertyListener(trace_listener);
            model_trace.tracePointType().removePropertyListener(trace_listener);
            model_trace.tracePointSize().removePropertyListener(trace_listener);
            model_trace.traceXValue().removePropertyListener(value_listener);
            model_trace.traceYValue().removePropertyListener(value_listener);
            model_trace.traceErrorValue().removePropertyListener(value_listener);
            plot.removeTrace(trace);
        }
    }

    private final List<TraceHandler> trace_handlers = new CopyOnWriteArrayList<>();


    @Override
    public Pane createJFXNode() throws Exception
    {
        // Plot is only active in runtime mode, not edit mode
        plot = new RTValuePlot(! toolkit.isEditMode());
        plot.setUpdateThrottle(Preferences.plot_update_delay, TimeUnit.MILLISECONDS);
        plot.showToolbar(false);
        plot.showCrosshair(false);
        plot.setManaged(false);

        // Create PlotMarkers once. Not allowing adding/removing them at runtime
        if (! toolkit.isEditMode())
            for (MarkerProperty marker : model_widget.propMarkers().getValue())
                createMarker(marker);

        // Add button to reset ranges of X and Y axes to the values when plot was first rendered.
        Button resetAxisRanges =
                plot.addToolItem(JFXUtil.getIcon("reset_axis_ranges.png"), Messages.Reset_Axis_Ranges);

        resetAxisRanges.setOnMouseClicked(me -> {
            plot.resetAxisRanges();
        });

        return plot;
    }

    private void createMarker(final MarkerProperty model_marker)
    {
        final PlotMarker<Double> plot_marker = plot.addMarker(JFXUtil.convert(model_marker.color().getValue()),
                                                              model_marker.interactive().getValue(),
                                                              model_marker.value().getValue());

        // Listen to model_marker.value(), interactive() .. and update plot_marker
        final UntypedWidgetPropertyListener model_marker_listener = (o, old, value) ->
        {
            if (changing_marker)
                return;
            changing_marker = true;
            plot_marker.setInteractive(model_marker.interactive().getValue());
            plot_marker.setPosition(model_marker.value().getValue());
            changing_marker = false;
            plot.requestUpdate();
        };
        model_marker.value().addUntypedPropertyListener(model_marker_listener);
        model_marker.interactive().addUntypedPropertyListener(model_marker_listener);
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
        final List<AxisWidgetProperty> y_axes = model_widget.propYAxes().getValue();
        trackAxisChanges(y_axes.get(0));
        // Create additional Y axes from model
        if (y_axes.size() > 1)
            yAxesChanged(model_widget.propYAxes(), null, y_axes.subList(1, y_axes.size()));
        // Track added/remove Y axes
        model_widget.propYAxes().addPropertyListener(yaxes_listener);

        model_widget.propWidth().addUntypedPropertyListener(position_listener);
        model_widget.propHeight().addUntypedPropertyListener(position_listener);

        tracesChanged(model_widget.propTraces(), null, model_widget.propTraces().getValue());
        model_widget.propTraces().addPropertyListener(traces_listener);

        model_widget.runtimePropConfigure().addPropertyListener(configure_listener);

        plot.addListener(plot_listener);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propBackground().removePropertyListener(config_listener);
        model_widget.propForeground().removePropertyListener(config_listener);
        model_widget.propGridColor().removePropertyListener(config_listener);
        model_widget.propTitle().removePropertyListener(config_listener);
        model_widget.propTitleFont().removePropertyListener(config_listener);
        model_widget.propToolbar().removePropertyListener(config_listener);
        model_widget.propLegend().removePropertyListener(config_listener);

        ignoreAxisChanges(model_widget.propXAxis());
        final List<AxisWidgetProperty> y_axes = model_widget.propYAxes().getValue();
        for (AxisWidgetProperty axis : y_axes)
            ignoreAxisChanges(axis);
        model_widget.propYAxes().removePropertyListener(yaxes_listener);

        model_widget.propWidth().removePropertyListener(position_listener);
        model_widget.propHeight().removePropertyListener(position_listener);

        tracesChanged(model_widget.propTraces(), model_widget.propTraces().getValue(), null);
        model_widget.propTraces().removePropertyListener(traces_listener);

        model_widget.runtimePropConfigure().removePropertyListener(configure_listener);

        plot.removeListener(plot_listener);
        super.unregisterListeners();
    }

    /** Listen to changed axis properties
     *  @param axis X or Y axis
     */
    private void trackAxisChanges(final AxisWidgetProperty axis)
    {
        axis.title().addUntypedPropertyListener(config_listener);
        axis.autoscale().addUntypedPropertyListener(range_listener);
        axis.logscale().addUntypedPropertyListener(config_listener);
        axis.minimum().addUntypedPropertyListener(range_listener);
        axis.maximum().addUntypedPropertyListener(range_listener);
        axis.grid().addUntypedPropertyListener(config_listener);
        axis.titleFont().addUntypedPropertyListener(config_listener);
        axis.scaleFont().addUntypedPropertyListener(config_listener);
        axis.visible().addUntypedPropertyListener(config_listener);
    }

    /** Ignore changed axis properties
     *  @param axis X or Y axis
     */
    private void ignoreAxisChanges(final AxisWidgetProperty axis)
    {
        axis.title().removePropertyListener(config_listener);
        axis.autoscale().removePropertyListener(range_listener);
        axis.logscale().removePropertyListener(config_listener);
        axis.minimum().removePropertyListener(range_listener);
        axis.maximum().removePropertyListener(range_listener);
        axis.grid().removePropertyListener(config_listener);
        axis.titleFont().removePropertyListener(config_listener);
        axis.scaleFont().removePropertyListener(config_listener);
        axis.visible().removePropertyListener(config_listener);
    }

    private void yAxesChanged(final WidgetProperty<List<AxisWidgetProperty>> property,
                              final List<AxisWidgetProperty> removed, final List<AxisWidgetProperty> added)
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
            plot.resize(w, h);
        }
        plot.requestUpdate();
    }

    private void updateConfig()
    {
        final Color foreground = JFXUtil.convert(model_widget.propForeground().getValue());
        plot.setForeground(foreground);
        plot.setBackground(JFXUtil.convert(model_widget.propBackground().getValue()));
        plot.setGridColor(JFXUtil.convert(model_widget.propGridColor().getValue()));
        plot.setTitleFont(JFXUtil.convert(model_widget.propTitleFont().getValue()));
        plot.setTitle(model_widget.propTitle().getValue());

        plot.showToolbar(model_widget.propToolbar().getValue());

        // Show trace names either in legend or on axis
        final boolean legend = model_widget.propLegend().getValue();
        plot.showLegend(legend);

        // Update X Axis
        updateAxisConfig(plot.getXAxis(), model_widget.propXAxis());
        // Use X axis font for legend
        plot.setLegendFont(JFXUtil.convert(model_widget.propXAxis().titleFont().getValue()));

        // Update Y Axes
        final List<AxisWidgetProperty> model_y = model_widget.propYAxes().getValue();
        if (plot.getYAxes().size() != model_y.size())
        {
            logger.log(Level.WARNING, "Plot has " + plot.getYAxes().size() + " while model has " + model_y.size() + " Y axes");
            return;
        }
        int i = 0;
        for (YAxis<Double> plot_axis : plot.getYAxes())
        {
            plot_axis.useTraceNames(!legend);
            updateAxisConfig(plot_axis, model_y.get(i));
            ++i;
        }
    }

    private void updateAxisConfig(final Axis<Double> plot_axis, final AxisWidgetProperty model_axis)
    {
        plot_axis.setName(model_axis.title().getValue());
        final Color foreground = JFXUtil.convert(model_widget.propForeground().getValue());
        plot_axis.setColor(foreground);
        if (plot_axis instanceof NumericAxis)
            ((NumericAxis)plot_axis).setLogarithmic(model_axis.logscale().getValue());
        plot_axis.setGridVisible(model_axis.grid().getValue());
        plot_axis.setLabelFont(JFXUtil.convert(model_axis.titleFont().getValue()));
        plot_axis.setScaleFont(JFXUtil.convert(model_axis.scaleFont().getValue()));
        plot_axis.setVisible(model_axis.visible().getValue());
    }

    private void updateRanges()
    {
        // Update X Axis
        updateAxisRange(plot.getXAxis(), model_widget.propXAxis());

        // Update Y Axes
        final List<AxisWidgetProperty> model_y = model_widget.propYAxes().getValue();
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
        super.dispose();
        plot.dispose();
        plot = null;
    }
}
