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

/** Simulated PV with random value, intermittently connected
 *  @author Kay Kasemir, based on similar code in diirt
 */
@SuppressWarnings("nls")
public class IntermittentPV extends SimulatedDoublePV
{
    private boolean connected = true;

    public static PV forParameters(final String name, final List<Double> parameters) throws Exception
    {
        if (parameters.size() <= 0)
            return new IntermittentPV(name, -5, 5, 1);
        else if (parameters.size() == 1)
            return new IntermittentPV(name, -5, 5, parameters.get(0));
        else if (parameters.size() == 2)
            return new IntermittentPV(name, parameters.get(1), parameters.get(1), parameters.get(0));
        else if (parameters.size() == 3)
            return new IntermittentPV(name, parameters.get(1), parameters.get(2), parameters.get(0));
        throw new Exception("sim://intermittent needs no parameters, (update_seconds), (update_seconds, value) or (update_seconds, min, max)");
    }

    private final double min, range;

    public IntermittentPV(final String name, final double min, final double max, final double update_seconds)
    {
        super(name);
        this.min = min;
        this.range = max - min;
        start(min, max, update_seconds);
    }

    @Override
    protected void update()
    {
        if (connected)
            super.update();
        else
            notifyListenersOfDisconnect();
        connected = ! connected;
    }

    @Override
    public double compute()
    {
        return min + Math.random() * range;
    }
}
