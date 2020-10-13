/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.persistence.XMLPersistence;
import org.csstudio.trends.databrowser3.ui.Perspective;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;

/** Application instance
 *
 *  <p>Runs a {@link Perspective} in a {@link DockItemWithInput}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataBrowserInstance implements AppInstance
{
    public static final ExtensionFilter[] file_extensions = new ExtensionFilter[] { new ExtensionFilter(Messages.FileFilterDesc, "*.plt") };

    /** Width of the display in pixels. Used to scale negative plot_bins */
    public static int display_pixel_width = 0;

    private final DataBrowserApp app;
    private Perspective perspective;
    private DockItemWithInput dock_item;

    /** Track changes that turn the instance 'dirty' **/
    private final ModelListener model_listener = new ModelListener()
    {
        @Override
        public void changedSaveChangesBehavior(final boolean save_changes)
        {   setDirty(true);   }

        @Override
        public void changedTitle()
        {   setDirty(true);   }

        @Override
        public void changedLayout()
        {   setDirty(true);   }

        @Override
        public void changedTiming()
        {   setDirty(true);   }

        @Override
        public void changedArchiveRescale()
        {   setDirty(true);   }

        @Override
        public void changedColorsOrFonts()
        {   setDirty(true);   }

        @Override
        public void changedTimerange()
        {   setDirty(true);   }

        @Override
        public void changedTimeAxisConfig()
        {   setDirty(true);   }

        @Override
        public void changedAxis(final Optional<AxisConfig> axis)
        {   setDirty(true);   }

        @Override
        public void itemAdded(final ModelItem item)
        {   setDirty(true);   }

        @Override
        public void itemRemoved(final ModelItem item)
        {   setDirty(true);   }

        @Override
        public void changedItemVisibility(final ModelItem item)
        {   setDirty(true);   }

        @Override
        public void changedItemLook(final ModelItem item)
        {   setDirty(true);   }

        @Override
        public void changedItemDataConfig(final PVItem item, final boolean archive_invalid)
        {   setDirty(true);   }

        @Override
        public void changedAnnotations()
        {   setDirty(true);   }
    };

    public DataBrowserInstance(final DataBrowserApp app, final boolean minimal)
    {
        this.app = app;

        // Determine width of widest monitor
        if (display_pixel_width == 0)
        {
            for (Screen screen : Screen.getScreens())
            {
                final int width = (int) screen.getBounds().getWidth();
                if (width > display_pixel_width)
                    display_pixel_width = width;
            }
            if (display_pixel_width <= 0)
            {
                logger.log(Level.WARNING, "Cannot determine display pixel width, using 1000");
                display_pixel_width = 1000;
            }
        }

        perspective = new Perspective(minimal);

        dock_item = new DockItemWithInput(this, perspective, null, file_extensions, this::doSave);
        DockPane.getActiveDockPane().addTab(dock_item);

        dock_item.addClosedNotification(this::dispose);

        perspective.getModel().addListener(model_listener);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    /** @return {@link Model} */
    public Model getModel()
    {
        return perspective.getModel();
    }

    /** Raise instance in case another tab is currently visible */
    public void raise()
    {
        dock_item.select();
    }

    void loadResource(final URI input)
    {
        // Set input ASAP so that other requests to open this
        // resource will find this instance and not start
        // another instance
        dock_item.setInput(input);

        // Load files in background job
        JobManager.schedule(Messages.FileLoadJobName, monitor ->
        {
            final Model new_model = new Model();
            try
            {
                // Check for macros
                final Macros macros = new Macros();
                ResourceParser.getQueryItemStream(input)
                              .filter(item -> item.getValue() != null)
                              .forEach(item -> macros.add(item.getKey(),
                                                          item.getValue()));
                new_model.setMacros(macros);

                // Load model from file in background
                // (strip 'query' from input)
                XMLPersistence.load(new_model, ResourceParser.getContent(
                        new URI(input.getScheme(),
                                input.getAuthority(),
                                input.getPath(),
                                null,
                                null)));

                Platform.runLater(() ->
                {
                    try
                    {
                        // On UI thread, update the running model
                        getModel().load(new_model);
                        // That changed the model, so mark as clean
                        // since it matches the file
                        setDirty(false);
                    }
                    catch (Exception ex)
                    {
                        ExceptionDetailsErrorDialog.openError(Messages.Error,
                                Messages.FileLoadErr + input,
                            ex);
                    }
                });
            }
            catch (Exception ex)
            {
                final String message = "Cannot open Data Browser file " + input;
                logger.log(Level.WARNING, message, ex);
                ExceptionDetailsErrorDialog.openError(app.getDisplayName(), Messages.FileOpenErr, ex);
            }
        });
    }

    void setDirty(final boolean dirty)
    {
        final boolean is_dirty = getModel().shouldSaveChanges()  &&  dirty;
        dock_item.setDirty(is_dirty);
    }

    @Override
    public void restore(final Memento memento)
    {
        perspective.restore(memento);
    }

    @Override
    public void save(final Memento memento)
    {
        perspective.save(memento);
    }

    private void doSave(final JobMonitor monitor) throws Exception
    {
        final File file = Objects.requireNonNull(ResourceParser.getFile(dock_item.getInput()));
        try
        (
            final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        )
        {
            XMLPersistence.write(getModel(), out);
        }
    }

    private void dispose()
    {
        perspective.getModel().removeListener(model_listener);
        perspective.dispose();
        perspective = null;
    }
}
