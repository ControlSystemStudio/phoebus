/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.csstudio.scan.server.config.ScanConfig;
import org.csstudio.scan.server.device.SimulatedDevice;
import org.phoebus.framework.macros.MacroHandler;
import org.python.core.PyException;

/** Context used for the simulation of {@link ScanCommandImpl}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimulationContext
{
    final private ScanConfig simulation_info;
    private final MacroContext macros;

    final private Map<String, SimulatedDevice> devices = new HashMap<String, SimulatedDevice>();

    final private PrintStream log_stream;

    private final SimulationHook hook;

    private double simulation_seconds = 0.0;

    /** Initialize
     *  @param jython {@link JythonSupport}
     *  @param log_stream Stream for simulation progress log
     *  @throws Exception on error while initializing {@link SimulationInfo}
     */
    public SimulationContext(final JythonSupport jython, final PrintStream log_stream) throws Exception
    {
        simulation_info = ScanServerInstance.getScanConfig();
        macros = new MacroContext(simulation_info.getMacros());
        this.log_stream = log_stream;

        final String hook_name = simulation_info.getSimulationHook();
        if (hook_name.isEmpty())
            hook = null;
        else
            try
            {
                hook = jython.loadClass(SimulationHook.class, hook_name);
            }
            catch (PyException ex)
            {
                throw new Exception(JythonSupport.getExceptionMessage(ex), ex);
            }
    }

    /** @return Macro support */
    public MacroContext getMacros()
    {
        return macros;
    }

    /** @return Current time of simulation in seconds */
    public double getSimulationSeconds()
    {
        return simulation_seconds;
    }

    /** @return Current time of simulation, "HH:MM:SS" */
    public String getSimulationTime()
    {
        double time = simulation_seconds;
        final long hours = (long) (time / (60*60));
        time -= hours * (60 * 60);
        final long minutes = (long) (time / 60);
        time -= minutes * 60;
        final long secs = (long) (time);
        time -= secs;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /** @param name Device name
     *  @return {@link SimulatedDevice}
     *  @throws Exception on error in macro handling
     */
    public SimulatedDevice getDevice(final String name) throws Exception
    {
        final String expanded_name = MacroHandler.replace(macros, name);
        SimulatedDevice device = devices.get(expanded_name);
        if (device == null)
        {
            device = new SimulatedDevice(expanded_name, simulation_info);
            devices.put(expanded_name, device);
        }
        return device;
    }

    /** Log information about the currently simulated command
     *  @param info End-user readable description what the command would do when executed
     *  @param seconds Estimated time in seconds that the command would take if executed
     */
    public void logExecutionStep(final String info, final double seconds)
    {
        log_stream.print(getSimulationTime());
        log_stream.print(" - ");
        log_stream.println(info);
        simulation_seconds += seconds;
    }

    /** @param scan Scan implementations to simulate
     *  @throws Exception
     */
    public void simulate(final List<ScanCommandImpl<?>> scan) throws Exception
    {
        for (ScanCommandImpl<?> impl : scan)
            if (hook == null  ||  ! hook.handle(impl.getCommand(), this))
                impl.simulate(this);
    }
}
