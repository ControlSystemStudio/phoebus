/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.jobs;

import java.util.concurrent.atomic.AtomicInteger;

/** Job monitor
 *
 *  <p>Each {@link Job} executes with a job monitor
 *  that allows the job to report progress.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BasicJobMonitor implements JobMonitor
{
    private volatile String task = "Idle";
    private volatile int steps = -1;
    private AtomicInteger worked = new AtomicInteger(0);

    private static enum State { RUNNING, CANCELLED, DONE };
    private volatile State state = State.RUNNING;

    @Override
    public void beginTask(final String task_name)
    {
        beginTask(task_name, -1);
    }

    @Override
    public void beginTask(final String task_name, final int steps)
    {
        updateTaskName(task_name);
        this.steps = steps;
        worked.set(0);
    }

    @Override
    public void updateTaskName(final String task_name)
    {
        task = task_name;
    }

    @Override
    public void worked(final int worked_steps)
    {
        worked.addAndGet(worked_steps);
    }

    @Override
    public int getPercentage()
    {
        if (state == State.DONE)
            return 100;
        if (steps > 0)
            return Math.min(worked.get() * 100 / steps, 100);
        return -1;
    }

    // Called by Job in same package
    void cancel()
    {
        state = State.CANCELLED;
    }

    @Override
    public boolean isCanceled()
    {
        return state == State.CANCELLED;
    }

    @Override
    public void done()
    {
        updateTaskName("Finished");
        state = State.DONE;
    }

    @Override
    public boolean isDone()
    {
        return state != State.RUNNING;
    }

    @Override
    public String toString()
    {
        if (isCanceled())
            return task + " - Cancelled";
        if (steps > 0)
            return task + " (" + worked.get() + "/" + steps + ")";
        return task;
    }
}
