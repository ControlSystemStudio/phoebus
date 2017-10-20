/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sim;

import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFactory;

/** Base for simulated text PVs
 *  @author Kay Kasemir, based on similar code in org.csstudio.utility.pv and diirt
 */
abstract public class SimulatedStringPV extends SimulatedPV
{
    /** @param name Full PV name */
    public SimulatedStringPV(final String name)
    {
        super(name);
    }

    /** Called by periodic timer */
    @Override
    protected void update()
    {
        final String value = compute();
        final VType vtype = ValueFactory.newVString(value, ValueFactory.alarmNone(), ValueFactory.timeNow());
        notifyListenersOfValue(vtype);
    }

    /** Invoked for periodic update.
     *  @return Current value of the simulated PV
     */
    abstract public String compute();
}
