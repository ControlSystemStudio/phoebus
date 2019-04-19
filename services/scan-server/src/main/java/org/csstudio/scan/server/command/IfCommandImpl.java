/*******************************************************************************
 * Copyright (c) 2011-2019 Oak Ridge National Laboratory.
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

import org.csstudio.scan.command.IfCommand;
import org.csstudio.scan.server.MacroContext;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanCommandImplTool;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.SimulationContext;
import org.csstudio.scan.server.condition.NumericValueCondition;
import org.csstudio.scan.server.condition.TextValueCondition;
import org.csstudio.scan.server.device.Device;
import org.csstudio.scan.server.internal.JythonSupport;

/** Command that performs 'if'
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class IfCommandImpl extends ScanCommandImpl<IfCommand>
{
    final private List<ScanCommandImpl<?>> implementation;

    /** Initialize
     *  @param command Command description
     *  @param jython Jython interpreter, may be <code>null</code>
     */
    public IfCommandImpl(final IfCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);
        implementation = ScanCommandImplTool.implement(command.getBody(), jython);
    }

    /** Initialize without Jython support
     *  @param command Command description
     */
    public IfCommandImpl(final IfCommand command) throws Exception
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
        return body_units;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getDeviceNames(final MacroContext macros) throws Exception
    {
        final String device_name = command.getDeviceName();
        final Set<String> device_names = new HashSet<>();
        device_names.add(macros.resolveMacros(device_name));
        for (ScanCommandImpl<?> command : implementation)
        {
            final String[] names = command.getDeviceNames(macros);
            for (String name : names)
                device_names.add(name);
        }
        return device_names.toArray(new String[device_names.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public void simulate(final SimulationContext context) throws Exception
    {
        // Show command
        context.logExecutionStep(context.getMacros().resolveMacros(command.toString()) + ":", 0.1);
        // Simulate body
        context.simulate(implementation);
        context.logExecutionStep("End If", 0.0);
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext context) throws Exception
    {
        final Device device = context.getDevice(context.getMacros().resolveMacros(command.getDeviceName()));

        final Object desired = command.getDesiredValue();
        final boolean is_condition_met;
        if (desired instanceof Number)
        {
            final double number = ((Number)desired).doubleValue();
            final NumericValueCondition condition = new NumericValueCondition(device, command.getComparison(),
                                                  number, command.getTolerance(), Duration.ZERO);
            condition.fetchInitialValue();
            is_condition_met = condition.isConditionMet();
        }
        else
        {
            final TextValueCondition condition = new TextValueCondition(device, command.getComparison(), desired.toString(), Duration.ZERO);
            condition.fetchInitialValue();
            is_condition_met = condition.isConditionMet();
        }

        // Perform body or not
        logger.log(Level.INFO, "'If' checking {0}: ", device);
        if (is_condition_met)
            context.execute(implementation);
    }
}
