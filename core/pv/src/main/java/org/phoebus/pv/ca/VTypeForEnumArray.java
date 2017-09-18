/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.ca;

import java.util.ArrayList;
import java.util.List;

import org.diirt.util.array.ArrayInt;
import org.diirt.util.array.ListInt;
import org.diirt.vtype.VEnumArray;
import org.diirt.vtype.VTypeToString;

import gov.aps.jca.dbr.DBR_TIME_Enum;
import gov.aps.jca.dbr.LABELS;

/** Wrap DBR as VType
 *
 *  <p>Based on ideas from org.epics.pvmanager.jca, Gabriele Carcassi
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypeForEnumArray extends DBRAlarmTimeWrapper<DBR_TIME_Enum> implements VEnumArray
{
    final private LABELS labels;

    public VTypeForEnumArray(final LABELS labels, final DBR_TIME_Enum dbr)
    {
        super(dbr);
        this.labels = labels;
    }

    @Override
    public List<String> getLabels()
    {
        return List.of(labels.getLabels());
    }

    @Override
    public ListInt getSizes()
    {
        return new ArrayInt(dbr.getEnumValue().length);
    }

    @Override
    public List<String> getData()
    {
        final String[] labels = this.labels.getLabels();
        final short[] enum_indices = dbr.getEnumValue();
        final List<String> result = new ArrayList<>(enum_indices.length);
        for (int i=0; i<enum_indices.length; ++i)
        {
            final int index = enum_indices[i];
            if (labels != null  &&  index >= 0 && index < labels.length)
                result.add(labels[index]);
            else
                result.add("<" + index + ">");
        }
        return result;
    }

    @Override
    public ListInt getIndexes()
    {
        final short[] enum_indices = dbr.getEnumValue();
        return new ListInt()
        {
            @Override
            public int size()
            {
                return enum_indices.length;
            }

            @Override
            public int getInt(final int index)
            {
                return enum_indices[index];
            }
        };
    }

    @Override
    public String toString()
    {
        return VTypeToString.toString(this);
    }
}
