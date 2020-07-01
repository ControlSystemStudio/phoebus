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

import java.util.ArrayList;
import java.util.List;

import org.csstudio.scan.command.SetCommand;
import org.csstudio.scan.server.MacroContext;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.SimulationContext;
import org.csstudio.scan.server.WriteHelper;
import org.csstudio.scan.server.device.SimulatedDevice;
import org.csstudio.scan.server.internal.JythonSupport;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.util.time.TimeDuration;

/** {@link ScanCommandImpl} that sets a device to a value
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SetCommandImpl extends ScanCommandImpl<SetCommand>
{
    private volatile WriteHelper write = null;

    /** {@inheritDoc} */
    public SetCommandImpl(final SetCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);
    }

    /** Implement without Jython support */
    public SetCommandImpl(final SetCommand command) throws Exception
    {
        this(command, null);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getDeviceNames(final MacroContext macros) throws Exception
    {
        final List<String> names = new ArrayList<>(2);
        names.add(macros.resolveMacros(command.getDeviceName()));
        final String readback = command.getReadback();
        if (command.getWait()  &&  readback.length() > 0)
            names.add(macros.resolveMacros(readback));
        return names.toArray(new String[names.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public void simulate(final SimulationContext context) throws Exception
    {
        final SimulatedDevice device = context.getDevice(context.getMacros().resolveMacros(command.getDeviceName()));

        // Get previous and desired values
        final double original = VTypeHelper.toDouble(device.read());
        final double desired;
        if (command.getValue() instanceof Number)
            desired = ((Number) command.getValue()).doubleValue();
        else
            desired = Double.NaN;

        // Estimate execution time
        final double time_estimate = command.getWait()
                ? device.getChangeTimeEstimate(desired)
                : 0.0;

        // Show command
        final StringBuilder buf = new StringBuilder();
        buf.append("Set '").append(command.getDeviceName()).append("' = ").append(command.getValue());
        // XXX Isn't resolving macros in readback, but that's only for the simu printout
        command.appendConditionDetail(buf);
        if (! Double.isNaN(original))
            buf.append(" [was ").append(original).append("]");
        context.logExecutionStep(buf.toString(), time_estimate);

        // Set to (simulated) new value
        device.write(command.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext context)  throws Exception
    {
        final MacroContext macros = context.getMacros();
        write = new WriteHelper(context,
                                macros.resolveMacros(command.getDeviceName()),
                                command.getValue(),
                                command.getCompletion(), command.getWait(),
                                macros.resolveMacros(command.getReadback()),
                                command.getReadbackValue(),
                                command.getTolerance(),
                                TimeDuration.ofSeconds(command.getTimeout()));
        try
        {
            write.perform();
        }
        finally
        {
            write = null;
        }
        context.workPerformed(1);
    }

    /** {@inheritDoc} */
    @Override
    public void next()
    {
        final WriteHelper save_copy = write;
        if (save_copy != null)
            save_copy.cancel();
    }
}
