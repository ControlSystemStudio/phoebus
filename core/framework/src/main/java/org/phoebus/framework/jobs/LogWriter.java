/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.jobs;

import static org.phoebus.framework.jobs.JobManager.logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

/** Thread that writes data from stream to log
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LogWriter extends Thread
{
    private final BufferedReader reader;
    private final String cmd;
    private Level level;

    public LogWriter(final InputStream stream, final String cmd, final Level level)
    {
        super("LogWriter " + level.getName() + " " + cmd);
        reader = new BufferedReader(new InputStreamReader(stream));
        this.cmd = cmd;
        this.level = level;
        setDaemon(true);
    }

    @Override
    public void run()
    {
        try
        {
            String line;
            while ((line = reader.readLine()) != null)
                logger.log(level, "Cmd {0}: {1}", new Object[] { cmd, line });
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error reading cmd output", ex);
        }
    }
}