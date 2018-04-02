/*******************************************************************************
 * Copyright (c) 2013-2018 Oak Ridge National Laboratory.
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

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.csstudio.scan.command.IncludeCommand;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.XMLCommandReader;
import org.csstudio.scan.server.MacroContext;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanCommandImplTool;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.ScanServerInstance;
import org.csstudio.scan.server.SimulationContext;
import org.csstudio.scan.server.internal.JythonSupport;
import org.csstudio.scan.server.internal.PathStreamTool;

/** {@link ScanCommandImpl} that includes another scan
 *  @author Kay Kasemir
 */
public class IncludeCommandImpl extends ScanCommandImpl<IncludeCommand>
{
    private List<ScanCommandImpl<?>> scan_impl;

    /** {@inheritDoc} */
    public IncludeCommandImpl(final IncludeCommand command, final JythonSupport jython) throws Exception
    {
        super(command, jython);

        // Parse scan
        final List<String> paths = ScanServerInstance.getScanConfig().getScriptPaths();
        final InputStream scan_stream = PathStreamTool.openStream(paths, command.getScanFile());
        final List<ScanCommand> commands = XMLCommandReader.readXMLStream(scan_stream);

        // Implement
        scan_impl = ScanCommandImplTool.implement(commands, jython);
    }

    /** {@inheritDoc} */
    @Override
    public long getWorkUnits()
    {
        long included_units = 0;
        for (ScanCommandImpl<?> command : scan_impl)
            included_units += command.getWorkUnits();
        return included_units;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getDeviceNames(final MacroContext macros) throws Exception
    {
        macros.pushMacros(command.getMacros());
        try
        {
            final Set<String> devices = new HashSet<String>();
            for (ScanCommandImpl<?> command : scan_impl)
            {
                for (String device_name : command.getDeviceNames(macros))
                    devices.add(device_name);
            }
            return devices.toArray(new String[devices.size()]);
        }
        finally
        {
            macros.popMacros();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void simulate(final SimulationContext context) throws Exception
    {
        context.getMacros().pushMacros(command.getMacros());
        try
        {
            context.logExecutionStep(context.getMacros().resolveMacros(command.toString()), 0.0);
            context.simulate(scan_impl);
        }
        finally
        {
            context.getMacros().popMacros();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ScanContext context) throws Exception
    {
        context.getMacros().pushMacros(command.getMacros());
        try
        {
            context.execute(scan_impl);
        }
        finally
        {
            context.getMacros().popMacros();
        }
    }
}
