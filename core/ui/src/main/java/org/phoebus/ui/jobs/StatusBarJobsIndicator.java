/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.jobs;

import java.util.concurrent.TimeUnit;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.ApplicationService;

import javafx.scene.control.Button;

/** Status bar entry to show number of jobs
 *
 *  <p>Allows opening the more detailed {@link JobViewer}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StatusBarJobsIndicator extends Button
{
    private volatile int last_count = 0;

    public StatusBarJobsIndicator()
    {
        super("Jobs: ?");
        setOnAction(event -> ApplicationService.findApplication(JobViewerApplication.NAME).create());
        JobViewer.TIMER.scheduleWithFixedDelay(this::update, 2000, 500, TimeUnit.MILLISECONDS);
    }

    private void update()
    {
        final int count = JobManager.getJobCount();
        if (count == last_count)
            return;
        if (count <= 0)
            setVisible(false);
        else
        {
            setText("Jobs: " + count);
            setVisible(true);
        }
        last_count = count;
    }
}
