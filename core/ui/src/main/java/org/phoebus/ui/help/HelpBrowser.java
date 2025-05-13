/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.help;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.File;
import java.util.logging.Level;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;

/** Web browser that displays help
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class HelpBrowser implements AppInstance
{
    /** At most one instance */
    static HelpBrowser INSTANCE = null;

    private final AppDescriptor app;

    private DockItem dock_item = null;

    private WebView browser;

    HelpBrowser(final AppDescriptor app)
    {
        this.app = app;

        browser = new WebView();
        dock_item = new DockItem(this, new BorderPane(browser))
        {
            // Add 'Web URL'
            @Override
            protected void fillInformation(final StringBuilder info)
            {
                super.fillInformation(info);
                info.append("\n");
                info.append(Messages.HelpPage).append(browser.getEngine().getLocation());
            }
        };
        dock_item.addClosedNotification(this::dispose);
        DockPane.getActiveDockPane().addTab(dock_item);

        JobManager.schedule(app.getDisplayName(), this::loadHelp);
    }

    private void loadHelp(final JobMonitor monitor)
    {
        final String location = OpenHelp.determineHelpLocation();
        logger.log(Level.CONFIG, "Showing help from " + location);
        Platform.runLater(() -> browser.getEngine().load(location));
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    /** Show the existing singleton instance */
    public void raise()
    {
        dock_item.select();
    }

    private void dispose()
    {
        INSTANCE = null;
    }
}
