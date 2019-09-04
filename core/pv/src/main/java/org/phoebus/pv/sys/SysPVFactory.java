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

/** PV Factory for system PVs
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SysPVFactory implements PVFactory
{
    final public static String TYPE = "sys";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public PV createPV(final String name, final String base_name) throws Exception
    {
        if (base_name.equals("time"))
            return new TimePV(name);
        else
            throw new Exception("Unknown system PV " + base_name);
    }
}
