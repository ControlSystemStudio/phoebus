/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.talk.TalkClient;
import org.phoebus.applications.alarm.ui.AlarmConfigSelector;
import org.phoebus.applications.alarm.ui.AlarmURI;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;

/** Instance of AnnunciatorTableApplication
 *  @author Even Smith
 */
@SuppressWarnings("nls")
public class AnnunciatorTableInstance implements AppInstance
{
    private final AnnunciatorTableApplication app;

    private String server = null, config_name = null;
    private TalkClient client = null;
    private final DockItemWithInput tab;
    private AnnunciatorTable annunciatorTable = null;

    public AnnunciatorTableInstance(final AnnunciatorTableApplication app, final URI input) throws Exception
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

    /** Create UI for input, starts alarm client
     *
     *  @param input Alarm URI, will be parsed into `server` and `config_name`
     *  @return Alarm UI
     *  @throws Exception
     */
    private Node create(final URI input) throws Exception
    {
        final String[] parsed = AlarmURI.parseAlarmURI(input);
        server = parsed[0];
        config_name = parsed[1];

        try
        {
            client = new TalkClient(server, config_name);
            annunciatorTable = new AnnunciatorTable(client);
            client.start();

            if (AlarmSystem.config_names.size() > 0)
            {
                final AlarmConfigSelector configs = new AlarmConfigSelector(config_name, this::changeConfig);
                annunciatorTable.getToolbar().getItems().add(0, configs);
            }

            return annunciatorTable;
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create annunciator table for " + input, ex);
            return new Label("Cannot create annunciator table for " + input);
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
            logger.log(Level.WARNING, "Cannot switch annunciator to " + config_name, ex);
        }
    }

    private void dispose()
    {
        if (client != null)
        {
            client.shutdown();
            client = null;
            annunciatorTable.shutdown();
            annunciatorTable = null;
        }
    }
}
