/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets.plots;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.widgets.plots.StripchartWidget;
import org.csstudio.display.builder.model.widgets.plots.StripchartWidget.AxisWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.StripchartWidget.TraceWidgetProperty;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.ui.Controller;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;

import javafx.scene.layout.Pane;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StripchartRepresentation extends RegionBaseRepresentation<Pane, StripchartWidget>
{
    /** Data Browser model */
    private final Model model = new Model();

    /** Data Browser plot */
    private volatile ModelBasedPlot plot;

    /** Data Browser controller
     *
     *  <p>Controller is always created,
     *  so that plot shows PV names etc.
     *  in edit mode.
     *  It is only started in run mode.
     */
    private volatile Controller controller;

    /** Size changed */
    private final DirtyFlag dirty_size = new DirtyFlag();

    /** Other options (toolbar, etc) changed */
    private final DirtyFlag dirty_opts = new DirtyFlag();

    /** Model (axes, traces) changed */
    private final DirtyFlag dirty_model = new DirtyFlag();

    private final WidgetPropertyListener<Integer> sizeChangedListener = this::sizeChanged;
    private final UntypedWidgetPropertyListener optsChangedListener = this::optsChanged;
    private final UntypedWidgetPropertyListener modelChangedListener = this::modelChanged;
    private final WidgetPropertyListener<List<AxisWidgetProperty>> axes_listener = this::axesChanged;
    private final WidgetPropertyListener<List<TraceWidgetProperty>> traces_listener = this::tracesChanged;
    private final WidgetPropertyListener<Instant> config_dialog_listener = (p, o, n) -> plot.getPlot().showConfigurationDialog();
    private final WidgetPropertyListener<Instant> open_databrowser_listener = (p, o, n) ->
        DataBrowserRepresentation.openFullDataBrowser(model, model_widget.getMacrosOrProperties(), model_widget.propToolbar().getValue());


    @Override
    protected Pane createJFXNode() throws Exception
    {
        plot = new ModelBasedPlot(! toolkit.isEditMode());
        controller = new Controller(model, plot);
        return plot.getPlot();
    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();

        if (! toolkit.isEditMode())
        {
            try
            {
                controller.start();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot start controller", ex);
            }
        }

        model_widget.propWidth().addPropertyListener(sizeChangedListener);
        model_widget.propHeight().addPropertyListener(sizeChangedListener);
        model_widget.propForeground().addUntypedPropertyListener(modelChangedListener);
        model_widget.propBackground().addUntypedPropertyListener(modelChangedListener);
        model_widget.propGrid().addUntypedPropertyListener(modelChangedListener);
        model_widget.propTitle().addUntypedPropertyListener(modelChangedListener);
        model_widget.propTitleFont().addUntypedPropertyListener(modelChangedListener);
        model_widget.propLabelFont().addUntypedPropertyListener(modelChangedListener);
        model_widget.propScaleFont().addUntypedPropertyListener(modelChangedListener);
        model_widget.propToolbar().addUntypedPropertyListener(optsChangedListener);
        model_widget.propLegend().addUntypedPropertyListener(modelChangedListener);
        model_widget.propTimeRange().addUntypedPropertyListener(modelChangedListener);
        model_widget.propYAxes().addPropertyListener(axes_listener);
        model_widget.propTraces().addPropertyListener(traces_listener);

        if (! toolkit.isEditMode())
        {
            model_widget.runtimePropConfigure().addPropertyListener(config_dialog_listener);
            model_widget.runtimePropOpenDataBrowser().addPropertyListener(open_databrowser_listener);
        }

        // Initial update
        for (TraceWidgetProperty trace : model_widget.propTraces().getValue())
            trackTraceChanges(trace);
        axesChanged(null, null, model_widget.propYAxes().getValue());
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        model_widget.propForeground().removePropertyListener(modelChangedListener);
        model_widget.propBackground().removePropertyListener(modelChangedListener);
        model_widget.propGrid().removePropertyListener(modelChangedListener);
        model_widget.propTitle().removePropertyListener(modelChangedListener);
        model_widget.propTitleFont().removePropertyListener(modelChangedListener);
        model_widget.propLabelFont().removePropertyListener(modelChangedListener);
        model_widget.propScaleFont().removePropertyListener(modelChangedListener);
        model_widget.propToolbar().removePropertyListener(optsChangedListener);
        model_widget.propLegend().removePropertyListener(modelChangedListener);
        model_widget.propTimeRange().removePropertyListener(modelChangedListener);
        model_widget.propYAxes().removePropertyListener(axes_listener);

        if (! toolkit.isEditMode())
        {
            model_widget.runtimePropConfigure().removePropertyListener(config_dialog_listener);
            model_widget.runtimePropOpenDataBrowser().removePropertyListener(open_databrowser_listener);
        }

        super.unregisterListeners();
    }

    private void axesChanged(final WidgetProperty<List<AxisWidgetProperty>> prop, final List<AxisWidgetProperty> removed, final List<AxisWidgetProperty> added)
    {
        // Track/ignore axes
        if (removed != null)
            for (AxisWidgetProperty axis : removed)
                ignoreAxisChanges(axis);

        if (added != null)
            for (AxisWidgetProperty axis : added)
                trackAxisChanges(axis);

        // Anything changed -> Update complete model
        modelChanged(null, null, null);
    }

    private void trackAxisChanges(final AxisWidgetProperty axis)
    {
        axis.title().addUntypedPropertyListener(modelChangedListener);
        axis.autoscale().addUntypedPropertyListener(modelChangedListener);
        axis.logscale().addUntypedPropertyListener(modelChangedListener);
        axis.minimum().addUntypedPropertyListener(modelChangedListener);
        axis.maximum().addUntypedPropertyListener(modelChangedListener);
        axis.grid().addUntypedPropertyListener(modelChangedListener);
        axis.visible().addUntypedPropertyListener(modelChangedListener);
    }

    private void ignoreAxisChanges(final AxisWidgetProperty axis)
    {
        axis.title().removePropertyListener(modelChangedListener);
        axis.autoscale().removePropertyListener(modelChangedListener);
        axis.logscale().removePropertyListener(modelChangedListener);
        axis.minimum().removePropertyListener(modelChangedListener);
        axis.maximum().removePropertyListener(modelChangedListener);
        axis.grid().removePropertyListener(modelChangedListener);
        axis.visible().removePropertyListener(modelChangedListener);
    }


    private void tracesChanged(final WidgetProperty<List<TraceWidgetProperty>> prop, final List<TraceWidgetProperty> removed, final List<TraceWidgetProperty> added)
    {
        // Track/ignore axes
        if (removed != null)
            for (TraceWidgetProperty trace : removed)
                ignoreTraceChanges(trace);

        if (added != null)
            for (TraceWidgetProperty trace : added)
                trackTraceChanges(trace);

        // Anything changed -> Update complete model
        modelChanged(null, null, null);
    }

    private void trackTraceChanges(final TraceWidgetProperty trace)
    {
        trace.traceName().addUntypedPropertyListener(modelChangedListener);
        trace.traceYPV().addUntypedPropertyListener(modelChangedListener);
        trace.traceYAxis().addUntypedPropertyListener(modelChangedListener);
        trace.traceType().addUntypedPropertyListener(modelChangedListener);
        trace.traceColor().addUntypedPropertyListener(modelChangedListener);
        trace.traceWidth().addUntypedPropertyListener(modelChangedListener);
        trace.tracePointType().addUntypedPropertyListener(modelChangedListener);
        trace.tracePointSize().addUntypedPropertyListener(modelChangedListener);
        trace.traceVisible().addUntypedPropertyListener(modelChangedListener);
    }

    private void ignoreTraceChanges(final TraceWidgetProperty trace)
    {
        trace.traceName().removePropertyListener(modelChangedListener);
        trace.traceYPV().removePropertyListener(modelChangedListener);
        trace.traceYAxis().removePropertyListener(modelChangedListener);
        trace.traceType().removePropertyListener(modelChangedListener);
        trace.traceColor().removePropertyListener(modelChangedListener);
        trace.traceWidth().removePropertyListener(modelChangedListener);
        trace.tracePointType().removePropertyListener(modelChangedListener);
        trace.tracePointSize().removePropertyListener(modelChangedListener);
        trace.traceVisible().removePropertyListener(modelChangedListener);
    }

    private void sizeChanged(final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    /** @return {@link Model} of the data browser (samples, ...) */
    public Model getDataBrowserModel()
    {
        return model;
    }

    private void optsChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_opts.mark();
        toolkit.scheduleUpdate(this);
    }

    private void modelChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_model.mark();
        toolkit.scheduleUpdate(this);
    }

    private void updateModel()
    {
        model.setPlotForeground(JFXUtil.convert(model_widget.propForeground().getValue()));
        model.setPlotBackground(JFXUtil.convert(model_widget.propBackground().getValue()));
        model.setGridVisible(model_widget.propGrid().getValue());
        model.setTitle(model_widget.propTitle().getValue());
        model.setTitleFont(JFXUtil.convert(model_widget.propTitleFont().getValue()));
        model.setLabelFont(JFXUtil.convert(model_widget.propLabelFont().getValue()));
        model.setScaleFont(JFXUtil.convert(model_widget.propScaleFont().getValue()));

        final TemporalAmount rel_start = TimeParser.parseTemporalAmount(model_widget.propTimeRange().getValue());
        model.setTimerange(TimeRelativeInterval.startsAt(rel_start));

        final boolean show_legend = model_widget.propLegend().getValue();
        model.setLegendVisible(show_legend);

        // Value Axes
        int index = 0;
        final List<AxisWidgetProperty> axes = model_widget.propYAxes().getValue();
        while (model.getAxisCount() > axes.size())
            model.removeAxis(model.getAxis(0));
        for (AxisWidgetProperty axis : axes)
        {
            final AxisConfig config;
            if (index < model.getAxisCount())
                config = model.getAxis(index);
            else
                config = model.addAxis();
            config.useAxisName(! axis.title().getValue().isEmpty());
            config.useTraceNames(! show_legend);
            config.setName(axis.title().getValue());
            config.setRange(axis.minimum().getValue(), axis.maximum().getValue());
            config.setAutoScale(axis.autoscale().getValue());
            config.setLogScale(axis.logscale().getValue());
            config.setGridVisible(axis.grid().getValue());
            config.setVisible(axis.visible().getValue());
            ++index;
        }

        // PV traces
        final List<ModelItem> items = model.getItems();
        for (int i=items.size()-1;  i>=0;  --i)
            model.removeItem(items.get(i));

        for (TraceWidgetProperty trace : model_widget.propTraces().getValue())
        {
            if (! trace.traceVisible().getValue())
                continue;

            final PVItem item = new PVItem(trace.traceYPV().getValue(), 0.0);
            item.setDisplayName(trace.traceName().getValue());
            item.setAxis(model.getAxis(trace.traceYAxis().getValue()));
            item.setTraceType(XYPlotRepresentation.map(trace.traceType().getValue()));
            item.setColor(JFXUtil.convert(trace.traceColor().getValue()));
            item.setLineWidth(trace.traceWidth().getValue());
            item.setPointType(XYPlotRepresentation.map(trace.tracePointType().getValue()));
            item.setPointSize(trace.tracePointSize().getValue());
            try
            {
                model.addItem(item);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot add trace to strip chart", ex);
            }
        }
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();

        if (dirty_model.checkAndClear())
            updateModel();

        if (dirty_size.checkAndClear())
            plot.getPlot().setPrefSize(model_widget.propWidth().getValue(),
                                       model_widget.propHeight().getValue());

        if (dirty_opts.checkAndClear())
        {
            plot.getPlot().showToolbar(model_widget.propToolbar().getValue());
        }
    }

    @Override
    public void dispose()
    {
        if (controller != null  &&  controller.isRunning())
        {
            controller.stop();
            controller = null;
        }
        super.dispose();
        if (plot != null)
        {
            plot.dispose();
            plot = null;
        }
    }
}
