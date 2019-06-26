/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets.plots;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.model.widgets.plots.DataBrowserWidget;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.trends.databrowser3.DataBrowserApp;
import org.csstudio.trends.databrowser3.DataBrowserInstance;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.persistence.XMLPersistence;
import org.csstudio.trends.databrowser3.ui.Controller;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.epics.util.array.ArrayDouble;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.util.time.TimestampFormats;

import javafx.scene.layout.Pane;

/** Creates JavaFX item for model widget
 *  @author Megan Grodowitz Original databrowser3.bobwidget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataBrowserRepresentation extends RegionBaseRepresentation<Pane, DataBrowserWidget>
{
    /** Size changed */
    private final DirtyFlag dirty_size = new DirtyFlag();

    /** Other options (toolbar, etc) changed */
    private final DirtyFlag dirty_opts = new DirtyFlag();

    private final WidgetPropertyListener<Integer> sizeChangedListener = this::sizeChanged;
    private final UntypedWidgetPropertyListener optsChangedListener = this::optsChanged;
    private final WidgetPropertyListener<String> fileChangedListener = this::fileChanged;

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
            model_widget.propSelectionValue().setValue(value);
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
        // Not monitoring macros.
        // Macros are read when the file property updates
        model_widget.propFile().addPropertyListener(fileChangedListener);
        model_widget.propShowToolbar().addUntypedPropertyListener(optsChangedListener);

        // Initial update
        final String filename = model_widget.propFile().getValue();
        if (! filename.isEmpty())
            ModelThreadPool.getExecutor().execute(() -> fileChanged(null, null, filename));

        if (! toolkit.isEditMode())
        {
            model_widget.runtimePropConfigure().addPropertyListener((p, o, n) -> plot.getPlot().showConfigurationDialog());
            model_widget.runtimePropOpenFull().addPropertyListener((p, o, n) ->
                openFullDataBrowser(model, model_widget.getMacrosOrProperties(), model_widget.propShowToolbar().getValue()));

            // Track selected sample?
            // 'selection_value_pv' must be set when runtime starts,
            // can not be set later
            if (model_widget.propSelectionValuePVName().getValue().length() > 0)
                model.addListener(new ModelSampleSelectionListener());
        }
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        model_widget.propShowToolbar().removePropertyListener(optsChangedListener);
        model_widget.propFile().removePropertyListener(fileChangedListener);
        super.unregisterListeners();
    }


    private void sizeChanged(final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    private void fileChanged(final WidgetProperty<String> property, final String old_value, final String new_value)
    {
        ModelThreadPool.getExecutor().execute(this::loadModel);
    }

    private void loadModel()
    {
        final Model db_model = new Model();
        try
        {
            // Resolve file relative to the source widget model (not 'top'!)
            // Get the display model from the widget tied to this representation
            final DisplayModel display = model_widget.getDisplayModel();
            final String filename = model_widget.propFile().getValue();
            if (! filename.isEmpty())
            {
                final String resource = ModelResourceUtil.resolveResource(display, filename);
                try
                (
                    final InputStream stream = ModelResourceUtil.openResourceStream(resource);
                )
                {
                    XMLPersistence.load(db_model, stream);
                }
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot load data browser " + model_widget.propFile(), ex);
        }
        // Override settings in *.plt file with those of widget
        db_model.setToolbarVisible(model_widget.propShowToolbar().getValue());
        db_model.setMacros(model_widget.getMacrosOrProperties());
        // Set 'new_model'. Plot will be updated on UI thread
        new_model.set(db_model);
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
            plot.getPlot().showToolbar(model_widget.propShowToolbar().getValue());
    }

    static void openFullDataBrowser(final Model model, final MacroValueProvider macros, final boolean show_toolbar)
    {
        try
        {
            final DataBrowserApp app = ApplicationService.findApplication("databrowser");
            final DataBrowserInstance instance = app.create(true);
            // Clone model via in-memory XML representation
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            XMLPersistence.write(model, out);
            final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            final Model clone = new Model();
            XMLPersistence.load(clone, in);
            // Override settings in *.plt file with those of widget
            clone.setToolbarVisible(show_toolbar);
            clone.setMacros(macros);
            instance.getModel().load(clone);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot open data browser", ex);
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
