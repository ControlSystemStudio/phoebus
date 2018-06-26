/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import java.util.function.BiConsumer;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.TitleDetailDelay;

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
public class AutomatedActionExecutor implements BiConsumer<AlarmTreeItem<?>, TitleDetailDelay>
{
    // TODO Use TitleDetailDelay
    public static final BiConsumer<AlarmTreeItem<?>, TitleDetailDelay> INSTANCE = new AutomatedActionExecutor();

    @Override
    public void accept(final AlarmTreeItem<?> item, final TitleDetailDelay action)
    {
        // TODO Perform the automated action in designated thread (work queue)
        System.out.println("TODO: Execute " + action + " for " + item.getPathName());
        if (action.detail.startsWith("mailto:"))
            System.out.println("TODO: Send email");
        else if (action.detail.startsWith("cmd:"))
            System.out.println("TODO: Execute external command");
    }
}
