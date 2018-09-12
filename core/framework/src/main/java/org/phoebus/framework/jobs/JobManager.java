/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Job Manager
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JobManager
{
    static final Logger logger = Logger.getLogger(JobManager.class.getPackageName());

    private static final ExecutorService POOL = Executors.newCachedThreadPool(new NamedThreadFactory("JobManager"));

    // Used ConcurrentSkipListSet, but that keeps past Job in memory for a long time,
    // making it harder to debug memory leaks.
    //
    // Don't expect too many jobs at the same time (10..50),
    // but need fast way to get count and copy of active jobs
    // -> Plain list, synchronized
    private static final List<Job> active_jobs = Collections.synchronizedList(new ArrayList<>());

    /** Submit a new Job
     *
     *  @param name Name of the Job (for UI that displays active Jobs)
     *  @param runnable {@link JobRunnable} to execute
     *  @return {@link Job}
     */
    public static Job schedule(final String name, final JobRunnable runnable)
    {
        final Job job = new Job(name, runnable);
        POOL.submit(() -> execute(job));
        return job;
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

    /** @return Number of active jobs */
    public static int getJobCount()
    {
        return active_jobs.size();
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
