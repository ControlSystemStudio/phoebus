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
import org.csstudio.scan.util.PVReference;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.util.time.TimeDuration;


/** Command that performs a loop
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WhileCommandImpl extends ScanCommandImpl<WhileCommand>
{
    private final List<ScanCommandImpl<?>> implementation;

    /** Thread that executes the loop variable write
     *
     *  SYNC on `this` to prevent race where
     *  executing thread tries to set thread back to null,
     *  while next() tries to interrupt.
     */
    private Thread thread = null;

    /** Flag to indicate 'next' was invoked on current loop step */
    private volatile boolean doSkip;

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
        long bodyUnits = 0;
        for (ScanCommandImpl<?> command : implementation)
            bodyUnits += command.getWorkUnits();
        if (bodyUnits == 0)
            return 0;
        return bodyUnits;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getDeviceNames(final MacroContext macros) throws Exception
    {
        final String deviceName = command.getDeviceName();
        final Set<String> deviceNames = new HashSet<String>();
        deviceNames.add(macros.resolveMacros(deviceName));
        if (command.getWait()  &&  !command.getReadback().isEmpty())
            deviceNames.add(macros.resolveMacros(command.getReadback()));
        // If desired_value is a PV reference, that PV also needs to be opened
        final Object desired = command.getDesiredValue();
        if (desired instanceof PVReference pvReference)
            deviceNames.add(macros.resolveMacros(pvReference.getPVName()));
        for (ScanCommandImpl<?> command : implementation)
        {
            final String[] names = command.getDeviceNames(macros);
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, () -> "getDeviceNames: names=" + Arrays.toString(names));
            java.util.Collections.addAll(deviceNames, names);
        }
        return deviceNames.toArray(new String[deviceNames.size()]);
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

        final NumericValueCondition condition = createWaitCondition(context, readback);

        logger.info("execute before while(true)...");
        while (true)
        {
            final boolean is_condition_met = isConditionMet(context, device);
            if (!is_condition_met) break;
            executeStep(context, device, condition, 0.0);
        }
    }

    private NumericValueCondition createWaitCondition(final ScanContext context, final Device readback) throws Exception
    {
        if (!command.getWait())
            return null;

        final double check_timeout = command.getCompletion() ? 1.0 : command.getTimeout();
        final double number = resolveDesiredNumericValue(context, command.getDesiredValue());
        return new NumericValueCondition(readback, Comparison.EQUALS,
                number,
                command.getTolerance(),
                TimeDuration.ofSeconds(check_timeout));
    }

    private boolean isConditionMet(final ScanContext context, final Device device) throws Exception
    {
        final Object desired = command.getDesiredValue();
        if (desired instanceof Number number)
            return evaluateNumericCondition(device, number.doubleValue());
        if (desired instanceof PVReference pvReference)
            return evaluateNumericCondition(device, resolvePVReferenceValue(context, pvReference));
        return evaluateTextCondition(device, desired.toString());
    }

    private boolean evaluateNumericCondition(final Device device, final double desiredValue) throws Exception
    {
        final NumericValueCondition condition = new NumericValueCondition(device, command.getComparison(),
                desiredValue, command.getTolerance(), Duration.ZERO);
        condition.fetchInitialValue();
        return condition.isConditionMet();
    }

    private boolean evaluateTextCondition(final Device device, final String desiredValue) throws Exception
    {
        final TextValueCondition condition = new TextValueCondition(device, command.getComparison(), desiredValue, Duration.ZERO);
        condition.fetchInitialValue();
        return condition.isConditionMet();
    }

    private double resolveDesiredNumericValue(final ScanContext context, final Object desired) throws Exception
    {
        if (desired instanceof PVReference pvReference)
            return resolvePVReferenceValue(context, pvReference);
        return ((Number) desired).doubleValue();
    }

    private double resolvePVReferenceValue(final ScanContext context, final PVReference pvReference) throws Exception
    {
        final Device refDevice = context.getDevice(
                context.getMacros().resolveMacros(pvReference.getPVName()));
        return VTypeHelper.toDouble(refDevice.read(ScanServerInstance.getScanConfig().getReadTimeout()));
    }

    /** Execute one step of the loop
     *  @param context
     *  @param device
     *  @param condition
     *  @param value
     *  @throws Exception
     */
    private void executeStep(final ScanContext context, final Device device,
            final NumericValueCondition condition, final double value)
            throws Exception
    {
        logger.log(Level.INFO, "While setting {0} = {1}{2}", new Object[] { device.getAlias(), value, (condition!=null ? " (waiting)" : "") });

        // Set device to value for current step of loop
        doSkip = false;
        synchronized (this)
        {
            thread = Thread.currentThread();
        }

        try
        {
            // Execute loop body or show some estimate of progress
            // (not including nested commands)
            if (doSkip)
                context.workPerformed(implementation.size());
            else
                context.execute(implementation);
        }
        finally
        {
            synchronized (this)
            {
                thread = null;
            }
        }

        // If there are no commands that inc. the work units, do it yourself
        if (implementation.isEmpty())
            context.workPerformed(1);
    }

    /** {@inheritDoc} */
    @Override
    public void next()
    {
        doSkip = true;
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
