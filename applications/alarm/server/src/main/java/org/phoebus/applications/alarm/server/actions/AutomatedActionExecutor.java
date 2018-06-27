/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server.actions;

import java.util.function.BiConsumer;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.framework.jobs.JobManager;

/** Executor for automated actions
 *
 *  <p>Handles automated actions with the following detail:
 *
 *  <p>"mailto:user@site.org,another@else.com"<br>
 *  Sends email with alarm detail to list of recipients.
 *
 *  <p>"cmd:some_command arg1 arg2"<br>
 *  Invokes command with list of space-separated arguments.
 *  The special argument "*" will be replaced with a list of alarm PVs and their alarm severity.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AutomatedActionExecutor implements BiConsumer<AlarmTreeItem<?>, TitleDetailDelay>
{
    public static final BiConsumer<AlarmTreeItem<?>, TitleDetailDelay> INSTANCE = new AutomatedActionExecutor();

    @Override
    public void accept(final AlarmTreeItem<?> item, final TitleDetailDelay action)
    {
        // Perform the automated action in background thread
        JobManager.schedule("Automated Action", monitor ->
        {
            if (action.detail.startsWith("mailto:"))
                EmailActionExecutor.sendEmail(item, action.detail.substring(7).split(" *, *"));
            else if (action.detail.startsWith("cmd:"))
                // TODO Execute cmd:.. actions
                System.out.println("Execute " + action + " for " + item.getPathName());
        });
    }
}
