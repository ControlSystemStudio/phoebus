/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
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
package org.csstudio.scan.command;

import static org.csstudio.scan.ScanSystem.logger;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.csstudio.scan.spi.ScanCommandRegistry;
import org.w3c.dom.Node;

/** Factory for {@link ScanCommand}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanCommandFactory
{
    private static final Map<String, URL> images = new HashMap<>();
    private static final Map<String, Supplier<ScanCommand>> factories = new HashMap<>();

    static
    {
        for (ScanCommandRegistry reg : ServiceLoader.load(ScanCommandRegistry.class))
        {
            images.putAll(reg.getImages());
            factories.putAll(reg.getCommands());
        }
    }

    /** @return All known command IDs */
    public static Set<String> getCommandIDs()
    {
        return factories.keySet();
    }

    /** @param id Command ID
     *  @return {@link ScanCommand}
     *  @throws Exception on error
     */
    @SuppressWarnings("unchecked")
    public static <CMD extends ScanCommand> CMD createCommandForID(final String id) throws Exception
    {
        final Supplier<ScanCommand> supplier = factories.get(id);
        if (supplier == null)
            throw new Exception("Unknown scan command ID " + id);
        return (CMD) supplier.get();
    }

    /** @param id Command ID
     *  @return Image URL or <code>null</code>
     */
    public static URL getImage(final String id)
    {
        final URL url = images.get(id);
        if (url == null)
            logger.log(Level.WARNING, "No image for " + id);
        return url;
    }

    public List<ScanCommand> readCommands(Node firstChild)
    {
        // TODO XML reader
        return null;
    }
}
