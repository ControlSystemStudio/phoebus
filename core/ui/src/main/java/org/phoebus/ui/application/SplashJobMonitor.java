/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import org.phoebus.framework.jobs.JobMonitor;

/** {@link JobMonitor} that updates {@link Splash}
 *
 *  <p>Wraps a parent job monitor.
 *
 *  @author Kay Kasemir
 */
class SplashJobMonitor implements JobMonitor
{
    final JobMonitor parent;
    final Splash splash;

    /** @param parent Base {@link JobMonitor} to which all calls will be forwarded
     *  @param splash {@link Splash} that will be updated
     */
    public SplashJobMonitor(final JobMonitor parent, final Splash splash)
    {
        this.parent = parent;
        this.splash = splash;
    }

    @Override
    public void beginTask(final String task_name)
    {
        parent.beginTask(task_name);
        splash.updateStatus(task_name);
    }

    @Override
    public void beginTask(final String task_name, final int steps)
    {
        parent.beginTask(task_name, steps);
        splash.updateStatus(task_name);
    }

    @Override
    public void updateTaskName(final String task_name)
    {
        splash.updateStatus(task_name);
    }

    @Override
    public void worked(final int steps)
    {
        parent.worked(steps);
        splash.updateProgress(parent.getPercentage());
    }

    @Override
    public int getPercentage()
    {
        return parent.getPercentage();
    }

    @Override
    public boolean isCancelled()
    {
        return parent.isCancelled();
    }

    @Override
    public boolean isDone()
    {
        return parent.isDone();
    }

    @Override
    public void done()
    {
        parent.done();
        splash.updateStatus("Enjoy Phoebus!");
        splash.updateProgress(parent.getPercentage());
    }
}