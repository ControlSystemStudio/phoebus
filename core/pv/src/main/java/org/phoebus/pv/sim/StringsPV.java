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

/** Simulated PV for strings "A", "AA", ...
 *  @author Kay Kasemir, based on similar code in diirt
 */
@SuppressWarnings("nls")
public class StringsPV extends SimulatedStringPV
{
    public static PV forParameters(final String name, final List<Double> parameters) throws Exception
    {
        if (parameters.size() <= 0)
            return new StringsPV(name, 10, 0.1);
        else if (parameters.size() == 1)
            return new StringsPV(name, 10, parameters.get(0));
        else if (parameters.size() == 2)
            return new StringsPV(name, parameters.get(0).intValue(), parameters.get(1));
        throw new Exception("sim://strings needs no parameters, (update_seconds) or (max length, update_seconds)");
    }

    private final StringBuffer buffer = new StringBuffer();
    private final int max_len;

    public StringsPV(final String name, final int max_len, final double update_seconds)
    {
        super(name);
        this.max_len = max_len > 1 ? max_len : 1;
        start(update_seconds);
    }

    @Override
    public String compute()
    {
        buffer.append("A");
        if (buffer.length() > max_len)
            buffer.setLength(1);
        return buffer.toString();
    }
}
