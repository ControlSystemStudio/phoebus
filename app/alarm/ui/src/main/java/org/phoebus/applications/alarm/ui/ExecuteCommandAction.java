/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.framework.jobs.CommandExecutor;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Action that executes an external command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ExecuteCommandAction extends MenuItem
{
    /** @param item Alarm item
     *  @param display Command to execute
     */
    public ExecuteCommandAction(final AlarmTreeItem<?> item, final TitleDetail command)
    {
        super(command.title, ImageCache.getImageView(AlarmSystem.class, "/icons/exec_command.png"));
        setOnAction(event ->
        {
            JobManager.schedule(command.title, monitor ->
            {
                final CommandExecutor executor = new CommandExecutor(command.detail, AlarmSystem.command_directory);
                executor.call();
            });
        });
    }
}
