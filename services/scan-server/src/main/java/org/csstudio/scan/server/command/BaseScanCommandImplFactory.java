/*******************************************************************************
 * Copyright (c) 2018-2019 Oak Ridge National Laboratory.
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

import static java.util.Map.entry;

import java.util.Map;

import org.csstudio.scan.command.CommentCommand;
import org.csstudio.scan.command.ConfigLogCommand;
import org.csstudio.scan.command.DelayCommand;
import org.csstudio.scan.command.IfCommand;
import org.csstudio.scan.command.IncludeCommand;
import org.csstudio.scan.command.LogCommand;
import org.csstudio.scan.command.LoopCommand;
import org.csstudio.scan.command.ParallelCommand;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScriptCommand;
import org.csstudio.scan.command.SequenceCommand;
import org.csstudio.scan.command.SetCommand;
import org.csstudio.scan.command.WaitCommand;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanCommandImplFactory;
import org.csstudio.scan.server.internal.JythonSupport;

/** Factory that implements base commands
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BaseScanCommandImplFactory implements ScanCommandImplFactory
{
    private final static Map<String, ScanCommandImplFactory> impls;

    static
    {
        impls = Map.ofEntries(
            entry("comment", (command, jython) -> new CommentCommandImpl((CommentCommand)command, jython)),
            entry("config_log", (command, jython) -> new ConfigLogCommandImpl((ConfigLogCommand)command, jython)),
            entry("delay", (command, jython) -> new DelayCommandImpl((DelayCommand)command, jython)),
            entry("if", (command, jython) -> new IfCommandImpl((IfCommand)command, jython)),
            entry("include", (command, jython) -> new IncludeCommandImpl((IncludeCommand)command, jython)),
            entry("log", (command, jython) -> new LogCommandImpl((LogCommand)command, jython)),
            entry("loop", (command, jython) -> new LoopCommandImpl((LoopCommand)command, jython)),
            entry("parallel", (command, jython) -> new ParallelCommandImpl((ParallelCommand)command, jython)),
            entry("script", (command, jython) -> new ScriptCommandImpl((ScriptCommand)command, jython)),
            entry("sequence", (command, jython) -> new SequenceCommandImpl((SequenceCommand)command, jython)),
            entry("set", (command, jython) -> new SetCommandImpl((SetCommand)command, jython)),
            entry("wait", (command, jython) -> new WaitCommandImpl((WaitCommand)command, jython))
        );
    }

    @Override
    public ScanCommandImpl<?> createImplementation(final ScanCommand command, final JythonSupport jython) throws Exception
    {
        final ScanCommandImplFactory factory = impls.get(command.getCommandID());
        return factory == null
               ? null
               : factory.createImplementation(command, jython);
    }

    @Override
    public String toString()
    {
        return "BaseScanCommandImplFactory";
    }
}
