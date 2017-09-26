/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.internal;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.File;
import java.util.logging.Level;

import org.phoebus.ui.jobs.JobMonitor;
import org.phoebus.ui.jobs.JobRunnable;

import javafx.application.Application;
import javafx.application.Platform;

/** Show help
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class HelpLauncher implements JobRunnable
{
    private final Application application;

    public HelpLauncher(final Application application)
    {
        this.application = application;
    }

    @Override
    public void run(final JobMonitor monitor) throws Exception
    {
        final String location = determineHelpLocation();

        Platform.runLater(() -> startBrowser(location));
    }

    private String determineHelpLocation()
    {
        // Installation directory can be defined as "phoenix.install",
        // falling back to "user.dir"
        final String phoenix_install = System.getProperty("phoenix.install", System.getProperty("user.dir"));

        logger.log(Level.CONFIG, "Installed in " + phoenix_install);

        // The distribution includes a lib/ and a doc/ folder.
        // Check for the doc/index.html
        File loc = new File(phoenix_install, "doc/index.html");
        if (loc.exists())
            return loc.toString();

        // During development,
        // product is started from IDE as ....../git/phoebus/phoebus-product.
        // Check for copy of docs in ....../git/phoebus-doc/build/html
        loc = new File(phoenix_install, "../../phoebus-doc");
        if (loc.exists())
        {
            loc = new File(loc, "build/html/index.html");
            if (loc.exists())
                return loc.toString();
            logger.log(Level.WARNING, "Found phoebus-doc repository, but no build/html/index.html. Run 'make html'");
        }

        // Fall back to online copy of the manual
        return "http://phoebus-doc.readthedocs.io";
    }

    private void startBrowser(final String location)
    {
        logger.log(Level.CONFIG, "Help location: " + location);
        application.getHostServices().showDocument(location);
    }
}
