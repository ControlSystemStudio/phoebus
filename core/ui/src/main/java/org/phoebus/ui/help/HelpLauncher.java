/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.help;

import static org.phoebus.ui.application.PhoebusApplication.logger;

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
        final String location = HelpBrowser.determineHelpLocation();

        Platform.runLater(() -> startBrowser(location));
    }
    private void startBrowser(final String location)
    {
        logger.log(Level.CONFIG, "Help location: " + location);

        // External web browser
        application.getHostServices().showDocument(location);
    }
}
