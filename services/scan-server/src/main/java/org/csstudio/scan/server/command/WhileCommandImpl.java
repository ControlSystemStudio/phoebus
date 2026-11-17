/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
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

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.Arrays;

import org.csstudio.scan.command.Comparison;
import org.csstudio.scan.command.WhileCommand;
import org.csstudio.scan.device.ScanSampleHelper;
import org.csstudio.scan.server.MacroContext;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanCommandImplTool;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.ScanServerInstance;
import org.csstudio.scan.server.SimulationContext;
import org.csstudio.scan.server.condition.NumericValueCondition;
import org.csstudio.scan.server.condition.TextValueCondition;
import org.csstudio.scan.server.device.Device;
import org.csstudio.scan.server.device.SimulatedDevice;
import org.csstudio.scan.server.internal.JythonSupport;
import org.csstudio.scan.server.log.DataLog;
import org.csstudio.scan.util.PVReference;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.util.time.TimeDuration;


/** Command that performs a loop
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WhileCommandImpl extends ScanCommandImpl<WhileCommand>
{
    final private List<ScanCommandImpl<?>> implementation;

    /** Thread that executes the loop variable write
     *
     *  SYNC on `this` to prevent race where
     *  executing thread tries to set thread back to null,
     *  while next() tries to interrupt.
     */
    private Thread thread = null;

    /** Flag to indicate 'next' was invoked on current loop step */
    private volatile boolean do_skip;

    /** Initialize
     *  @param command Command description
     *  @param jython Jython interpreter, may be <code>null</code>
     */
    public WhileCommandImpl(final WhileCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);
        implementation = ScanCommandImplTool.implement(command.getBody(), jython);
    }

    /** Initialize without Jython support
     *  @param command Command description
     */
    public WhileCommandImpl(final WhileCommand command) throws Exception
    {
        this(command, null);
    }

    /** {@inheritDoc} */
    @Override
    public long getWorkUnits()
    {
        long body_units = 0;
        for (ScanCommandImpl<?> command : implementation)
            body_units += command.getWorkUnits();
        if (body_units == 0)
            return 0;
        return body_units;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getDeviceNames(final MacroContext macros) throws Exception
    {
        final String device_name = command.getDeviceName();
        final Set<String> device_names = new HashSet<String>();
        device_names.add(macros.resolveMacros(device_name));
        if (command.getWait()  &&  command.getReadback().length() > 0)
            device_names.add(macros.resolveMacros(command.getReadback()));
        // If desired_value is a PV reference, that PV also needs to be opened
        final Object desired = command.getDesiredValue();
        if (desired instanceof PVReference)
            device_names.add(macros.resolveMacros(((PVReference) desired).getPVName()));
        for (ScanCommandImpl<?> command : implementation)
        {
            final String[] names = command.getDeviceNames(macros);
            logger.info("getDeviceNames: names="+ Arrays.toString(names));
            for (String name : names) {
                device_names.add(name);
            }
        }
        return device_names.toArray(new String[device_names.size()]);
    }


    /** {@inheritDoc} */
    @Override
    public void simulate(final SimulationContext context) throws Exception
    {
        final SimulatedDevice device = context.getDevice(context.getMacros().resolveMacros(command.getDeviceName()));

        simulateStep(context, device, 0.0);
    }

    /** Simulate one step in the loop iteration
     *  @param context {@link SimulationContext}
     *  @param device {@link SimulatedDevice} that the loop modifies
     *  @param value Value of the loop variable for this iteration
     *  @throws Exception on error
     */
    private void simulateStep(final SimulationContext context,
            final SimulatedDevice device, final double value) throws Exception
    {
        // Get previous value
        final double original = VTypeHelper.toDouble(device.read());


        // Show command
        final StringBuilder buf = new StringBuilder();
        final double time_estimate = command.getTimeout();
        buf.append("While '").append(command.getDeviceName()).append("' = ").append(value);
        command.appendConditionDetail(buf);
        if (! Double.isNaN(original))
            buf.append(" [was ").append(original).append("]");
        context.logExecutionStep(context.getMacros().resolveMacros(buf.toString()), time_estimate);


        // Simulate loop body
        context.simulate(implementation);
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext context) throws Exception
    {
        logger.info("execute begin");
        final Device device = context.getDevice(context.getMacros().resolveMacros(command.getDeviceName()));

        // Separate read-back device, or use 'set' device?
        final Device readback;
        if (command.getReadback().isEmpty())
            readback = device;
        else
            readback = context.getDevice(context.getMacros().resolveMacros(command.getReadback()));

        //  Wait for the device to reach the value?
        final NumericValueCondition condition;
        if (command.getWait())
        {
            // When using completion, readback needs to match "right away"
            final double check_timeout = command.getCompletion() ? 1.0 : command.getTimeout();
            final Object desired = command.getDesiredValue();
            final double number;
            if (desired instanceof PVReference)
            {
                final Device ref_device = context.getDevice(
                        context.getMacros().resolveMacros(((PVReference) desired).getPVName()));
                number = VTypeHelper.toDouble(ref_device.read(ScanServerInstance.getScanConfig().getReadTimeout()));
            }
            else
                number = ((Number)desired).doubleValue();
            condition = new NumericValueCondition(readback, Comparison.EQUALS,
                        number,
                        command.getTolerance(),
                        TimeDuration.ofSeconds(check_timeout));
        }
        else
            condition = null;

        logger.info("execute before while(true)...");
        while (true) {

            final Object desired = command.getDesiredValue();
            final boolean is_condition_met;
            if (desired instanceof Number)
            {
                final double number = ((Number)desired).doubleValue();
                final NumericValueCondition condit1 = new NumericValueCondition(device, command.getComparison(),
                                                  number, command.getTolerance(), Duration.ZERO);
                condit1.fetchInitialValue();
                is_condition_met = condit1.isConditionMet();
            }
            else if (desired instanceof PVReference)
            {
                // Read the referenced PV's current value at runtime and compare numerically
                final Device ref_device = context.getDevice(
                        context.getMacros().resolveMacros(((PVReference) desired).getPVName()));
                final double ref_value = VTypeHelper.toDouble(
                        ref_device.read(ScanServerInstance.getScanConfig().getReadTimeout()));
                final NumericValueCondition condit1 = new NumericValueCondition(device, command.getComparison(),
                                                  ref_value, command.getTolerance(), Duration.ZERO);
                condit1.fetchInitialValue();
                is_condition_met = condit1.isConditionMet();
            }
            else
            {
                final TextValueCondition condit1 = new TextValueCondition(device, command.getComparison(), desired.toString(), Duration.ZERO);
                condit1.fetchInitialValue();
                is_condition_met = condit1.isConditionMet();
            }
            if (!is_condition_met) break;
            executeStep(context, device, condition, readback, 0.0);
		}
    }

    /** Execute one step of the loop
     *  @param context
     *  @param device
     *  @param condition
     *  @param readback
     *  @param value
     *  @throws Exception
     */
    private void executeStep(final ScanContext context, final Device device,
            final NumericValueCondition condition, final Device readback, double value)
            throws Exception
    {
        logger.log(Level.INFO, "While setting {0} = {1}{2}", new Object[] { device.getAlias(), value, (condition!=null ? " (waiting)" : "") });

        // Set device to value for current step of loop
        do_skip = false;

        // Execute loop body or show some estimate of progress
        // (not including nested commands)
        if (do_skip)
            context.workPerformed(implementation.size());
        else
            context.execute(implementation);

        // If there are no commands that inc. the work units, do it yourself
        if (implementation.size() <= 0)
            context.workPerformed(1);
    }

    /** {@inheritDoc} */
    @Override
    public void next()
    {
        do_skip = true;
        synchronized (this)
        {
            if (thread != null)
                thread.interrupt();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return command.toString();
    }
}
