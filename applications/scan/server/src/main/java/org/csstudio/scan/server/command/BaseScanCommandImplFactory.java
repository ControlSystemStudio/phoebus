/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
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
import org.csstudio.scan.command.DelayCommand;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.server.JythonSupport;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanCommandImplFactory;

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
            entry("delay", (command, jython) -> new DelayCommandImpl((DelayCommand)command, jython))
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
}
