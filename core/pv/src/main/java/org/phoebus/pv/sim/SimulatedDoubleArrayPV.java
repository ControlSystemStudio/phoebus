/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sim;

import org.epics.util.array.ArrayDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VType;

/** Base for simulated array PVs
 *
 *  <p>Value is of type VDoubleArray.
 *  If there is a valid min/max range,
 *  display settings use it with
 *  warnings generated at 20%/80% of range,
 *  alarms at 10%/90% of range.
 *
 *  @author Kay Kasemir, based on similar code in org.csstudio.utility.pv
 */
abstract public class SimulatedDoubleArrayPV extends SimulatedPV
{
    /** Display for value updates, also defines warning/alarm range */
    protected Display display;

    /** @param name Full PV name */
    public SimulatedDoubleArrayPV(final String name)
    {
        super(name);
    }

    /** Init. 'display' and start periodic updates
     *  @param min Display ..
     *  @param max .. range
     *  @param update_seconds Update period in seconds
     */
    protected void start(final double min, final double max, final double update_seconds)
    {
        display = SimulatedDoublePV.createDisplay(min, max);
        super.start(update_seconds);
    }

    /** Called by periodic timer */
    @Override
    protected void update()
    {
        final double value[] = compute();
        // Creates vtype with alarm according to display warning/alarm ranges
        final VType vtype = VDoubleArray.of(ArrayDouble.of(value),
                                            Alarm.none(), Time.now(), display);
        notifyListenersOfValue(vtype);
    }

    /** Invoked for periodic update.
     *  @return Current value of the simulated PV
     */
    abstract public double[] compute();
}
