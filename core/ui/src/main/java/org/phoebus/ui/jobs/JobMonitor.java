/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.jobs;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/** Job monitor
 *
 *  <p>Each {@link Job} executes with a job monitor
 *  that allows the job to report progress.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JobMonitor
{
    private final Job job;
    private volatile String task = "Idle";
    private volatile int steps = -1;
    private AtomicInteger worked = new AtomicInteger(0);
    volatile boolean cancelled = false;

    JobMonitor(final Job job)
    {
        this.job = job;
    }

    /** Indicate a new (sub) task
     *  @param task_name Name of the task
     */
    public void beginTask(final String task_name)
    {
        beginTask(task_name, -1);
    }

    /** Indicate a new (sub) task with a known number of steps
     *  @param task_name Name of the task
     *  @param steps Number of steps in task
     */
    public void beginTask(final String task_name, final int steps)
    {
        task = task_name;
        this.steps = steps;
        logger.log(Level.INFO, job.toString());
    }

    /** Indicate completion of steps
     *  @param steps Number of steps completed
     */
    public void worked(final int steps)
    {
        worked.addAndGet(steps);
        logger.log(Level.INFO, job.toString());
    }

    // Called by Job on successful completion
    void done()
    {
        task = "Finished";
        steps = -1;
        logger.log(Level.INFO, job.toString());
    }

    /** Well-behaved long running jobs check for cancellation
     *  @return Has the job been asked to cancel execution?
     */
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public String toString()
    {
        if (cancelled)
            return task + " - Cancelled";
        if (steps > 0)
            return task + " (" + worked.get() + "/" + steps + ")";
        return task;
    }
}
