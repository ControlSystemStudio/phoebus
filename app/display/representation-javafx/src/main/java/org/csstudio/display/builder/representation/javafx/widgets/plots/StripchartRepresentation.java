/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets.plots;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.widgets.plots.StripchartWidget;
import org.csstudio.display.builder.model.widgets.plots.StripchartWidget.AxisWidgetProperty;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
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
        model_widget.propLegend().addUntypedPropertyListener(optsChangedListener);
        model_widget.propTimeRange().addUntypedPropertyListener(modelChangedListener);
        model_widget.propYAxes().addPropertyListener(axes_listener);

        if (! toolkit.isEditMode())
        {
            model_widget.runtimePropConfigure().addPropertyListener((p, o, n) -> plot.getPlot().showConfigurationDialog());
        }

        // Initial update
        axesChanged(null, null, model_widget.propYAxes().getValue());
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        model_widget.propToolbar().removePropertyListener(optsChangedListener);

        // TODO Unregister all the listeners

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
        System.out.println("model changed: " + property);
        dirty_model.mark();
        toolkit.scheduleUpdate(this);
    }

    private void updateModel()
    {
        // TODO Monitor traces. When trace added, set its color?

        model.setPlotForeground(JFXUtil.convert(model_widget.propForeground().getValue()));
        model.setPlotBackground(JFXUtil.convert(model_widget.propBackground().getValue()));
        model.setGridVisible(model_widget.propGrid().getValue());
        model.setTitle(model_widget.propTitle().getValue());
        model.setTitleFont(JFXUtil.convert(model_widget.propTitleFont().getValue()));
        model.setLabelFont(JFXUtil.convert(model_widget.propLabelFont().getValue()));
        model.setScaleFont(JFXUtil.convert(model_widget.propScaleFont().getValue()));

        final TemporalAmount rel_start = TimeParser.parseTemporalAmount(model_widget.propTimeRange().getValue());
        model.setTimerange(TimeRelativeInterval.startsAt(rel_start));

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
            config.setName(axis.title().getValue());
            config.setRange(axis.minimum().getValue(), axis.maximum().getValue());
            config.setAutoScale(axis.autoscale().getValue());
            config.setLogScale(axis.logscale().getValue());
            config.setGridVisible(axis.grid().getValue());
            config.setVisible(axis.visible().getValue());
            ++index;
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
            model.setLegendVisible(model_widget.propLegend().getValue());
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
