/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.ca;

import java.util.List;

import org.diirt.vtype.VEnum;
import org.diirt.vtype.VTypeToString;

import gov.aps.jca.dbr.DBR_TIME_Enum;
import gov.aps.jca.dbr.LABELS;

/** Wrap DBR as VType
 *
 *  <p>Based on ideas from org.epics.pvmanager.jca, Gabriele Carcassi
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypeForEnum extends DBRAlarmTimeWrapper<DBR_TIME_Enum> implements VEnum
{
    final private LABELS labels;

    public VTypeForEnum(final LABELS labels, final DBR_TIME_Enum dbr)
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
    public String getValue()
    {
        final int index = getIndex();
        final String[] labels = this.labels.getLabels();
        if (index >= 0 && index < labels.length)
            return labels[index];
        return "<" + index + ">";
    }

    @Override
    public int getIndex()
    {
        return dbr.getEnumValue()[0];
    }

    @Override
    public String toString()
    {
        return VTypeToString.toString(this);
    }
}
