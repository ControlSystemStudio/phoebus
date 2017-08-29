/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sim;

import java.text.NumberFormat;

import org.diirt.util.array.ArrayDouble;
import org.diirt.vtype.Display;
import org.diirt.vtype.VType;
import org.diirt.vtype.ValueFactory;
import org.diirt.vtype.ValueUtil;

/** Base for simulated array PVs
 *
 *  <p>Value is of type VDoubleArray.
 *  If there is a valid min/max range,
 *  display settings use it with
 *  warnings generated at 20%/80% of range,
 *  alarms at 10%/90% of range.
 *
 *  @author Kay Kasemir, based on similar code in org.csstudio.utility.pv and diirt
 */
@SuppressWarnings("nls")
abstract public class SimulatedDoubleArrayPV extends SimulatedPV
{
    /** Format for Display */
    private final static NumberFormat format = ValueUtil.getDefaultNumberFormat();

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
        final double range = max - min;
        if (range > 0)
            display = ValueFactory.newDisplay(min, min + range * 0.1, min + range * 0.2, "a.u.", format,
                                              min + range * 0.8, min + range * 0.9, max, min, max);
        else
            display = ValueFactory.newDisplay(0.0, Double.NaN, Double.NaN, "a.u.", format,
                                              Double.NaN, Double.NaN, 10.0, 0.0, 10.0);
        super.start(update_seconds);
    }

    /** Called by periodic timer */
    @Override
    protected void update()
    {
        final double value[] = compute();
        // Creates vtype with alarm according to display warning/alarm ranges
        final VType vtype = ValueFactory.newVDoubleArray(new ArrayDouble(value),
                                                         ValueFactory.alarmNone(),
                                                         ValueFactory.timeNow(),
                                                         display);
        notifyListenersOfValue(vtype);
    }

    /** Invoked for periodic update.
     *  @return Current value of the simulated PV
     */
    abstract public double[] compute();
}
