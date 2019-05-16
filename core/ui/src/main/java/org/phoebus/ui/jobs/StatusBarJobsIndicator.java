/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.jobs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.application.Messages;

import javafx.application.Platform;
import javafx.scene.control.Button;

/** Status bar entry to show number of jobs
 *
 *  <p>Allows opening the more detailed {@link JobViewer}
 *  @author Kay Kasemir
 */
public class StatusBarJobsIndicator extends Button
{
    // Initially, the button is visible with "Jobs: ?" to size the button.
    // Start 'last_count' with -1, an impossible job count, to assert initial update()
    // call to either set the correct "Jobs: .." count or hide the button when there are no jobs.
    private volatile int last_count = -1;

    public StatusBarJobsIndicator()
    {
        super(Messages.JobBtnInit);
        setOnAction(event -> ApplicationService.createInstance(JobViewerApplication.NAME));
        JobViewer.TIMER.scheduleWithFixedDelay(this::update, 2000, 500, TimeUnit.MILLISECONDS);
    }

    private void update()
    {
        // Determine what to show
        final int count = JobManager.getJobCount();
        if (count == last_count)
            return;

        final String text;
        if (count <= 0)
            text = null;
        else
            text = Messages.JobBtnCnt + count;

        // Update button on UI thread
        final CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() ->
        {
            if (text == null)
                setVisible(false);
            else
            {
                setText(text);
                setVisible(true);
            }
            done.countDown();
        });

        // Wait
        try
        {
            done.await(2, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            // Ignore
        }
        last_count = count;
        // Next update will be triggered by timer
    }
}
