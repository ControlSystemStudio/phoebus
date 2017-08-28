/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.jobs;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

/** Demo of the Job API
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JobDemo
{
    @Test
    public void demoJob() throws Exception
    {
        final CountDownLatch done = new CountDownLatch(1);

        JobManager.schedule("Demo", monitor ->
        {
            monitor.beginTask("Stepping", 5);
            for (int step=0; step<5; ++step)
            {
                Thread.sleep(500);
                monitor.worked(1);
            }
            done.countDown();
        });

        while (done.getCount() > 0)
        {
            System.out.println(JobManager.getJobs());
            Thread.sleep(250);
        }
        // Show final info
        System.out.println(JobManager.getJobs());
   }


    @Test
    public void demoCancellation() throws Exception
    {
        final CountDownLatch did_some_steps = new CountDownLatch(2);

        JobManager.schedule("Demo", monitor ->
        {
            monitor.beginTask("Wasting time");
            while (true)
            {
                System.out.println("Doing something..");
                did_some_steps.countDown();
                if (monitor.isCancelled())
                {
                    System.out.println("Cancelled, exiting early");
                    return;
                }
                Thread.sleep(500);
            }
        });

        did_some_steps.await();
        JobManager.getJobs().forEach(job -> job.cancel());
        // Show final info
        Collection<Job> jobs = JobManager.getJobs();
        do
        {
            System.out.println(jobs);
            Thread.sleep(500);
        }
        while (! jobs.isEmpty());
   }

}
