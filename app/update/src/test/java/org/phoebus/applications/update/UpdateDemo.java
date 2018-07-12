/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.update;

import java.io.File;
import java.time.Instant;

import org.phoebus.framework.jobs.BasicJobMonitor;
import org.phoebus.framework.jobs.JobMonitor;

/** Demo of update
 *
 *  <p>Example arguments:
 *  /home/ky9/Downloads/product-sns-0.0.1
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class UpdateDemo
{
    public static void main(final String[] args) throws Exception
    {
        if (args.length != 1)
        {
            System.err.println("Need path to installation,");
            return;
        }
        final File install_location = new File(args[0]);

        final JobMonitor monitor = new BasicJobMonitor()
        {
            @Override
            public void beginTask(final String task_name)
            {
                System.out.println(task_name);
                super.beginTask(task_name);
            }

            @Override
            public void updateTaskName(final String task_name)
            {
                System.out.println(task_name);
                super.updateTaskName(task_name);
            }
        };

        final Instant new_version = Update.checkForUpdate(monitor);
        if (new_version != null)
            Update.downloadAndUpdate(monitor, install_location);
    }
}