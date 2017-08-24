/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sim;

import java.util.List;
import java.util.Random;

import org.phoebus.pv.PV;

/** Simulated PV for gaussian noise
 *  @author Kay Kasemir, based on similar PV in diirt
 */
@SuppressWarnings("nls")
public class GaussianNoisePV extends SimulatedDoublePV
{
    private static final Random rand = new Random();
    private final double center, std_dev;

    public static PV forParameters(final String name, List<Double> parameters) throws Exception
    {
        if (parameters.isEmpty())
            return new GaussianNoisePV(name, 0.0, 1.0, 0.1);
        if (parameters.size() == 3)
            return new GaussianNoisePV(name, parameters.get(0), parameters.get(1), parameters.get(2));
        throw new Exception("sim://gaussianNoise needs no parameters or (center, std_dev, update_seconds)");
    }

    public GaussianNoisePV(final String name, final double center, final double std_dev, final double update_seconds)
    {
        super(name);
        this.center = center;
        this.std_dev = std_dev;
        start(center-2*std_dev, center+2*std_dev, update_seconds);
    }

    @Override
    public double compute()
    {
        return center + rand.nextGaussian() * std_dev;
    }
}
