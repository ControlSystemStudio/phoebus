/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver;

import org.epics.vtype.AlarmSeverity;

/** Wrapper for {@link AlarmSeverity} that adds ChannelArchiver detail
 *  @author Kay Kasemir
 */
public class SeverityInfo
{
    final private AlarmSeverity severity;
    final private String text;
    final private boolean has_value;
    final private boolean txt_stat;

    SeverityInfo(final AlarmSeverity severity, final String text, final boolean has_value,
            final boolean txt_stat)
    {
        this.severity = severity;
        this.text = text;
        this.has_value = has_value;
        this.txt_stat = txt_stat;
    }

    public AlarmSeverity getSeverity()
    {
        return severity;
    }

    public String getText()
    {
        return text;
    }

    public boolean hasValue()
    {
        return has_value;
    }

    public boolean statusIsText()
    {
        return txt_stat;
    }

    @Override
    public String toString()
    {
        return text;
    }
}
