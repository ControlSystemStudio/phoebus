/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sim;

import java.util.List;

import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.pv.loc.LocalPVFactory;
import org.phoebus.pv.loc.ValueHelper;

/** Constant PV
 *  @author Kay Kasemir
 */
public class ConstPV extends PV
{
    public static PV create(final String name, final String base_name) throws Exception
    {
        final String[] ntv = ValueHelper.parseName(base_name);

        // Info for initial value, null if nothing provided
        final List<String> initial_value = ValueHelper.splitInitialItems(ntv[2]);

        // Determine type from initial value or use given type
        final Class<? extends VType> type = ntv[1] == null
                                          ? LocalPVFactory.determineValueType(initial_value)
                                          : LocalPVFactory.parseType(ntv[1]);

        final VType value = ValueHelper.getInitialValue(initial_value, type);
        return new ConstPV(name, value);
     }

    private ConstPV(final String name, final VType value)
    {
        super(name);
        notifyListenersOfValue(value);
    }
}
