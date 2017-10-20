/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.jobs;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;

/** Job Manager
 *  @author Kay Kasemir
 */
public class JobManager
{
    private static final ConcurrentSkipListSet<Job> active_jobs =
        new ConcurrentSkipListSet<>((job1, job2) -> System.identityHashCode(job2) - System.identityHashCode(job1));

    /** Submit a new Job
     *
     *  @param name Name of the Job (for UI that displays active Jobs)
     *  @param runnable {@link JobRunnable} to execute
     */
    public static void schedule(final String name, final JobRunnable runnable)
    {
        ForkJoinPool.commonPool().submit(() -> execute(new Job(name, runnable)));
    }

    private static Void execute(final Job job) throws Exception
    {
        active_jobs.add(job);
        try
        {
            job.execute();
        }
        catch (Throwable ex)
        {
        	logger.log(Level.WARNING, "Job '" + job.getName() + "' failed", ex);
        	throw ex;
        }
        finally
        {
            active_jobs.remove(job);
        }
        return null;
    }

    /** Obtain snapshot of currently running Jobs
     *
     *  <p>Note that the list is not updated,
     *  need to get new list for updated information.
     *
     *  @return Currently active jobs
     */
    public static List<Job> getJobs()
    {
        return new ArrayList<>(active_jobs);
    }
}
