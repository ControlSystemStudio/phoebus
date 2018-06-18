/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.talk.TalkClient;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.scene.Node;
import javafx.scene.control.Label;

public class AnnunciatorTableInstance implements AppInstance
{

    /** Singleton instance maintained by {@link AnnunciatorTableApplication} */
    static AnnunciatorTableInstance INSTANCE = null;

    private final AnnunciatorTableApplication app;

    private TalkClient client;
    private final DockItem tab;

    private AnnunciatorTable annunciatorTable;

    public AnnunciatorTableInstance(final AnnunciatorTableApplication app)
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
            client = new TalkClient(AlarmSystem.server, AlarmSystem.config_name);
            annunciatorTable = new AnnunciatorTable(client);
            client.start();
            return annunciatorTable;
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create annunciator table", ex);
            return new Label("Cannot create annunciator table");
        }
    }

    private void dispose()
    {
        if (client != null)
        {
            client.shutdown();
            client = null;
            annunciatorTable.shutdown();
        }
    }

}
