/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

import org.csstudio.scan.command.ScanCommand;

/** Hook for simulation customization
 *
 *  <p>The scan configuration file has a <code>&ltsimulation_hook></code>
 *  entry to provide the name of a Jython class that implements
 *  a custom simulation hook.
 *
 *  @author Kay Kasemir
 */
public class SimulationHook
{
    /** Called for each command to allow custom simulation
     *
     *  <p>Implementation should either return <code>false</code>
     *  to request default simulation of the command,
     *  or perform its own simulation,
     *  typically calling <code>SimulationContext#logExecutionStep("text", seconds")</code>
     *  with the result, and then return <code>true</code>.
     *
     *  @param command {@link ScanCommand} to simulate
     *  @param context {@link SimulationContext}
     *  @return <code>true</code> if hook handled the simulation of the command,
     *         <code>false</code> if default simulation should be used
     */
    public boolean handle(ScanCommand command, SimulationContext context)
    {
        return false;
    }
}
