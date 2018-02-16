/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
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
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.stage.FileChooser.ExtensionFilter;

/** Application instance
 *
 *  <p>Runs a {@link Perspective} in a {@link DockItemWithInput}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataBrowserInstance implements AppInstance
{
    public static final ExtensionFilter[] file_extensions = new ExtensionFilter[] { new ExtensionFilter("Data Browser", "*.plt") };

    private final DataBrowserApp app;
    private final Perspective perspective = new Perspective();
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
        public void changedItemDataConfig(PVItem item)
        {   setDirty(true);   }

        @Override
        public void changedAnnotations()
        {   setDirty(true);   }
    };

    public DataBrowserInstance(final DataBrowserApp app)
    {
        this.app = app;

        dock_item = new DockItemWithInput(this, perspective, null, file_extensions, this::doSave);
        DockPane.getActiveDockPane().addTab(dock_item);

        dock_item.addCloseCheck(() ->
        {
            dispose();
            return true;
        });

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
        JobManager.schedule("Load Data Browser", monitor ->
        {
            final Model new_model = new Model();
            try
            {
                // Load model from file in background
                XMLPersistence.load(new_model, ResourceParser.getContent(input));
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
                            "Cannot load " + input,
                            ex);
                    }
                });
            }
            catch (Exception ex)
            {
                final String message = "Cannot open Data Browser file\n" + input;
                logger.log(Level.WARNING, message, ex);
                ExceptionDetailsErrorDialog.openError(app.getDisplayName(), message, ex);
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
        perspective.dispose();
    }
}
