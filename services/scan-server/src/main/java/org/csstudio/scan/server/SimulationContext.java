/*******************************************************************************
 * Copyright (c) 2012-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.info.SimulationResult;
import org.csstudio.scan.server.condition.WaitForDevicesCondition;
import org.csstudio.scan.server.config.ScanConfig;
import org.csstudio.scan.server.device.Device;
import org.csstudio.scan.server.device.DeviceContext;
import org.csstudio.scan.server.device.DeviceContextHelper;
import org.csstudio.scan.server.device.SimulatedDevice;
import org.csstudio.scan.server.internal.JythonSupport;
import org.phoebus.framework.macros.MacroHandler;
import org.python.core.PyException;

/** Context used for the simulation of {@link ScanCommandImpl}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimulationContext
{
    private final ScanConfig simulation_info;
    private final MacroContext macros;

    /** Real devices, will be connected to check connection and
     *  for reading limit PVs
     */
    private final DeviceContext real_devices = new DeviceContext();

    /** Simulated devices, will be written */
    private final Map<String, SimulatedDevice> devices = new HashMap<>();

    private final PrintStream log_stream;

    private final SimulationHook hook;

    private double simulation_seconds = 0.0;

    private final AtomicInteger parallel_level = new AtomicInteger();

    /** Initialize
     *  @param jython {@link JythonSupport}
     *  @param log_stream Stream for simulation progress log
     *  @throws Exception on error while initializing {@link SimulationInfo}
     */
    public SimulationContext(final JythonSupport jython, final PrintStream log_stream) throws Exception
    {
        // Read scan config for each simulation to allow changing slew rates etc.
        try
        {
            simulation_info = new ScanConfig(ScanServerInstance.getScanConfigURL().openStream());
        }
        catch (Exception ex)
        {
            throw new Exception("Simulation fails to read scan config", ex);
        }
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

    /** Increment level of 'Parallel' commands
     *  @return Incremented parallel command level
     */
    public int incParallelLevel()
    {
        return parallel_level.incrementAndGet();
    }

    /** Decrement level of 'Parallel' commands */
    public void decParallelLevel()
    {
        parallel_level.decrementAndGet();
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
            device = new SimulatedDevice(expanded_name, simulation_info, real_devices);
            devices.put(expanded_name, device);
        }
        return device;
    }

    /** Add error line to the simulation log
     *  @param error Line describing the error
     */
    public void logError(final String error)
    {
        log_stream.print(SimulationResult.ERROR);
        log_stream.println(error);
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

    /** Perform complete simulation: Check devices, sim commands, ...
     *  @param scan Scan implementations to simulate
     *  @throws Exception
     */
    public void performSimulation(final List<ScanCommandImpl<?>> scan) throws Exception
    {
        // Collect all devices used by the commands
        DeviceContextHelper.addScanDevices(real_devices, macros, scan);

        // Add min/max devices
        final List<String> range_pvs = new ArrayList<>();
        for (Device device : real_devices.getDevices())
        {
            String pv = simulation_info.getMinimumPV(device.getName());
            if (pv != null)
                range_pvs.add(pv);
            pv = simulation_info.getMaximumPV(device.getName());
            if (pv != null)
                range_pvs.add(pv);
        }
        for (String pv : range_pvs)
            real_devices.addPVDevice(new DeviceInfo(pv));

        // Check connection
        real_devices.startDevices();
        try
        {
            logger.log(Level.INFO, "Check device connections...");
            final WaitForDevicesCondition connect = new WaitForDevicesCondition(real_devices.getDevices());
            final Duration timeout = ScanServerInstance.getScanConfig().getReadTimeout();
            if (! connect.await(timeout.toMillis(), TimeUnit.MILLISECONDS))
                for (Device device : real_devices.getDevices())
                    if (! device.isReady())
                        logError("Cannot access " + device);

            // Simulate commands
            try
            {
                simulate(scan);
            }
            catch (Exception ex)
            {
                logError(ex.getMessage());
                logger.log(Level.WARNING, "Simulation fails", ex);
            }
        }
        finally
        {
            real_devices.stopDevices();
        }
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
