/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Job Manager
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JobManager
{
    private static final ExecutorService pool = Executors.newCachedThreadPool(new NamedThreadFactory("Jobs"));
    private static final ConcurrentSkipListSet<Job> active_jobs =
        new ConcurrentSkipListSet<>((job1, job2) -> System.identityHashCode(job2) - System.identityHashCode(job1));

    /** Submit a new Job
     *
     *  @param name Name of the Job (for UI that displays active Jobs)
     *  @param runnable {@link JobRunnable} to execute
     */
    public static void schedule(final String name, final JobRunnable runnable)
    {
        pool.submit(() -> execute(new Job(name, runnable)));
    }

    private static Void execute(final Job job) throws Exception
    {
        active_jobs.add(job);
        try
        {
            job.execute();
        }
        finally
        {
            active_jobs.remove(job);
        }
        return null;
    }

    /** @return Currently active jobs */
    public static List<Job> getJobs()
    {
        return new ArrayList<>(active_jobs);
    }
}
