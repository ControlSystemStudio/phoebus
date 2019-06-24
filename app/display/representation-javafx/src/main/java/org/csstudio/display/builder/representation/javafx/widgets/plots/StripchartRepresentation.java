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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.widgets.plots.StripchartWidget;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.ui.Controller;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.epics.util.array.ArrayDouble;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;
import org.phoebus.util.time.TimestampFormats;

import javafx.scene.layout.Pane;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StripchartRepresentation extends RegionBaseRepresentation<Pane, StripchartWidget>
{
    /** Size changed */
    private final DirtyFlag dirty_size = new DirtyFlag();

    /** Other options (toolbar, etc) changed */
    private final DirtyFlag dirty_opts = new DirtyFlag();

    private final WidgetPropertyListener<Integer> sizeChangedListener = this::sizeChanged;
    private final UntypedWidgetPropertyListener optsChangedListener = this::optsChanged;

    /** Data Browser model */
    private final Model model = new Model();

    /** New model to show */
    private final AtomicReference<Model> new_model = new AtomicReference<>();

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


    /** Listener to model's selected sample, updates widget.propSelectionValue() */
    private class ModelSampleSelectionListener implements ModelListener
    {
        @Override
        public void selectedSamplesChanged()
        {
            // Create VTable value from selected samples
            final List<ModelItem> items = model.getItems();
            final List<String> names = new ArrayList<>(items.size());
            final List<String> times = new ArrayList<>(items.size());
            final double[] values = new double[items.size()];
            int i=0;
            for (ModelItem item : items)
            {
                names.add(item.getResolvedDisplayName());
                final Optional<PlotDataItem<Instant>> sample = item.getSelectedSample();
                if (sample.isPresent())
                {
                    times.add(TimestampFormats.MILLI_FORMAT.format(sample.get().getPosition()));
                    values[i++] = sample.get().getValue();
                }
                else
                {
                    times.add("-");
                    values[i++] = Double.NaN;
                }
            }
            final VType value = VTable.of(
                    List.of(String.class, String.class, double.class),
                    List.of("Trace", "Timestamp", "Value"),
                    List.of(names, times, ArrayDouble.of(values)));
            // TODO model_widget.propSelectionValue().setValue(value);
        }
    };

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
        model_widget.propToolbar().addUntypedPropertyListener(optsChangedListener);

        // TODO Initial update

        if (! toolkit.isEditMode())
        {
            model_widget.runtimePropConfigure().addPropertyListener((p, o, n) -> plot.getPlot().showConfigurationDialog());

            // Track selected sample?
            // 'selection_value_pv' must be set when runtime starts,
            // can not be set later
// TODO            if (model_widget.propSelectionValuePVName().getValue().length() > 0)
//                model.addListener(new ModelSampleSelectionListener());
        }
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        model_widget.propToolbar().removePropertyListener(optsChangedListener);
        super.unregisterListeners();
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

    @Override
    public void updateChanges()
    {
        super.updateChanges();

        if (dirty_size.checkAndClear())
            plot.getPlot().setPrefSize(model_widget.propWidth().getValue(),
                                       model_widget.propHeight().getValue());

        // Has new model been loaded in background, to be represented?
        final Model to_load = new_model.getAndSet(null);
        if (to_load != null)
        {
            model.clear();
            try
            {
                model.load(to_load);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot update data browser", ex);
            }
        }

        if (dirty_opts.checkAndClear())
            plot.getPlot().showToolbar(model_widget.propToolbar().getValue());
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
