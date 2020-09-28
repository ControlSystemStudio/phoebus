/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.area;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.ui.AlarmConfigSelector;
import org.phoebus.applications.alarm.ui.AlarmURI;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;

/** Alarm area panel application instance (singleton)
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmAreaInstance implements AppInstance
{
    private final AlarmAreaApplication app;

    private String server = null, config_name = null;
    private AlarmClient client = null;
    private final DockItemWithInput tab;

    public AlarmAreaInstance(final AlarmAreaApplication app, final URI input) throws Exception
    {
        this.app = app;

        tab = new DockItemWithInput(this, create(input), input, null, null);
        Platform.runLater(() -> tab.setLabel(config_name + " " + app.getDisplayName()));
        tab.addCloseCheck(() ->
        {
            dispose();
            return CompletableFuture.completedFuture(true);
        });
        DockPane.getActiveDockPane().addTab(tab);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    private Node create(final URI input) throws Exception
    {
        final String[] parsed = AlarmURI.parseAlarmURI(input);
        server = parsed[0];
        config_name = parsed[1];

        try
        {
            client = new AlarmClient(server, config_name);
            final AlarmAreaView area_view = new AlarmAreaView(client);
            client.start();

            if (AlarmSystem.config_names.size() > 0)
            {
                final AlarmConfigSelector select = new AlarmConfigSelector(config_name, new_config_name ->
                {
                    // CustomMenuItem configured to stay open to allow selection.
                    // Once selected, do close the menu.
                    area_view.getMenu().hide();
                    changeConfig(new_config_name);
                });
                final CustomMenuItem select_item = new CustomMenuItem(select, false);
                area_view.getMenu().getItems().add(0, select_item);
            }

            return area_view;
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create alarm area panel for " + input, ex);
            return new Label("Cannot create alarm area panel for " + input);
        }
    }

    private void changeConfig(final String new_config_name)
    {
        // Dispose existing setup
        dispose();

        try
        {
            // Use same server name, but new config_name
            final URI new_input = AlarmURI.createURI(server, new_config_name);
            tab.setContent(create(new_input));
            tab.setInput(new_input);
            Platform.runLater(() -> tab.setLabel(config_name + " " + app.getDisplayName()));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot switch alarm area panel to " + config_name, ex);
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
