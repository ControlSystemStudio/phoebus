/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.archive;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.csstudio.trends.databrowser3.DemoSettings;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.ChannelInfo;
import org.junit.Test;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;

/** Demo of {@link SearchJob}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SearchJobDemo
{
    @Test
    public void testSearch() throws Exception
    {
        final ArchiveDataSource archive = new ArchiveDataSource(DemoSettings.url, "Test");

        final Job job = SearchJob.submit(List.of(archive),
                                         DemoSettings.name_pattern,
                                         channels ->
                                         {
                                             System.out.println("Channels:");
                                             for (ChannelInfo channel : channels)
                                                 System.out.println(channel);
                                         },
                                         ( failed_url, ex ) ->
                                         {
                                             System.err.println("Error reading from " + archive);
                                             ex.printStackTrace();
                                         });

        while (! job.getMonitor().isDone())
        {
            System.out.println(JobManager.getJobs());
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println("Done.");
    }
}