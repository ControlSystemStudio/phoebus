/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sim;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.phoebus.pv.PV;

/** Simulated PV for gaussian wave
 *  @author Kay Kasemir, based on similar PV in diirt
 */
@SuppressWarnings("nls")
public class GaussianWavePV extends SimulatedDoubleArrayPV
{
    private final double period;
    private final double[] shape;
    private final Instant start = Instant.now();

    public static PV forParameters(final String name, List<Double> parameters) throws Exception
    {
        if (parameters.isEmpty())
            return new GaussianWavePV(name, 1.0, 100.0, 100, 0.1);
        if (parameters.size() == 4)
            return new GaussianWavePV(name, parameters.get(0), parameters.get(1), parameters.get(2).intValue(), parameters.get(3));
        throw new Exception("sim://gaussianwave needs no parameters or" +
                            "(period_seconds, std_dev, size, update_seconds)");
    }

    public GaussianWavePV(final String name, final double period_seconds, final double std_dev,
                          int size, final double update_seconds)
    {
        super(name);
        period = period_seconds;

        if (size < 1)
            size = 1;

        shape = new double[size];
        final double center = size / 2.0;
        for (int i=0; i<size; ++i)
        {
            final double dx = i - center;
            shape[i] = Math.exp(- dx*dx / std_dev);
        }
        start(0.0, 1.0, update_seconds);
    }

    @Override
    public double[] compute()
    {
        final double[] value = new double[shape.length];

        final Duration dist = Duration.between(start, Instant.now());
        final double t = dist.getSeconds() + dist.getNano()*1e-9;
        final double periods = period > 0 ? t / period : 0.0;
        final int i0 = (int) ((periods - (int)periods) * value.length);

        // value[..] = shape[i0 to end] concatenated with shape[0 to i0-1]
        final int rest = value.length - i0;
        System.arraycopy(shape, i0, value, 0, rest);
        System.arraycopy(shape, 0, value, rest, i0);
        return value;
    }
}
