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

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.server.internal.JythonSupport;

/** SPI for Factory that creates {@link ScanCommandImpl} for given {@link ScanCommand}
 *
 *  <p>SPI can be used to register additional factories.
 *  {@link ScanCommandImplTool} calls all factories until one returns an implementation.
 *  @author Kay Kasemir
 */
@FunctionalInterface
public interface ScanCommandImplFactory
{
    /** Create implementation for a command
     *  @param command ScanCommand to implement
     *  @param jython Jython interpreter, may be <code>null</code>
     *  @return {@link ScanCommandImpl} or <code>null</code> if this factory cannot implement given command
     *  @throws Exception on error
     */
    public ScanCommandImpl<?> createImplementation(ScanCommand command, JythonSupport jython) throws Exception;
}
