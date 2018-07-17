/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.ui.table;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.ui.tree.AlarmTreeApplication;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.scene.Node;
import javafx.scene.control.Label;

/** Alarm table application instance (singleton)
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class AlarmTableInstance implements AppInstance
{
    /** Singleton instance maintained by {@link AlarmTreeApplication} */
    static AlarmTableInstance INSTANCE = null;

    private final AlarmTableApplication app;

    private AlarmClient client;
    private AlarmTableUI table;
    private AlarmTableMediator mediator;
    private final DockItem tab;

    public AlarmTableInstance(final AlarmTableApplication app)
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
            client = new AlarmClient(AlarmSystem.server, AlarmSystem.config_name);
            table = new AlarmTableUI(client);
            mediator = new AlarmTableMediator(client, table);
            client.addListener(mediator);
            client.start();
            return table;
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create alarm tree", ex);
            return new Label("Cannot create alarm tree");
        }
    }

    @Override
    public void restore(final Memento memento)
    {
        table.restore(memento);
    }

    @Override
    public void save(final Memento memento)
    {
        table.save(memento);
    }

    private void dispose()
    {
        if (mediator != null)
        {
            client.removeListener(mediator);
            mediator = null;
        }
        if (client != null)
        {
            client.shutdown();
            client = null;
        }
    }
}
