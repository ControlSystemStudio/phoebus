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

/** Simulated PV for noise wave
 *  @author Kay Kasemir, based on similar PV in org.csstudio.utility.pv and diirt
 */
@SuppressWarnings("nls")
public class NoiseWavePV extends SimulatedDoubleArrayPV
{
    private final double min, range;
    private final int size;

    public static PV forParameters(final String name, List<Double> parameters) throws Exception
    {
        if (parameters.isEmpty())
            return new NoiseWavePV(name, -5, 5, 100, 1.0);
        if (parameters.size() == 3)
            return new NoiseWavePV(name, parameters.get(0), parameters.get(1), 100, parameters.get(2));
        if (parameters.size() == 4)
            return new NoiseWavePV(name, parameters.get(0), parameters.get(1), parameters.get(2).intValue(), parameters.get(3));
        throw new Exception("sim://noisewave needs no parameters, " +
                            "(min, max, update_seconds) or" +
                            "(min, max, size, update_seconds)");
    }

    public NoiseWavePV(final String name, final double min, final double max,
                       int size, final double update_seconds)
    {
        super(name);

        // Empty array is OK, but not negative size
        if (size <= 0)
            size = 0;

        this.min = min;
        this.range =  max - min;
        this.size = size;
        start(min, max, update_seconds);
    }

    @Override
    public double[] compute()
    {
        final double[] value = new double[size];
        for (int i=0; i<size; ++i)
            value[i] = min + Math.random() * range;
        return value;
    }
}
