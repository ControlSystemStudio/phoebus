/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server.actions;

// Eclipse may report that "javax.mail.Session" and "Transport" are not accessible,
// but it still compiles and runs ?!

// Eclipse also keeps deleting this import:
// import static org.phoebus.applications.alarm.AlarmSystem.logger;
import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.server.AlarmServerPV;
import org.phoebus.email.EmailPreferences;
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

        final Properties props = new Properties();
        props.put("mail.smtp.host", EmailPreferences.mailhost);
        props.put("mail.smtp.port", EmailPreferences.mailport);

        final Session session = Session.getDefaultInstance(props);

        for (String address : addresses)
            try
            {
                final Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(AlarmSystem.automated_email_sender));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(address));
                message.setSubject(title);
                message.setText(body);

                Transport.send(message);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failed to email to " + address, ex);
            }
    }

    private static String createTitle(final AlarmTreeItem<?> item)
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

    private static String createBody(final AlarmTreeItem<?> item)
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
