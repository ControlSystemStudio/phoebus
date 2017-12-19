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
import org.phoebus.vtype.VEnum;
import org.phoebus.vtype.VString;

/** Archive-derived {@link VString} implementation
 *  @author Kay Kasemir
 */
public class ArchiveVString extends ArchiveVType implements VString
{
    final private String value;

    public ArchiveVString(final Instant timestamp,
            final AlarmSeverity severity, final String status,
            final String value)
    {
        super(timestamp, severity, status);
        this.value = value;
    }

    @Override
    public String getValue()
    {
        return value;
    }

    /** @return Hash based on the text */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        return super.hashCode() * prime + value.hashCode();
    }

    /** Compare based on the text.
     *  @param obj Other {@link VString} or {@link VEnum}
     *  @return <code>true</code> if the two strings match
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
            return true;
        if (! super.equals(obj))
            return false;
        if (obj instanceof VString)
        {
            final VString str = (VString) obj;
            return value.equals(str.getValue());
        }
        if (obj instanceof VEnum)
        {
            final VEnum str = (VEnum) obj;
            return value.equals(str.getValue());
        }
        return false;
    }

    @Override
    public String toString()
    {
        return VTypeHelper.toString(this);
    }
}
