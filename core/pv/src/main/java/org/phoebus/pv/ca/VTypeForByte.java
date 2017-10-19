/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.ca;

import org.phoebus.vtype.VByte;
import org.phoebus.vtype.VTypeToString;

import gov.aps.jca.dbr.DBR_TIME_Byte;
import gov.aps.jca.dbr.GR;

/** Wrap DBR as VType
 *
 *  <p>Based on ideas from org.epics.pvmanager.jca, Gabriele Carcassi
 *  @author Kay Kasemir
 */
public class VTypeForByte extends DBRAlarmTimeDisplayWrapper<DBR_TIME_Byte> implements VByte
{
    public VTypeForByte(final GR metadata, final DBR_TIME_Byte dbr)
    {
        super(metadata, dbr);
    }

    @Override
    public Byte getValue()
    {
        return dbr.getByteValue()[0];
    }

    @Override
    public String toString()
    {
        return VTypeToString.toString(this);
    }
}
