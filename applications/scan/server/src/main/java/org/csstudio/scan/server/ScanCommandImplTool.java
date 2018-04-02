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
package org.csstudio.scan.server;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.server.internal.JythonSupport;

/** Tool that creates executable command implementations of a {@link ScanCommand}.
 *
 *  <p>Uses SPI to locate each {@link ScanCommandImplFactory},
 *  calling each until a {@link ScanCommandImpl} is returned.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanCommandImplTool
{
    private static final List<ScanCommandImplFactory> factories = new ArrayList<>();

    static
    {
        for (ScanCommandImplFactory factory : ServiceLoader.load(ScanCommandImplFactory.class))
        {
            logger.log(Level.CONFIG, "Found " + factory);
            factories.add(factory);
        }
    }

    /** Get implementation
     *  @param command Command description
     *  @param jython Jython interpreter, may be <code>null</code>
     *  @return Implementation
     *  @throws Exception if command lacks an implementation
     */
    @SuppressWarnings("unchecked")
    public static <C extends ScanCommand> ScanCommandImpl<C> implement(final C command, final JythonSupport jython) throws Exception
    {
        for (ScanCommandImplFactory factory : factories)
        {
            ScanCommandImpl<?> impl = factory.createImplementation(command, jython);
            if (impl != null)
                return (ScanCommandImpl<C>)impl;
        }
        throw new Exception("Unknown command " + command.getClass().getName());
    }

    /** Get implementations
     *  @param commands Command descriptions
     *  @param jython Jython interpreter, may be <code>null</code>
     *  @return Implementations
     *  @throws Exception if a command lacks an implementation
     */
    public static List<ScanCommandImpl<?>> implement(final List<ScanCommand> commands, final JythonSupport jython) throws Exception
    {
        final List<ScanCommandImpl<?>> impl = new ArrayList<>(commands.size());
        for (ScanCommand command : commands)
            impl.add(implement(command, jython));
        return impl;
    }
}
