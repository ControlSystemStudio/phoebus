/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.net.URI;
import java.util.logging.Level;

import org.csstudio.trends.databrowser3.model.Model;
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
    private DockItemWithInput dock_item;

    private Perspective perspective;

    public DataBrowserInstance(final DataBrowserApp app)
    {
        this.app = app;

        final DockPane dock_pane = DockPane.getActiveDockPane();

        perspective = new Perspective();

        dock_item = new DockItemWithInput(this, perspective, null, file_extensions, this::doSave);
        dock_pane.addTab(dock_item);

        dock_item.addCloseCheck(() ->
        {
            dispose();
            return true;
        });
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

    public void loadResource(final URI input)
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
                XMLPersistence.load(new_model, ResourceParser.getContent(input));
                Platform.runLater(() ->
                {
                    try
                    {
                        getModel().load(new_model);
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

    void doSave(final JobMonitor monitor) throws Exception
    {

    }

    private void dispose()
    {
        perspective.dispose();
    }
}
