/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.net.URL;
import java.util.logging.Level;

import org.csstudio.display.builder.model.persist.ModelLoader;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.jobs.JobManager;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

/** PV Table Application
 *  @author Kay Kasemir
 */
public class DisplayRuntime implements AppInstance
{
    final private AppDescriptor app;
    final private DockItemWithInput dock_item;

    DisplayRuntime(final AppDescriptor app, final URL input)
    {
        this.app = app;

        final BorderPane layout = new BorderPane(new Label("TODO: load, represent, start " + input));
        dock_item = new DockItemWithInput(this, layout, input, null);
        DockPane.getActiveDockPane().addTab(dock_item);

        loadResource(input);

        dock_item.addClosedNotification(this::stop);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    public void raise()
    {
        dock_item.select();
    }

    public void loadResource(final URL input)
    {
        // Set input ASAP so that other requests to open this
        // resource will find this instance and not start
        // another instance
        dock_item.setInput(input);

        // Load files in background job
        JobManager.schedule("Load Display", monitor ->
        {
            monitor.beginTask(input.toExternalForm());
            try
            {
                String parent_display = null;
                ModelLoader.resolveAndLoadModel(parent_display , input.toString());
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error loading " + input, ex);
                showError("Error loading " + input);
            }
        });
    }

    private void showError(final String message)
    {
        Platform.runLater(() ->
        {
            // TODO Show error in dock_item
        });
    }

    public void stop()
    {
        // TODO Stop runtime, dispose representation, release model
    }
}
