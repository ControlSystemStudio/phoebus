/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.ca;

import org.diirt.vtype.VShort;
import org.diirt.vtype.VTypeToString;

import gov.aps.jca.dbr.DBR_TIME_Short;
import gov.aps.jca.dbr.GR;

/** Wrap DBR as VType
 *
 *  <p>Based on ideas from org.epics.pvmanager.jca, Gabriele Carcassi
 *  @author Kay Kasemir
 */
public class VTypeForShort extends DBRAlarmTimeDisplayWrapper<DBR_TIME_Short> implements VShort
{
    public VTypeForShort(final GR metadata, final DBR_TIME_Short dbr)
    {
        super(metadata, dbr);
    }

    @Override
    public Short getValue()
    {
        return dbr.getShortValue()[0];
    }

    @Override
    public String toString()
    {
        return VTypeToString.toString(this);
    }
}
