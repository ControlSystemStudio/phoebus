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

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

/** Alarm tree application instance (singleton)
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class AlarmTreeInstance implements AppInstance
{
    /** Memento tag for last used configuration */
    private static final String TAG_CONFIG = "config";

    // TODO Allow multiple instances...
    /** Singleton instance maintained by {@link AlarmTreeApplication} */
    static AlarmTreeInstance INSTANCE = null;

    private final AlarmTreeApplication app;

    private AlarmClient client = null;
    private final DockItem tab;


    public AlarmTreeInstance(final AlarmTreeApplication app)
    {
        this.app = app;
        // Start with dummy node, to be replaced in restore()
        tab = new DockItem(this, new Group());
        tab.addCloseCheck(() ->
        {
            dispose();
            return true;
        });
        tab.addClosedNotification(() -> INSTANCE = null);
        final DockPane dockPane = DockPane.getActiveDockPane();
        if (null != dockPane)
        	dockPane.addTab(tab);
        else
        {
        	dispose();
        	INSTANCE = null;
        }
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

    @Override
    public void restore(final Memento memento)
    {
        // Use config from previous run, or default to preference
        final String config_name = memento.getString(TAG_CONFIG).orElse(AlarmSystem.config_name);
        tab.setContent(create(config_name));
    }

    @Override
    public void save(final Memento memento)
    {
        memento.setString(TAG_CONFIG, client.getRoot().getName());
    }

    private Node create(final String config_name)
    {
        try
        {
            client = new AlarmClient(AlarmSystem.server, config_name);
            final AlarmTreeView tree_view = new AlarmTreeView(client);
            client.start();

            if (AlarmSystem.config_names.size() > 0)
            {
                final ComboBox<String> configs = new ComboBox<>();
                configs.getItems().setAll(AlarmSystem.config_names);
                configs.setValue(config_name);
                configs.setOnAction(event ->  changeConfig(configs.getValue()));
                tree_view.getToolbar().getItems().add(0, configs);
            }

            return tree_view;
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create alarm tree", ex);
            return new Label("Cannot create alarm tree");
        }
    }

    private void changeConfig(final String config_name)
    {
        dispose();
        tab.setContent(create(config_name));
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
