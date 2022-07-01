/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sys;

import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

import java.util.List;
import java.util.stream.Collectors;

/** PV Factory for system PVs
 *  @author Kay Kasemir, Kunal Shroff
 */
@SuppressWarnings("nls")
public class SysPVFactory implements PVFactory
{
    /** PV type implemented by this factory */
    final public static String TYPE = "sys";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public PV createPV(final String name, final String base_name) throws Exception
    {
        // Determine system pv function name and (optional) parameters
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

        if (func.equals("time"))
            return new TimePV(name);
        else if (func.equals("timeOffset"))
            return TimeOffsetPV.forParameters(name, List.of(parameters.split(",")).stream().map(String::strip).collect(Collectors.toList()));
        else
            throw new Exception("Unknown system PV " + base_name);
    }
}
