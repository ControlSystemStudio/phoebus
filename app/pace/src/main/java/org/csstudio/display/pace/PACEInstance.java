/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.pace;

import java.net.URI;

import org.csstudio.display.pace.gui.GUI;
import org.csstudio.display.pace.model.Model;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;

/** PACE Instance
 *  @author Kay Kasemir
 */
public class PACEInstance implements AppInstance
{
    private final GUI gui = new GUI();
    private final AppDescriptor app;
    private final DockItemWithInput tab;

    public PACEInstance(final PACEApp app, final URI resource)
    {
        this.app = app;
        tab = new DockItemWithInput(this, gui, resource, null, null);
        DockPane.getActiveDockPane().addTab(tab);
        Platform.runLater(() -> tab.setLabel(app.getDisplayName()));

        // Load in background...
        JobManager.schedule("Load " + resource, monitor -> loadModel(monitor, resource));
    }

    private void loadModel(final JobMonitor monitor, final URI resource) throws Exception
    {
        Thread.sleep(2000);
        final Model model = new Model(Model.class.getResourceAsStream("/pace_examples/localtest.pace"));
        gui.setModel(model);
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
}
