/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.device;

import java.time.Duration;

import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.server.config.ScanConfig;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFactory;

/** Simulated device
 *  @author Kay Kasemir
 */
public class SimulatedDevice extends Device
{
    private volatile VType value = ValueFactory.newVDouble(Double.NaN);

    final private double slew_rate;

    /** Initialize
     *  @param name Name of the simulated device
     */
    public SimulatedDevice(final String name)
    {
        super(new DeviceInfo(name, name));
        slew_rate = ScanConfig.DEFAULT_SLEW_RATE;
    }

    /** Initialize
     *  @param name Name of the simulated device
     *  @param simulation_info Simulation info
     */
    public SimulatedDevice(final String name, final ScanConfig simulation_info)
    {
        super(new DeviceInfo(name, name));
        slew_rate = simulation_info.getSlewRate(name);
    }

    /** Estimate how long a device will need to reach a desired value
     *  @param device Device where change in value should be simulated
     *  @param desired_value Desired value of the device
     *  @return Estimated time in seconds for changing the device
     *  @throws Exception on error getting current value from the device
     */
    public double getChangeTimeEstimate(final double desired_value) throws Exception
    {
        // Get previous value
        final double original = VTypeHelper.toDouble(read());

        // Estimate time for update
        double time_estimate = Double.NaN;
        if (slew_rate > 0)
            time_estimate = Math.abs(desired_value - original) / slew_rate;
        if (Double.isInfinite(time_estimate)  ||  Double.isNaN(time_estimate))
            time_estimate = 1.0;
        return time_estimate;
    }

    /** {@inheritDoc} */
    @Override
    public VType read() throws Exception
    {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public void write(final Object value) throws Exception
    {
        if (value instanceof Number)
            this.value = ValueFactory.newVDouble( ((Number) value).doubleValue() );
        fireDeviceUpdate();
    }

    /** {@inheritDoc} */
    @Override
    public void write(final Object value, final Duration timeout) throws Exception
    {
        write(value);
    }
}
