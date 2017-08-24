/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sim;

import java.util.List;

import org.phoebus.pv.PV;

/** Simulated PV for ramp
 *  @author Kay Kasemir, based on similar code in org.csstudio.utility.pv and diirt
 */
@SuppressWarnings("nls")
public class RampPV extends SimulatedDoublePV
{
    public static PV forParameters(final String name, final List<Double> parameters) throws Exception
    {
        if (parameters.size() <= 0)
            return new RampPV(name, -5, 5, 1, 1);
        else if (parameters.size() == 3)
            return new RampPV(name, parameters.get(0), parameters.get(1), 1, parameters.get(2));
        else if (parameters.size() == 4)
            return new RampPV(name, parameters.get(0), parameters.get(1), parameters.get(2), parameters.get(3));
        throw new Exception("sim://ramp needs no parameters, (min, max, update_seconds) or (min, max, step, update_seconds)");
    }

    private final double min, max, step;
    private double value = 0;

    public RampPV(final String name, final double min, final double max, final double step, final double update_seconds)
    {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step == 0 ? 1 : step;
        this.value = min - step; // First call to compute() will return 'min'
        start(min, max, update_seconds);
    }

    @Override
    public double compute()
    {
        value += step;
        if (value > max)
            value = min;
        else if (value < min)
            value = max;
        return value;
    }
}
