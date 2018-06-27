/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server.actions;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.List;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.server.AlarmServerPV;
import org.phoebus.framework.jobs.CommandExecutor;

/** Executor for automated commands
 *
 *  <p>Handles automated actions with the following detail:
 *
 *  <p>"cmd:some_command arg1 arg2"<br>
 *  Invokes command with list of space-separated arguments.
 *  The special argument "*" will be replaced with a list of alarm PVs and their alarm severity.
 *
 *  <p>Command is executed in AlarmSystem.command_directory
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CommandActionExecutor
{
    /** @param item Item for which to run command
     *  @param command The command to run
     *  @throws Exception on error
     */
    public static void run(final AlarmTreeItem<?> item, final String command) throws Exception
    {
        final String patched = patchCommand(item, command);
        logger.log(Level.INFO, "Execute " + patched + " in " + AlarmSystem.command_directory);
        CommandExecutor exe = new CommandExecutor(patched, AlarmSystem.command_directory);
        exe.call();
    }

    /** @param item Item for which to run command
     *  @param command Command that may contain '*'
     *  @return Command where '*' has been replaced by a list of PVs in alarm and their alarm severity
     */
    private static String patchCommand(final AlarmTreeItem<?> item, final String command)
    {
        if (command.indexOf('*') < 0)
            return command;
        final List<AlarmServerPV> pvs = AutomatedActionExecutor.getAlarmPVs(item);
        final StringBuilder buf = new StringBuilder();
        for (AlarmServerPV pv : pvs)
        {
            if (buf.length() > 0)
                buf.append(" ");
            buf.append(pv.getName()).append(" ").append(pv.getState().severity);
        }
        return command.replace("*", buf.toString());
    }
}
