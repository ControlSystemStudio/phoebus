/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.help;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.File;
import java.util.logging.Level;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.jobs.JobManager;
import org.phoebus.ui.jobs.JobMonitor;

import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;

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
        dock_item = new DockItem(this, new BorderPane(browser));
        dock_item.addClosedNotification(this::dispose);
        DockPane.getActiveDockPane().addTab(dock_item);

        JobManager.schedule(app.getDisplayName(), this::loadHelp);
    }

    private void loadHelp(final JobMonitor monitor)
    {
        final String location = determineHelpLocation();
        Platform.runLater(() -> browser.getEngine().load(location));
    }

    public static String determineHelpLocation()
    {
        // Installation directory can be defined as "phoenix.install",
        // falling back to "user.dir"
        final String phoenix_install = System.getProperty("phoenix.install", System.getProperty("user.dir"));

        logger.log(Level.CONFIG, "Installed in " + phoenix_install);

        // The distribution includes a lib/ and a doc/ folder.
        // Check for the doc/index.html
        File loc = new File(phoenix_install, "doc/index.html");
        if (loc.exists())
            return loc.toURI().toString();

        // During development,
        // product is started from IDE as ....../git/phoebus/phoebus-product.
        // Check for copy of docs in ....../git/phoebus-doc/build/html
        loc = new File(phoenix_install, "../../phoebus-doc");
        if (loc.exists())
        {
            loc = new File(loc, "build/html/index.html");
            if (loc.exists())
                return loc.toURI().toString();
            logger.log(Level.WARNING, "Found phoebus-doc repository, but no build/html/index.html. Run 'make html'");
        }

        // Fall back to online copy of the manual
        return "http://phoebus-doc.readthedocs.io";
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
