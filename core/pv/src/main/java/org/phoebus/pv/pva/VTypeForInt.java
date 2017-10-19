/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVStructure;
import org.phoebus.vtype.VInt;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.VTypeToString;

/** Hold/decode data of {@link PVStructure} in {@link VType}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class VTypeForInt extends VTypeTimeAlarmDisplayBase implements VInt
{
    final private int value;

    public VTypeForInt(final PVStructure struct)
    {
        super(struct);
        value = PVStructureHelper.convert.toInt(struct.getSubField(PVScalar.class, "value"));
    }

    @Override
    public Integer getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return VTypeToString.toString(this);
    }
}
