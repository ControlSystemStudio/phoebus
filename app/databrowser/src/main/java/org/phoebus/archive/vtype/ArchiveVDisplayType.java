/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import java.text.NumberFormat;
import java.time.Instant;

import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.Display;

/** Base of archive-derived {@link VType} implementations that include {@link Display}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArchiveVDisplayType extends ArchiveVType implements Display
{
    final private Display display;

    public ArchiveVDisplayType(final Instant timestamp,
            final AlarmSeverity severity, final String status,
            final Display display)
    {
        super(timestamp, severity, status);
        this.display = display;
    }

    @Override
    public Double getLowerDisplayLimit()
    {
        return display == null ? Double.NaN : display.getLowerDisplayLimit();
    }

    @Override
    public Double getLowerCtrlLimit()
    {
        return display == null ? Double.NaN : display.getLowerCtrlLimit();
    }

    @Override
    public Double getLowerAlarmLimit()
    {
        return display == null ? Double.NaN : display.getLowerAlarmLimit();
    }

    @Override
    public Double getLowerWarningLimit()
    {
        return display == null ? Double.NaN : display.getLowerWarningLimit();
    }

    @Override
    public String getUnits()
    {
        return display == null ? "" : display.getUnits();
    }

    @Override
    public NumberFormat getFormat()
    {
        if (display != null)
            return display.getFormat();
        return null;
    }

    @Override
    public Double getUpperWarningLimit()
    {
        return display == null ? Double.NaN : display.getUpperWarningLimit();
    }

    @Override
    public Double getUpperAlarmLimit()
    {
        return display == null ? Double.NaN : display.getUpperAlarmLimit();
    }

    @Override
    public Double getUpperCtrlLimit()
    {
        return display == null ? Double.NaN : display.getUpperCtrlLimit();
    }

    @Override
    public Double getUpperDisplayLimit()
    {
        return display == null ? Double.NaN : display.getUpperDisplayLimit();
    }

    // hashCode() and equals() are specifically NOT implemented:
    // Values are compared based on their timestamp and alarm via ArchiveVType,
    // combined with the actual value from the derived class (ArchiveVNumber, ...).
    // The display meta data is NOT used as part of the comparisons.
}
