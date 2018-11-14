/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sim;

import java.text.NumberFormat;

import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;

/** Base for simulated scalar PVs
 *
 *  <p>Value is of type VDouble.
 *  If there is a valid min/max range,
 *  display settings use it with
 *  warnings generated at 20%/80% of range,
 *  alarms at 10%/90% of range.
 *
 *  @author Kay Kasemir, based on similar code in org.csstudio.utility.pv and diirt
 */
@SuppressWarnings("nls")
abstract public class SimulatedDoublePV extends SimulatedPV
{
    /** Format for Display */
    final static NumberFormat format = NumberFormats.precisionFormat(2);

    /** Display for value updates, also defines warning/alarm range */
    protected Display display;

    /** @param name Full PV name */
    public SimulatedDoublePV(final String name)
    {
        super(name);
    }

    static Display createDisplay(final double min, final double max)
    {
        final double range = max - min;
        if (range > 0)
            return Display.of(Range.of(min, max),
                              Range.of(min + range * 0.1, min + range * 0.9),
                              Range.of(min + range * 0.2, min + range * 0.8),
                              Range.of(min, max),
                              "a.u.", format);
        return Display.of(Range.of(0, 10),
                          Range.undefined(),
                          Range.undefined(),
                          Range.of(0, 10),
                          "a.u.", format);

    }

    /** Init. 'display' and start periodic updates
     *  @param min Display ..
     *  @param max .. range
     *  @param update_seconds Update period in seconds
     */
    protected void start(final double min, final double max, final double update_seconds)
    {
        display = createDisplay(min, max);
        super.start(update_seconds);
    }

    /** Called by periodic timer */
    @Override
    protected void update()
    {
        final double value = compute();
        // Creates vtype with alarm according to display warning/alarm ranges
        final VType vtype = VDouble.of(value, display.newAlarmFor(value), Time.now(), display);
        notifyListenersOfValue(vtype);
    }

    /** Invoked for periodic update.
     *  @return Current value of the simulated PV
     */
    abstract public double compute();
}
