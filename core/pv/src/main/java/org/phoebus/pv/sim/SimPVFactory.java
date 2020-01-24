/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

/** PV Factory for simulated PVs
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimPVFactory implements PVFactory
{
    final public static String TYPE = "sim";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public PV createPV(final String name, final String base_name) throws Exception
    {
        // Determine simulation function name and (optional) parameters
        final String func, parameters;
        int sep = base_name.indexOf('(');
        if (sep < 0)
        {
            func = base_name;
            parameters = "";
        }
        else
        {
            final int end = base_name.lastIndexOf(')');
            if (end < 0)
                throw new Exception("Missing closing bracket for parameters in '" + name + "'");
            func = base_name.substring(0, sep);
            parameters = base_name.substring(sep+1, end);
        }

        if (func.equals("sine"))
            return SinePV.forParameters(name, parseDoubles(parameters));
        else if (func.equals("ramp"))
            return RampPV.forParameters(name, parseDoubles(parameters));
        else if (func.equals("noise"))
            return NoisePV.forParameters(name, parseDoubles(parameters));
        else if (func.equalsIgnoreCase("gaussiannoise"))
            return GaussianNoisePV.forParameters(name, parseDoubles(parameters));
        else if (func.equals("strings"))
            return StringsPV.forParameters(name, parseDoubles(parameters));
        else if (func.startsWith("intermittent"))           // diirt used "intermittentChannel"
            return IntermittentPV.forParameters(name, parseDoubles(parameters));
        else if (func.startsWith("sawtooth"))               // diirt used "sawtoothWaveform"
            return SawtoothWavePV.forParameters(name, parseDoubles(parameters));
        else if (func.toLowerCase().startsWith("sinewave")) // diirt used "sineWaveform"
            return SineWavePV.forParameters(name, parseDoubles(parameters));
        else if (func.toLowerCase().startsWith("gaussianwave")) // diirt used "gaussianWaveform"
            return GaussianWavePV.forParameters(name, parseDoubles(parameters));
        else if (func.toLowerCase().startsWith("noisewave")) // diirt used "noiseWaveform"
            return NoiseWavePV.forParameters(name, parseDoubles(parameters));
        else if (func.equals("flipflop"))
            return FlipFlopPV.forParameters(name, parseDoubles(parameters));
        else if (func.equals("const"))
            return ConstPV.create(name, base_name);
        else
            throw new Exception("Unknown simulated PV " + name);
    }

    /** @param parameters Parameters of the form "3.14, 1e-7, 17"
     *  @return Numbers parsed from the parameters
     *  @throws Exception
     */
    private List<Double> parseDoubles(final String parameters) throws Exception
    {
        final List<Double> result = new ArrayList<>();
        final StringTokenizer tokenizer = new StringTokenizer(parameters, " \t,", false);
        while (tokenizer.hasMoreTokens())
        {
            final String item = tokenizer.nextToken();
            try
            {
                result.add(Double.parseDouble(item));
            }
            catch (NumberFormatException ex)
            {
                throw new Exception("Cannot parse number from '" + item + "'");
            }
        }
        return result;
    }
}
