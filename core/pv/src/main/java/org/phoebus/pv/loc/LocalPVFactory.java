/*******************************************************************************
 * Copyright (c) 2014-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.loc;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.epics.vtype.VBoolean;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VInt;
import org.epics.vtype.VLong;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;
import org.phoebus.pv.PVPool;

/** Factory for creating {@link LocalPV}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LocalPVFactory implements PVFactory
{
    final public static String TYPE = "loc";

    /** Map of local PVs */
    private static final Map<String, LocalPV> local_pvs = new HashMap<>();

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getCoreName(final String name)
    {
        int sep = name.indexOf('<');
        if (sep > 0)
            return name.substring(0, sep);
        sep = name.indexOf('(');
        if (sep > 0)
            return name.substring(0, sep);
        return name;
    }

    @Override
    public PV createPV(final String name, final String base_name) throws Exception
    {
        final String[] ntv = ValueHelper.parseName(base_name);

        // Actual name: loc://the_pv  without <type> or (initial value)
        final String actual_name = LocalPVFactory.TYPE + PVPool.SEPARATOR + ntv[0];

        // Info for initial value, null if nothing provided
        final List<String> initial_value = ValueHelper.splitInitialItems(ntv[2]);

        // Determine type from initial value or use given type
        final Class<? extends VType> type = ntv[1] == null
                                          ? determineValueType(initial_value)
                                          : parseType(ntv[1]);
        LocalPV pv;
        // TODO Use ConcurrentHashMap, computeIfAbsent
        synchronized (local_pvs)
        {
            pv = local_pvs.get(actual_name);
            if (pv == null)
            {
                pv = new LocalPV(actual_name, type, initial_value);
                local_pvs.put(actual_name, pv);
            }
            else
                pv.checkInitializer(type, initial_value);
        }
        return pv;
    }

    public static Class<? extends VType> determineValueType(final List<String> items) throws Exception
    {
        if (items == null)
            return VDouble.class;

        if (ValueHelper.haveInitialStrings(items))
        {
            if (items.size() == 1)
                return VString.class;
            else
                return VStringArray.class;
        }
        else
        {
            if (items.size() == 1)
                return VDouble.class;
            else
                return VDoubleArray.class;
        }
    }

    public static Class<? extends VType> parseType(final String type) throws Exception
    {   // Lenient check, ignore case and allow partial match
        final String lower = type.toLowerCase();
        if (lower.contains("doublearray"))
            return VDoubleArray.class;
        if (lower.contains("double")) // 'VDouble', 'vdouble', 'double'
            return VDouble.class;
        if (lower.contains("stringarray"))
            return VStringArray.class;
        if (lower.contains("string"))
            return VString.class;
        if (lower.contains("enum"))
            return VEnum.class;
        if (lower.contains("long"))
            return VLong.class;
        if (lower.contains("int"))
            return VInt.class;
        if (lower.contains("boolean"))
            return VBoolean.class;
        if (lower.contains("table"))
            return VTable.class;
        throw new Exception("Local PV cannot handle type '" + type + "'");
    }

    /** Remove local PV from pool
     *  To be called by LocalPV when closed
     *  @param pv {@link LocalPV}
     */
    static void releasePV(final LocalPV pv)
    {
        synchronized (local_pvs)
        {
            local_pvs.remove(pv.getName());
        }
    }

    // For unit test
    public static Collection<LocalPV> getLocalPVs()
    {
        synchronized (local_pvs)
        {
            return local_pvs.values();
        }
    }
}
