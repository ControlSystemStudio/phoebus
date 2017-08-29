/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.ca;

import java.util.List;

import org.diirt.util.array.ArrayFloat;
import org.diirt.util.array.ArrayInt;
import org.diirt.util.array.ListFloat;
import org.diirt.util.array.ListInt;
import org.diirt.vtype.ArrayDimensionDisplay;
import org.diirt.vtype.VFloatArray;
import org.diirt.vtype.VTypeToString;
import org.diirt.vtype.ValueUtil;

import gov.aps.jca.dbr.DBR_TIME_Float;
import gov.aps.jca.dbr.GR;

/** Wrap DBR as VType
 *
 *  <p>Based on ideas from org.epics.pvmanager.jca, Gabriele Carcassi
 *  @author Kay Kasemir
 */
public class VTypeForFloatArray extends DBRAlarmTimeDisplayWrapper<DBR_TIME_Float> implements VFloatArray
{
    public VTypeForFloatArray(final GR metadata, final DBR_TIME_Float dbr)
    {
        super(metadata, dbr);
    }

    @Override
    public List<ArrayDimensionDisplay> getDimensionDisplay()
    {
        return ValueUtil.defaultArrayDisplay(this);
    }

    @Override
    public ListInt getSizes()
    {
        return new ArrayInt(dbr.getFloatValue().length);
    }

    @Override
    public ListFloat getData()
    {
        return new ArrayFloat(dbr.getFloatValue());
    }

    @Override
    public String toString()
    {
        return VTypeToString.toString(this);
    }
}
