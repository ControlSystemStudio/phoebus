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

/** Simulated PV for noise
 *  @author Kay Kasemir, based on similar code in org.csstudio.utility.pv and diirt
 */
@SuppressWarnings("nls")
public class NoisePV extends SimulatedDoublePV
{
    public static PV forParameters(final String name, final List<Double> parameters) throws Exception
    {
        if (parameters.size() <= 0)
            return new NoisePV(name, -5, 5, 1);
        else if (parameters.size() == 3)
            return new NoisePV(name, parameters.get(0), parameters.get(1), parameters.get(2));
        throw new Exception("sim://noise needs no parameters or (min, max, update_seconds)");
    }

    private final double min, range;

    public NoisePV(final String name, final double min, final double max, final double update_seconds)
    {
        super(name);
        this.min = min;
        this.range = max - min;
        start(min, max, update_seconds);
    }

    @Override
    public double compute()
    {
        return min + Math.random() * range;
    }
}
