/*******************************************************************************
 * Copyright (c) 2012-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.device;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.time.Duration;
import java.util.logging.Level;

import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.server.ScanServerInstance;
import org.csstudio.scan.server.config.ScanConfig;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

/** Simulated device
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimulatedDevice extends Device
{
    private final double slew_rate;
    private double minimum, maximum;

    private volatile VType value = VDouble.of(Double.NaN, Alarm.none(), Time.now(), Display.none());


    /** Initialize
     *  @param name Name of the simulated device
     *  @param simulation_info Simulation info
     * @param real_devices
     */
    public SimulatedDevice(final String name, final ScanConfig simulation_info, final DeviceContext real_devices)
    {
        super(new DeviceInfo(name, name));
        slew_rate = simulation_info.getSlewRate(name);

        // Get real devices for min/max PVs
        minimum = getLimit(simulation_info.getMinimumPV(name), real_devices);
        maximum = getLimit(simulation_info.getMaximumPV(name), real_devices);
    }

    private double getLimit(final String limit_name, final DeviceContext real_devices)
    {
        if (limit_name != null)
        {
            try
            {
                final Device device = real_devices.getDevice(limit_name);
                return VTypeHelper.toDouble(device.read(ScanServerInstance.getScanConfig().getReadTimeout()));
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot read limit PV " + limit_name, ex);
            }
        }
        return Double.NaN;
    }

    /** Estimate how long a device will need to reach a desired value
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
        {
            final double new_value = ((Number) value).doubleValue();

            // Throw exception if value outside of permitted range
            if ((Double.isFinite(minimum)  &&  new_value < minimum)  ||
                (Double.isFinite(maximum)  &&  new_value > maximum))
                throw new Exception("Cannot set " + toString() + " = " + new_value + ", valid range is " + minimum + " .. " + maximum);

            this.value = VDouble.of(new_value , Alarm.none(), Time.now(), Display.none() );
        }
        fireDeviceUpdate();
    }

    /** {@inheritDoc} */
    @Override
    public void write(final Object value, final Duration timeout) throws Exception
    {
        write(value);
    }
}
