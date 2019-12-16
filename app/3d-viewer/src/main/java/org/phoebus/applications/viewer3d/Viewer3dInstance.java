/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.viewer3d;

import static org.phoebus.applications.viewer3d.Viewer3dPane.logger;

import java.net.URI;
import java.util.logging.Level;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;

/** Application instance
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class Viewer3dInstance implements AppInstance
{
    private final AppDescriptor app;
    private final DockItemWithInput tab;
    private Viewer3dPane viewer;

    public Viewer3dInstance(final Viewer3dApp viewerApp, final URI resource)
    {
        app = viewerApp;
        tab = new DockItemWithInput(this, create(resource), resource, null, null);
        viewer = null;

        Platform.runLater(() -> tab.setLabel(app.getDisplayName()));

        DockPane.getActiveDockPane().addTab(tab);
    }

    private Node create(URI resource)
    {
        try
        {
            viewer = new Viewer3dPane(resource, this::changeInput);
            return viewer;
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create 3d Viewer for " + resource, ex);
            return new Label("Cannot create 3d Viewer for " + resource);
        }
    }

    private void changeInput(final URI resource)
    {
        tab.setInput(resource);
        Platform.runLater(() -> tab.setLabel(app.getDisplayName()));
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    public void raise()
    {
        tab.select();
    }

    public void reload()
    {
        if (null != viewer)
        {
            viewer.reload();
        }
    }
}
