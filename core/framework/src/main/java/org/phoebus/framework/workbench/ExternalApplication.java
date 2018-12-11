/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.workbench;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.phoebus.framework.jobs.CommandExecutor;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;

/** 'Application' that executes an external command
 *
 *  <p>Created by {@link ApplicationService} based on preference settings
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ExternalApplication implements AppResourceDescriptor
{
    private final String name;
    private final List<String> ext;
    private final String command;

    ExternalApplication(final String name, final List<String> ext, final String command)
    {
        this.name = name;
        this.ext = ext;
        this.command = command;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getDisplayName()
    {
        return "External " + name;
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return ext;
    }

    @Override
    public URL getIconURL()
    {
        return ExternalApplication.class.getResource("/icons/ext_app.png");
    }

    @Override
    public AppInstance create()
    {
        final CommandExecutor executor = new CommandExecutor(command, WorkbenchPreferences.external_apps_directory);
        JobManager.schedule(command, monitor -> executor.call());
        return null;
    }

    @Override
    public AppInstance create(final URI resource)
    {
        JobManager.schedule(command, monitor ->
        {
            // If resource is a file, provide the plain file path because external command
            // is unlikely to understand "file://..".
            // Otherwise pass the URI, expecting command to understand "http://.."
            final File file = ResourceParser.getFile(resource);
            final String cmd = (file == null)
                    ? command + " \"" + resource + "\""
                            : command + " \"" + file.getAbsolutePath() + "\"";

            final CommandExecutor executor = new CommandExecutor(cmd, WorkbenchPreferences.external_apps_directory);
            executor.call();
        });

        return null;
    }
}
