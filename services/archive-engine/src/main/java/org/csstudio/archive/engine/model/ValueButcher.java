/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.model;

import org.csstudio.archive.writer.rdb.TimestampHelper;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Time;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

/** Helper that does various unspeakable things to values.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ValueButcher
{
    // These strings match the text representation of the
    // ChannelArchiver's informational severity codes
    // - if they existed in previous versions.

    /** Status string for 'disabled' state */
    private static final String DISABLED = "Archive_Disabled";

    /** Status string for 'disconnected' state */
    private static final String DISCONNECTED = "Disconnected";

    /** Status string for 'off' state */
    private static final String OFF = "Archive_Off";

    /** Status string for 'write error' state */
    private static final String WRITE_ERROR = "Write_Error";

    /** @return Info value to indicate disabled state */
    public static VType createDisabled()
    {
        return createInfoSample(DISABLED);
    }

    /** @return Info value to indicate disconnected state */
    public static VType createDisconnected()
    {
        return createInfoSample(DISCONNECTED);
    }

    /** @return Info value to indicate that archive was turned off */
    public static VType createOff()
    {
        return createInfoSample(OFF);
    }

    /** @return Info value to indicate write error */
    public static VType createWriteError()
    {
        return createInfoSample(WRITE_ERROR);
    }

    /** Create sample with status set to some info */
    private static VType createInfoSample(final String info)
    {
        return VString.of(info, Alarm.of(AlarmSeverity.INVALID, AlarmStatus.CLIENT, info), Time.now());
    }

    /** @param value Value
     *  @return "Time Value [Sevr/Stat]"
     */
    public static String format(final VType value)
    {
        if (value == null)
            return "null";

        final StringBuilder buf = new StringBuilder();
        buf.append(TimestampHelper.format(VTypeHelper.getTimestamp(value)))
           .append(' ')
           .append(VTypeHelper.toString(value));

        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm != null)
            buf.append(" [")
               .append(alarm.getSeverity())
               .append('/')
               .append(alarm.getStatus())
               .append(']');
        return buf.toString();
    }
}
