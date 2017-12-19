/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import java.time.Instant;

import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.Display;
import org.phoebus.vtype.VEnum;
import org.phoebus.vtype.VNumber;

/** Archive-derived {@link VNumber} implementation
 *  @author Kay Kasemir
 */
public class ArchiveVNumber extends ArchiveVDisplayType implements VNumber
{
    final private Number value;

    public ArchiveVNumber(final Instant timestamp,
            final AlarmSeverity severity, final String status,
            final Display display, final Number value)
    {
        super(timestamp, severity, status, display);
        this.value = value;
    }

    @Override
    public Number getValue()
    {
        return value;
    }

    /** @return Hash based on the double-typed value */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        return super.hashCode() * prime + Double.valueOf(value.doubleValue()).hashCode();
    }

    /** Compare based on the double-typed value.
     *  @param obj Other {@link VNumber} or {@link VEnum}
     *  @return <code>true</code> if the two numbers match as a 'double',
     *          so Double(3) and Integer(3) will be 'equal'
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (obj instanceof VNumber)
        {
            final VNumber number = (VNumber) obj;
            return number.getValue().doubleValue() == value.doubleValue();
        }
        if (obj instanceof VEnum)
        {
            final VEnum number = (VEnum) obj;
            return number.getIndex() == value.doubleValue();
        }
        return false;
    }

    @Override
    public String toString()
    {
        return VTypeHelper.toString(this);
    }
}
