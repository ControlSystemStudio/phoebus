/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import java.time.Instant;
import java.util.List;

import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.VEnum;
import org.phoebus.vtype.VNumber;
import org.phoebus.vtype.VString;

/** Archive-derived {@link VEnum} implementation
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArchiveVEnum extends ArchiveVType implements VEnum
{
    final private List<String> labels;
    final private int index;

    public ArchiveVEnum(final Instant timestamp,
            final AlarmSeverity severity, final String status,
            final List<String> labels, final int index)
    {
        super(timestamp, severity, status);
        this.labels = labels;
        this.index = index;
    }

    @Override
    public List<String> getLabels()
    {
        return labels;
    }

    @Override
    public String getValue()
    {
        try
        {
            return labels.get(index);
        }
        catch (RuntimeException ex)
        {
            return "Enum <" + index + ">";
        }
    }

    @Override
    public int getIndex()
    {
        return index;
    }

    /** @return Hash based on the index */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        return super.hashCode() * prime + index;
    }

    /** Compare based on index
     *  @param obj Other {@link VNumber} or {@link VEnum} or {@link VString}
     *  @return <code>true</code> if the two numbers have the same index,
     *          so Enum(3) and Integer(3) will be 'equal'
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (obj instanceof VEnum)
            return index == ((VEnum)obj).getIndex();
        if (obj instanceof VNumber)
        {
            final double dbl = ((VNumber)obj).getValue().doubleValue();
            return index == dbl;
        }
        if (obj instanceof VString)
        {
            final VString str = (VString) obj;
            return getValue().equals(str.getValue());
        }
        return false;
    }


    @Override
    public String toString()
    {
        return VTypeHelper.toString(this);
    }
}
