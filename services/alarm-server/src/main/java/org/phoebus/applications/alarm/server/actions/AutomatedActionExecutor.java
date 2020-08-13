/*******************************************************************************
 * Copyright (c) 2018-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server.actions;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.applications.alarm.server.AlarmLogic;
import org.phoebus.applications.alarm.server.AlarmServerPV;
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
	    {
		if (AlarmLogic.getDisableNotify() == true)
		{
		    return;
		}
                EmailActionExecutor.sendEmail(item, action.detail.substring(7).split(" *, *"));
	    }
            else if (action.detail.startsWith("cmd:"))
                CommandActionExecutor.run(item, action.detail.substring(4));
            else
                logger.log(Level.WARNING, "Automated action " + action + " lacks 'mailto:' or 'cmd:' in detail");
        });
    }

    /** Locate PVs in alarm
     *
     *  @param item Item in alarm tree
     *  @return List of PVs which are currently in alarm at or below that item
     */
    public static List<AlarmServerPV> getAlarmPVs(final AlarmTreeItem<?> item)
    {
        final List<AlarmServerPV> pvs = new ArrayList<>();
        locateAlarmPVs(pvs, item);
        return pvs;
    }

    /** Locate PVs in alarm
     *  @param pvs List where PVs are to be added
     *  @param item Item in alarm tree
     */
    private static void locateAlarmPVs(final List<AlarmServerPV> pvs, final AlarmTreeItem<?> item)
    {
        if (item instanceof AlarmServerPV)
        {
            final AlarmServerPV pv = (AlarmServerPV) item;
            if (pv.getState().severity.isActive())
                pvs.add(pv);
        }
        else
            for (AlarmTreeItem<?> child : item.getChildren())
                locateAlarmPVs(pvs, child);
    }
}
