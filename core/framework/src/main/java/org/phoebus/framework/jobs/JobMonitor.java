/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.jobs;

/** Job monitor
 *
 *  <p>Each {@link Job} executes with a job monitor
 *  that allows the job to report progress.
 *
 *  @author Kay Kasemir
 */
public interface JobMonitor
{
    /** Indicate a new task with indeterminate number of steps
     *  @param task_name Name of the task
     */
    public void beginTask(final String task_name);

    /** Indicate a new task with a known number of steps
     *  @param task_name Name of the task
     *  @param steps Number of steps in task
     */
    public void beginTask(final String task_name, final int steps);

    /** Update task name
     *
     *  <p>Changes the task name without affecting the progress count,
     *  i.e. NOT starting a new step counter.
     *
     *  @param task_name Name of the task
     */
    public void updateTaskName(final String task_name);

    /** Indicate completion of steps
     *
     *  <p>Should only be called after {@link #beginTask(String, int)}
     *
     *  @param steps Number of steps completed
     */
    public void worked(final int steps);

    /** Get percentage of work completed
     *
     *  <p>Only meaningful for jobs that call
     *  {@link #beginTask(String, int)}
     *  and {@link #worked(int)},
     *  will otherwise return -1 until
     *  the job completes, whereupon it returns 100.
     *
     * @return Percentage 0 .. 100 or -1 for indeterminate
     */
    public int getPercentage();

    /** Well-behaved long running jobs check for cancellation
     *  @return Has the job been asked to cancel execution?
     */
    public boolean isCanceled();

    /** Indicate that the job has completed.
     *
     *  <p>The {@link JobManager} will automatically
     *  call this when the Job runnable returns.
     */
    public void done();

    /** Check if the job has completed.
     *  @return <code>true</code> if the job has completed
     */
    public boolean isDone();
}
