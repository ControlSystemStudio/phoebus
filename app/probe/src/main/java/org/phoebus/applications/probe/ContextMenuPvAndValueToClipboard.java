/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.probe;

import static org.phoebus.applications.probe.Probe.logger;

import java.util.List;
import java.util.logging.Level;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Time;
import org.epics.vtype.VType;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.vtype.FormatOption;
import org.phoebus.ui.vtype.FormatOptionHandler;
import org.phoebus.util.time.TimestampFormats;

/** Context menu entry for PV that copies PV name and value to clipboard
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ContextMenuPvAndValueToClipboard extends ContextMenuPvToClipboard
{
    @Override
    public String getName()
    {
        return Messages.CopyWithValue;
    }

    @Override
    protected String createText(final List<ProcessVariable> pvs)
    {
        final StringBuilder buf = new StringBuilder();

        for (ProcessVariable pv : pvs)
        {
            buf.append(pv.getName());

            try
            {
                // A PV obtained from the PV pool might be new,
                // needing time to connect before we can fetch a value.
                // The context menu code is on the UI thread and cannot wait for a connection.
                // This code is, however, invoked on for example a widget with an existing PV,
                // so the PV is expected to be in the pool and already connected.
                final PV active_pv = PVPool.getPV(pv.getName());
                try
                {
                    // We expect the PV to already have a value which we can display right away
                    final VType value = active_pv.read();
                    if (value != null)
                    {
                        buf.append(" ");

                        final Time time = Time.timeOf(value);
                        if (time != null)
                            buf.append(TimestampFormats.FULL_FORMAT.format(time.getTimestamp()))
                               .append(" ");

                        buf.append(FormatOptionHandler.format(value, FormatOption.DEFAULT, -1, true));

                        final Alarm alarm = Alarm.alarmOf(value);
                        if (alarm != null   &&  alarm.getSeverity() != AlarmSeverity.NONE)
                            buf.append(" ")
                               .append(alarm.getSeverity())
                               .append("/")
                               .append(alarm.getStatus());
                    }
                    // .. else: PV doesn't have a value right now. Don't wait, just don't show any data
                }
                finally
                {
                    PVPool.releasePV(active_pv);
                }
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot show value for " + pv);
            }

            buf.append("\n");
        }
        return buf.toString();
    }
}
