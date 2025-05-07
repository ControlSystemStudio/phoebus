/*******************************************************************************
 * Copyright (c) 2018-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server.actions;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.server.AlarmServerPV;
import org.phoebus.email.EmailService;
import org.phoebus.util.time.TimestampFormats;

/** Executor for email actions
 *
 *  <p>Handles automated actions with the following detail:
 *
 *  <p>"mailto:user@site.org,another@else.com"<br>
 *  Sends email with alarm detail to list of recipients.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EmailActionExecutor
{
    /** @param item Item for which to send email
     *  @param addresses Recipients
     */
    static void sendEmail(final AlarmTreeItem<?> item, final String[] addresses)
    {
        logger.log(Level.INFO, item.getPathName() + ": Send email to " + Arrays.toString(addresses));

        final String title = createTitle(item);
        final String body = createBody(item);
        for (String address : addresses)
            try
            {
                EmailService.send(address, AlarmSystem.automated_email_sender, title, body);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failed to email to " + address, ex);
            }
    }

    /** Create title for email, also used by Info PV 
     *  @param item Item for which to create title
     *  @return Title
     */
    static String createTitle(final AlarmTreeItem<?> item)
    {
        final StringBuilder buf = new StringBuilder();

        // "MAJOR alarm: .." or just "OK:"
        final SeverityLevel severity = item.getState().severity;
        buf.append(severity);
        if (severity != SeverityLevel.OK)
            buf.append(" alarm");
        buf.append(": ");

        // PV's description or path to item
        if (item instanceof AlarmServerPV)
        {
            final AlarmServerPV pv = (AlarmServerPV)item;
            buf.append(pv.getDescription());
        }
        else
            buf.append(item.getPathName());

        return buf.toString();
    }

    /** Create info body for email, also used by Info PV 
     *  @param item Item for which to create info
     *  @return Info text
     */
    static String createBody(final AlarmTreeItem<?> item)
    {
        final StringBuilder buf = new StringBuilder();

        if (item instanceof AlarmServerPV)
            addPVDetail(buf, (AlarmServerPV)item);
        else
        {
            final List<AlarmServerPV> pvs = AutomatedActionExecutor.getAlarmPVs(item);
            if (pvs.isEmpty())
                buf.append("No active alarms\n");
            else
            {
                buf.append("PVs:\n\n");
                for (AlarmServerPV pv : pvs)
                    addPVDetail(buf, pv);
            }
        }

        return buf.toString();
    }

    private static void addPVDetail(final StringBuilder buf, final AlarmServerPV pv)
    {
        final String[] path_elements = AlarmTreePath.splitPath(pv.getPathName());
        final String path = AlarmTreePath.makePath(path_elements, path_elements.length - 1);

        buf.append("PV: ").append(path).append(' ').append(pv.getName()).append("\n")
           .append("Description: ").append(pv.getDescription()).append("\n");

        AlarmState state = pv.getState();
        buf.append("Alarm Time: ").append(TimestampFormats.MILLI_FORMAT.format(state.time)).append("\n");
        buf.append("Alarm Severity: ").append(state.severity).append(", ");
        buf.append("Status: ").append(state.message).append(", ");
        buf.append("Value: ").append(state.value).append("\n");

        state = pv.getCurrentState();
        buf.append("Current PV Severity: ").append(state.severity).append(", ");
        buf.append("Status: ").append(state.message).append("\n");
        buf.append("\n");
    }
}
