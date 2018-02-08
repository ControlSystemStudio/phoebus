/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.jobs;

import java.util.concurrent.atomic.AtomicInteger;

/** Sub Job monitor
 *
 *  <p>Takes a portion of work steps from a parent job monitor.
 *
 *  <p>Useful for cases where a job needs to delegate
 *  pieces of work to subroutines.
 *  These subroutines are unaware of the total number of
 *  work steps. They are presented with a {@link SubJobMonitor}
 *  for their own accounting of work steps.
 *
 *  @author Kay Kasemir
 */
public class SubJobMonitor implements JobMonitor
{
    private final JobMonitor parent;

    /** Amount of steps that this sub monitor will consume in parent when 'done' */
    private final int total_parent_steps;

    /** Steps consumed in parent as of yet */
    private volatile int consumed_parent_steps = 0;

    /** Steps to perform within this sub monitor */
    private volatile int steps = -1;

    /** Steps worked within this sub monitor, valued 0 .. steps */
    private AtomicInteger worked = new AtomicInteger(0);

    /** Create sub-monitor
     *
     *  <p>Sub monitor allocates a number of steps in the parent.
     *  The initial assumption is that it will perform that number
     *  of steps, but a call to {@link #beginTask(String, int)}
     *  can rescale that.
     *
     *  @param parent Parent {@link JobMonitor}
     *  @param parent_steps Steps to consume from the parent
     */
    public SubJobMonitor(final JobMonitor parent, final int parent_steps)
    {
        this.parent = parent;
        total_parent_steps = parent_steps;
        this.steps = parent_steps;
    }

    @Override
    public void beginTask(final String task_name)
    {
        // Only update parent task name, NOT changing the parent's step counter
        updateTaskName(task_name);
    }

    @Override
    public void beginTask(final String task_name, final int steps)
    {
        // Only update parent task name, NOT changing the parent's step counter
        updateTaskName(task_name);
        this.steps = steps;
        worked.set(0);
    }

    @Override
    public void updateTaskName(final String task_name)
    {
        parent.updateTaskName(task_name);
    }

    @Override
    public void worked(final int worked_steps)
    {
        final int sub_steps = worked.addAndGet(worked_steps);
        if (steps > 0)
        {
            // Scale steps within sub monitor to our allocated parent steps
            final int parent_steps = Math.min(total_parent_steps * sub_steps / steps,
                                              total_parent_steps);
            // Any new steps to report to parent?
            final int new_steps = parent_steps - consumed_parent_steps;
            if (new_steps > 0)
            {
                consumed_parent_steps += new_steps;
                parent.worked(new_steps);
            }
        }
    }

    @Override
    public int getPercentage()
    {
        if (steps > 0)
            return Math.min(worked.get() * 100 / steps, 100);
        return -1;
    }

    @Override
    public boolean isCanceled()
    {
        return parent.isCanceled();
    }

    @Override
    public boolean isDone()
    {
        return parent.isDone();
    }

    @Override
    public void done()
    {
        final int remaining = total_parent_steps - consumed_parent_steps;
        if (remaining > 0)
        {
            consumed_parent_steps = total_parent_steps;
            parent.worked(remaining);
        }
    }
}
