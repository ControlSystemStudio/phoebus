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
package org.csstudio.scan.spi;

import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;

import org.csstudio.scan.command.ScanCommand;

/** SPI for contributing scan commands
 *  @author Kay Kasemir
 */
public interface ScanCommandRegistry
{
    /** @return Map of command ID to image for the command */
    public Map<String, URL> getImages();

    /** @return Map of command ID to method that creates an instance */
    public Map<String, Supplier<ScanCommand>> getCommands();
}
