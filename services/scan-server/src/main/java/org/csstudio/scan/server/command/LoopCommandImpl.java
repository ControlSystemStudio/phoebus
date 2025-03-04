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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.csstudio.scan.command.Comparison;
import org.csstudio.scan.command.LoopCommand;
import org.csstudio.scan.device.ScanSampleHelper;
import org.csstudio.scan.server.MacroContext;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanCommandImplTool;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.SimulationContext;
import org.csstudio.scan.server.condition.NumericValueCondition;
import org.csstudio.scan.server.device.Device;
import org.csstudio.scan.server.device.SimulatedDevice;
import org.csstudio.scan.server.internal.JythonSupport;
import org.csstudio.scan.server.log.DataLog;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.util.time.TimeDuration;


/** Command that performs a loop
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LoopCommandImpl extends ScanCommandImpl<LoopCommand>
{
    final private boolean reverse;
    final private List<ScanCommandImpl<?>> implementation;
    private int direction = 1;

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
    public LoopCommandImpl(final LoopCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);
        reverse = (command.getStart() <= command.getEnd()  &&  command.getStepSize() < 0) ||
                (command.getStart() >= command.getEnd()  &&  command.getStepSize() > 0);
        implementation = ScanCommandImplTool.implement(command.getBody(), jython);
    }

    /** Initialize without Jython support
     *  @param command Command description
     */
    public LoopCommandImpl(final LoopCommand command) throws Exception
    {
        this(command, null);
    }

    /** {@inheritDoc} */
    @Override
    public long getWorkUnits()
    {
        final long iterations = getNumSteps();
        long body_units = 0;
        for (ScanCommandImpl<?> command : implementation)
            body_units += command.getWorkUnits();
        if (body_units == 0)
            return iterations;
        return iterations * body_units;
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
        for (ScanCommandImpl<?> command : implementation)
        {
            final String[] names = command.getDeviceNames(macros);
            for (String name : names)
                device_names.add(name);
        }
        return device_names.toArray(new String[device_names.size()]);
    }

    private double getLoopStart()
    {
        return Math.min(command.getStart(), command.getEnd());
    }
    private double getLoopEnd()
    {
        return Math.max(command.getStart(), command.getEnd());
    }
    private double getLoopStep()
    {
        final double step  = direction * command.getStepSize();
        // Revert direction for next iteration of the complete loop?
        if (reverse)
            direction = -direction;
        return step;
    }

    public int getNumSteps() {
        return (int)Math.ceil(Math.abs(((command.getEnd() - command.getStart()) / command.getStepSize()))) + 1;
    }

    /** {@inheritDoc} */
    @Override
    public void simulate(final SimulationContext context) throws Exception
    {
        final SimulatedDevice device = context.getDevice(context.getMacros().resolveMacros(command.getDeviceName()));

        final double step  = getLoopStep();
        // step is < 0 means we are stepping down
        double start = step < 0 ? getLoopEnd() : getLoopStart();
        int num_steps = getNumSteps();
        for (int i = 0; i < num_steps; i++)
            simulateStep(context, device, start + i * step);
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

        // Estimate execution time
        final double time_estimate = command.getWait()
                ? device.getChangeTimeEstimate(value)
                : 0.0;

        // Show command
        final StringBuilder buf = new StringBuilder();
        buf.append("Loop '").append(command.getDeviceName()).append("' = ").append(value);
        command.appendConditionDetail(buf);
        if (! Double.isNaN(original))
            buf.append(" [was ").append(original).append("]");
        context.logExecutionStep(context.getMacros().resolveMacros(buf.toString()), time_estimate);

        // Set to (simulated) new value
        device.write(value);

        // Simulate loop body
        context.simulate(implementation);
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext context) throws Exception
    {
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
            condition = new NumericValueCondition(readback, Comparison.EQUALS,
                        command.getStart(),
                        command.getTolerance(),
                        TimeDuration.ofSeconds(check_timeout));
        }
        else
            condition = null;

        double step  = getLoopStep();
        // step is < 0 means we are stepping down
        double start = step < 0 ? getLoopEnd() : getLoopStart();
        int num_steps = getNumSteps();

        for (int i = 0; i < num_steps; i++)
            executeStep(context, device, condition, readback, start + i * step);
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
        logger.log(Level.INFO, "Loop setting {0} = {1}{2}", new Object[] { device.getAlias(), value, (condition!=null ? " (waiting)" : "") });

        // Set device to value for current step of loop
        do_skip = false;
        synchronized (this)
        {
            thread = Thread.currentThread();
        }
        try
        {
            if (command.getCompletion())
                device.write(value, TimeDuration.ofSeconds(command.getTimeout()));
            else
                device.write(value);

            // .. wait for device to reach value
            if (condition != null)
            {
                condition.setDesiredValue(value);
                condition.await();
            }

            // Log the device's value?
            if (context.isAutomaticLogMode())
            {
                final DataLog log = context.getDataLog().get();
                final long serial = log.getNextScanDataSerial();
                log.log(readback.getAlias(), ScanSampleHelper.createSample(serial, readback.read()));
            }
        }
        catch (InterruptedException ex)
        {   // Ignore if 'next' was requested
            if (! do_skip)
                throw ex;
        }
        finally
        {
            synchronized (this)
            {
                thread = null;
            }
        }

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
        return command.toString() + (reverse ? " (reversing)" : "");
    }
}
