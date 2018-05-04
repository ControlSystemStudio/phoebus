/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.logging.Level;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.scene.Node;
import javafx.scene.control.Label;

/** Alarm tree application instance (singleton)
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class AlarmTreeInstance implements AppInstance
{
    /** Singleton instance maintained by {@link AlarmTreeApplication} */
    static AlarmTreeInstance INSTANCE = null;

    private final AlarmTreeApplication app;

    private AlarmClient client;
    private final DockItem tab;


    public AlarmTreeInstance(final AlarmTreeApplication app)
    {
        this.app = app;
        tab = new DockItem(this, create());
        tab.addCloseCheck(() ->
        {
            dispose();
            return true;
        });
        tab.addClosedNotification(() -> INSTANCE = null);
        DockPane.getActiveDockPane().addTab(tab);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    void raise()
    {
        tab.select();
    }

    private Node create()
    {
        try
        {
            // TODO Preferences
            client = new AlarmClient("localhost:9092", "Accelerator");
            final AlarmTreeView tree_view = new AlarmTreeView(client);
            client.start();
            return tree_view;
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create alarm tree", ex);
            return new Label("Cannot create alarm tree");
        }
    }

    private void dispose()
    {
        if (client != null)
        {
            client.shutdown();
            client = null;
        }
    }
}
