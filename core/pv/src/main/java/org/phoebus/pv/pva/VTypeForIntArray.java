/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import java.util.List;

import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStructure;
import org.phoebus.util.array.ArrayInt;
import org.phoebus.util.array.ListInt;
import org.phoebus.vtype.ArrayDimensionDisplay;
import org.phoebus.vtype.VIntArray;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.VTypeToString;
import org.phoebus.vtype.ValueUtil;

/** Hold/decode data of {@link PVStructure} in {@link VType}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class VTypeForIntArray extends VTypeTimeAlarmDisplayBase implements VIntArray
{
    final private ListInt value;

    public VTypeForIntArray(final PVStructure struct)
    {
        super(struct);
        final PVScalarArray pv_array = struct.getSubField(PVScalarArray.class, "value");
        final int length = pv_array.getLength();
        final int[] data = new int[length];
        PVStructureHelper.convert.toIntArray(pv_array, 0, length, data, 0);
        value = new ArrayInt(data);
    }

    @Override
    public List<ArrayDimensionDisplay> getDimensionDisplay()
    {
        return ValueUtil.defaultArrayDisplay(this);
    }

    @Override
    public ListInt getSizes()
    {
        return new ArrayInt(value.size());
    }

    @Override
    public ListInt getData()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return VTypeToString.toString(this);
    }
}
