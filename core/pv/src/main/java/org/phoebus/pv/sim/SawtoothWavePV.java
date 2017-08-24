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

/** Simulated PV for sawtooth wave
 *  @author Kay Kasemir, based on similar PV in org.csstudio.utility.pv and diirt
 */
@SuppressWarnings("nls")
public class SawtoothWavePV extends SimulatedDoubleArrayPV
{
    private final double min, range, period, wavelength;
    private final int size;
    private final Instant start = Instant.now();

    public static PV forParameters(final String name, List<Double> parameters) throws Exception
    {
        if (parameters.isEmpty())
            return new SawtoothWavePV(name, 1.0, 10.0, 50, 0.1, -1, +1);
        if (parameters.size() == 4)
            return new SawtoothWavePV(name, parameters.get(0), parameters.get(1), parameters.get(2).intValue(), parameters.get(3), -1, +1);
        if (parameters.size() == 6)
            return new SawtoothWavePV(name, parameters.get(0), parameters.get(1), parameters.get(2).intValue(), parameters.get(3),
                                        parameters.get(4), parameters.get(5));
        throw new Exception("sim://sawtooth needs no parameters, " +
                            "(period_seconds, wavelength, size, update_seconds) or" +
                            "(period_seconds, wavelength, size, update_seconds, min, max)");
    }

    public SawtoothWavePV(final String name, final double period_seconds, double sample_wavelength,
                      int size, final double update_seconds, final double min, final double max)
    {
        super(name);

        // Avoid 1/zero later
        if (size <= 0)
            size = 10;
        if (sample_wavelength <= 0)
            sample_wavelength = size;

        this.min = min;
        // Adjust range so value (almost) reaches max
        this.range =  (max - min) * (1.0 + 1/sample_wavelength);
        this.period = period_seconds;
        this.size = size;
        this.wavelength = sample_wavelength;
        start(min, max, update_seconds);
    }

    @Override
    public double[] compute()
    {
        final Duration dist = Duration.between(start, Instant.now());
        final double t = dist.getSeconds() + dist.getNano()*1e-9;
        final double x0 = period > 0 ? t / period : 0.0;

        final double[] value = new double[size];
        for (int i=0; i<size; ++i)
        {
            final double x = x0 + i / wavelength;
            value[i] = min + (x - (long)x) * range;
        }
        return value;
    }
}
