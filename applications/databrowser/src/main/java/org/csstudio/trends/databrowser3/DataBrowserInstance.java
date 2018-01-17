/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import org.csstudio.trends.databrowser3.ui.Perspective;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.stage.FileChooser.ExtensionFilter;

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
