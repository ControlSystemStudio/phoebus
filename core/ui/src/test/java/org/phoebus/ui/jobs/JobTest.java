/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.jobs;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

/** Demo of the Job API
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JobTest
{
    @Test
    public void demoJob() throws Exception
    {
        final CountDownLatch running = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);

        JobManager.schedule("Demo", monitor ->
        {
            monitor.beginTask("Stepping", 3);
            running.countDown();
            for (int step=0; step<3; ++step)
            {
                Thread.sleep(500);
                monitor.worked(1);
            }
            done.countDown();
        });

        // Wait for job to start
        running.await();
        System.out.println(JobManager.getJobs());
        assertThat(JobManager.getJobs().size(), equalTo(1));

        // Wait for job to end
        while (done.getCount() > 0)
        {
            System.out.println(JobManager.getJobs());
            Thread.sleep(250);
        }
        // Show final info
        Thread.sleep(500);
        System.out.println(JobManager.getJobs());
        assertThat(JobManager.getJobs().size(), equalTo(0));
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

        // Wait for job to perform a few steps
        did_some_steps.await();

        // Cancel
        JobManager.getJobs().forEach(job -> job.cancel());
        // Show final info
        while (true)
        {
	        	Thread.sleep(2000);
	        	final Collection<Job> jobs = JobManager.getJobs();
	        	if (jobs.isEmpty())
	        		break;
            System.out.println(jobs);
        }
   }
}
