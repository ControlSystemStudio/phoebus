/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.sys;

import java.time.Instant;

import org.phoebus.pv.sim.SimulatedStringPV;
import org.phoebus.util.time.TimestampFormats;

/** System "time" PV
 *  @author Kay Kasemir, based on similar code in diirt
 */
public class TimePV extends SimulatedStringPV
{
    public TimePV(final String name)
    {
        super(name);
        start(1);
    }

    @Override
    public String compute()
    {
        return TimestampFormats.SECONDS_FORMAT.format(Instant.now());
    }
}
