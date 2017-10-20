/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.ca;

import java.util.List;

import org.phoebus.util.array.ArrayInt;
import org.phoebus.util.array.ListInt;
import org.phoebus.vtype.VStringArray;
import org.phoebus.vtype.VTypeToString;

import gov.aps.jca.dbr.DBR_String;
import gov.aps.jca.dbr.DBR_TIME_String;

/** Wrap DBR as VType
*
*  <p>Based on ideas from org.epics.pvmanager.jca, Gabriele Carcassi
*  @author Kay Kasemir
*/
public class VTypeForStringArray extends DBRAlarmTimeWrapper<DBR_TIME_String> implements VStringArray
{
    final private String[] strings;

    public VTypeForStringArray(final DBR_String dbr)
    {
        super((dbr instanceof DBR_TIME_String) ? (DBR_TIME_String) dbr : null);
        strings = dbr.getStringValue();
    }

    @Override
    public ListInt getSizes()
    {
        return new ArrayInt(strings.length);
    }

    @Override
    public List<String> getData()
    {
        return List.of(strings);
    }

    @Override
    public String toString()
    {
        return VTypeToString.toString(this);
    }
}
