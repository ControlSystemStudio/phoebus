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

/** Simulated PV for sine
 *  @author Kay Kasemir, based on similar PV in org.csstudio.utility.pv and diirt
 */
@SuppressWarnings("nls")
public class SinePV extends SimulatedDoublePV
{
    private final double min, range, step;
    private double x = 0;

    public static PV forParameters(final String name, List<Double> parameters) throws Exception
    {
        if (parameters.isEmpty())
            return new SinePV(name, -5.0, 5.0, 10.0, 1.0);
        if (parameters.size() == 3)
            return new SinePV(name, parameters.get(0), parameters.get(1), 10.0, parameters.get(2));
        if (parameters.size() == 4)
            return new SinePV(name, parameters.get(0), parameters.get(1), parameters.get(2), parameters.get(3));
        throw new Exception("sim://sine needs no parameters or (min, max, update_seconds) or (min, max, steps, update_seconds)");
    }

    public SinePV(final String name, final double min, final double max, final double steps, final double update_seconds)
    {
        super(name);
        this.min = min;
        this.range = max - min;
        this.step = 2.0*Math.PI / Math.max(steps, 1);
        start(min, max, update_seconds);
    }

    @Override
    public double compute()
    {
        final double value = min + (Math.sin(x)+1.0)/2.0 * range;
        x += step;
        return value;
    }
}
