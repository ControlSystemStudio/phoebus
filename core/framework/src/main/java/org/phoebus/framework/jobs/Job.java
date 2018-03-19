/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.jobs;

import static org.phoebus.framework.jobs.JobManager.logger;

import java.util.logging.Level;

/** A Job
 *
 *  <p>A Job is created and executed by the {@link JobManager}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Job
{
    private final BasicJobMonitor monitor = new BasicJobMonitor();
    private final String name;
    private final JobRunnable runnable;

    // Create via JobManager
    Job(final String name, final JobRunnable runnable)
    {
        this.name = name;
        this.runnable = runnable;
    }

    /** @return Name of the Job */
    public String getName()
    {
        return name;
    }

    /** @return Information about progress */
    public JobMonitor getMonitor()
    {
        return monitor;
    }

    // Executed by JobManager
    void execute() throws Exception
    {
        monitor.beginTask("Running");
        runnable.run(monitor);
        monitor.done();
    }

    /** Request cancellation */
    public void cancel()
    {
        monitor.cancel();
        logger.log(Level.INFO, toString());
    }

    @Override
    public String toString()
    {
        final String detail = monitor.toString();
        if (detail.isEmpty())
            return "Job '" + name;
        return "Job '" + name + "': " + monitor;
    }
}
