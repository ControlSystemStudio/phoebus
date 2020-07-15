/*******************************************************************************
 * Copyright (c) 2011-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.server.command;

import java.time.Duration;
import java.time.Instant;

import org.csstudio.scan.command.WaitCommand;
import org.csstudio.scan.server.MacroContext;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.SimulationContext;
import org.csstudio.scan.server.condition.DeviceCondition;
import org.csstudio.scan.server.condition.NumericValueCondition;
import org.csstudio.scan.server.condition.TextValueCondition;
import org.csstudio.scan.server.device.Device;
import org.csstudio.scan.server.device.SimulatedDevice;
import org.csstudio.scan.server.internal.JythonSupport;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.util.time.SecondsParser;
import org.phoebus.util.time.TimeDuration;

/** {@link ScanCommandImpl} that delays the scan until a device reaches a certain value
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WaitCommandImpl extends ScanCommandImpl<WaitCommand>
{
    /** Currently 'await'-ed condition or null when nobody's waiting*/
    private volatile DeviceCondition condition = null;
    private volatile Instant started = null;

    /** {@inheritDoc} */
    public WaitCommandImpl(final WaitCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);
    }

    /** Implement without Jython support */
    public WaitCommandImpl(final WaitCommand command) throws Exception
    {
        this(command, null);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getDeviceNames(final MacroContext macros) throws Exception
    {
        return new String[] { macros.resolveMacros(command.getDeviceName()) };
    }

    /** {@inheritDoc} */
    @Override
    public void simulate(final SimulationContext context) throws Exception
    {
        final SimulatedDevice device = context.getDevice(context.getMacros().resolveMacros(command.getDeviceName()));

        // Estimate execution time
        final double time_estimate;
        double original = VTypeHelper.toDouble(device.read());
        Object desired = command.getDesiredValue();
        if (desired instanceof Number)
        {
            switch (command.getComparison())
            {
            case INCREASE_BY:
                if (Double.isNaN(original))
                {
                    original = 0.0;
                    device.write(original);
                }
                desired = original + ((Number) desired).doubleValue();
                break;
            case DECREASE_BY:
                if (Double.isNaN(original))
                {
                    original = 0.0;
                    device.write(original);
                }
                desired = original - ((Number) desired).doubleValue();
                break;
            default:
                break;
            }
            time_estimate = device.getChangeTimeEstimate(((Number) desired).doubleValue());
        }
        else
            time_estimate = 1.0; // Not numeric, no known slew rate

        // Show command
        final StringBuilder buf = new StringBuilder();
        buf.append(command.toString());
        if (! Double.isNaN(original))
            buf.append(" [was ").append(original).append("]");
        context.logExecutionStep(buf.toString(), time_estimate);

        // Set to (simulated) new value
        device.write(desired);
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext context) throws Exception
    {
        final Device device = context.getDevice(context.getMacros().resolveMacros(command.getDeviceName()));

        final Duration timeout = TimeDuration.ofSeconds(command.getTimeout());
        final Object desired = command.getDesiredValue();
        if (desired instanceof Number)
        {
            final double number = ((Number)desired).doubleValue();
            condition = new NumericValueCondition(device, command.getComparison(),
                    number, command.getTolerance(), timeout);
        }
        else
            condition = new TextValueCondition(device, command.getComparison(), desired.toString(), timeout);
        try
        {
            started = Instant.now();
            condition.await();
        }
        finally
        {
            condition = null;
            started = null;
        }
        context.workPerformed(1);
    }

    /** {@inheritDoc} */
    @Override
    public void next()
    {
        final DeviceCondition safe_copy = condition;
        if (safe_copy != null)
            safe_copy.complete();
    }

    @Override
    public String toString()
    {
        String info = super.toString();
        final Instant start = started;
        if (start != null)
        {
            final Instant now = Instant.now();
            final Duration duration = Duration.between(start, now);
            info += ". Elapsed: " + SecondsParser.formatSeconds(duration.getSeconds() + duration.getNano()*1e-9);
        }
        return info;
    }
}
