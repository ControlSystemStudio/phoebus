/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.csstudio.scan.command.SequenceCommand;
import org.csstudio.scan.server.MacroContext;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanCommandImplTool;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.SimulationContext;
import org.csstudio.scan.server.internal.JythonSupport;

/** Command that executes the commands in its body
 *  @author Kay Kasemir
 */
public class SequenceCommandImpl extends ScanCommandImpl<SequenceCommand>
{
    final private List<ScanCommandImpl<?>> implementation;

    /** Initialize
     *  @param command Command description
     *  @param jython Jython interpreter, may be <code>null</code>
     */
    public SequenceCommandImpl(final SequenceCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);
        implementation = ScanCommandImplTool.implement(command.getBody(), jython);
    }

    /** Initialize without Jython support
     *  @param command Command description
     */
    public SequenceCommandImpl(final SequenceCommand command) throws Exception
    {
        this(command, null);
    }

    /** {@inheritDoc} */
    @Override
    public long getWorkUnits()
    {
        long body_units = 1; // One unit for this command, rest for body
        for (ScanCommandImpl<?> command : implementation)
            body_units += command.getWorkUnits();
        return body_units;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getDeviceNames(final MacroContext macros) throws Exception
    {
        final Set<String> device_names = new HashSet<String>();
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
        context.simulate(implementation);
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext context) throws Exception
    {
        context.workPerformed(1);
        for (ScanCommandImpl<?> body_command : implementation)
            context.execute(body_command);
    }

    // public void next()
    // is not implemented because the 'next' command
    // will be sent to the active body command.
    // Once they have all been completed, the sequence completes.

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return command.toString();
    }
}
