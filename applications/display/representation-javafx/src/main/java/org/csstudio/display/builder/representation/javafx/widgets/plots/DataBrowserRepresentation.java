/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets.plots;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.model.widgets.plots.DataBrowserWidget;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.persistence.XMLPersistence;
import org.csstudio.trends.databrowser3.ui.Controller;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;

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

    /** Data Browser model */
    private final Model model = new Model();

    /** New model to show */
    private final AtomicReference<Model> new_model = new AtomicReference<>();

    /** Data Browser plot */
    private volatile ModelBasedPlot plot;

    /** Data Browser controller */
    private volatile Controller controller;

    @Override
    protected Pane createJFXNode() throws Exception
    {
        // plot = new ModelBasedPlot(! toolkit.isEditMode());
        plot = new ModelBasedPlot(true);
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

        controller = new Controller(model, plot);
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

        model_widget.propWidth().addPropertyListener(this::sizeChanged);
        model_widget.propHeight().addPropertyListener(this::sizeChanged);

        // Not monitoring macros.
        // Macros are read when the file property updates
        model_widget.propFile().addPropertyListener(this::fileChanged);

        model_widget.propShowToolbar().addUntypedPropertyListener(this::optsChanged);

        // Initial update
        ModelThreadPool.getExecutor().execute(() -> fileChanged(null, null, model_widget.propFile().getValue()));
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
        new_model.set(db_model);
        toolkit.scheduleUpdate(this);
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

        final Model to_load = new_model.getAndSet(null);
        if (to_load != null)
        {
            model.clear();
            try
            {
                model.load(to_load);
//                plot.removeAll();
//                int i=0;
//                for (AxisConfig axis : model.getAxes())
//                    plot.updateAxis(i++, axis);
//                for (ModelItem item : model.getItems())
//                    plot.addTrace(item);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot update data browser", ex);
            }
        }

        if (dirty_opts.checkAndClear())
            plot.getPlot().showToolbar(model_widget.propShowToolbar().getValue());
    }

    @Override
    public void dispose()
    {
        super.dispose();
        if (controller != null  &&  controller.isRunning())
            controller.stop();
        if (plot != null)
            plot.dispose();
    }
}
