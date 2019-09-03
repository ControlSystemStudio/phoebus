/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.opva;

import static org.phoebus.pv.PV.logger;

import java.util.Arrays;
import java.util.logging.Level;

import org.epics.pvaccess.ClientFactory;
import org.epics.pvaccess.PVAVersion;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelProviderRegistry;
import org.epics.pvaccess.client.ChannelProviderRegistryFactory;

/** Singleton context for pvAccess
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVA_Context
{
    private static PVA_Context instance;

    final private ChannelProvider provider;

    private PVA_Context() throws Exception
    {
        ClientFactory.start();
        final ChannelProviderRegistry registry = ChannelProviderRegistryFactory.getChannelProviderRegistry();
        provider = registry.getProvider("pva");
        if (provider == null)
            throw new Exception("Tried to locate 'pva' provider, found " + Arrays.toString(registry.getProviderNames()));
        logger.log(Level.CONFIG, "PVA Provider {0}: {1}.{2}.{3}",
                   new Object[] { provider.getProviderName(), PVAVersion.VERSION_MAJOR, PVAVersion.VERSION_MINOR, PVAVersion.VERSION_MAINTENANCE });
        // 0 - none, 1 - debug, 2 - more debug, 3 - dump messages
        logger.log(Level.CONFIG, getConfig("EPICS_PVA_DEBUG"));
        logger.log(Level.CONFIG, getConfig("EPICS_PVA_ADDR_LIST"));
        logger.log(Level.CONFIG, getConfig("EPICS_PVA_AUTO_ADDR_LIST"));
        logger.log(Level.CONFIG, getConfig("EPICS_PVA_CONN_TMO"));
        logger.log(Level.CONFIG, getConfig("EPICS_PVA_BROADCAST_PORT"));
    }

    private String getConfig(final String key)
    {
        // PVA ClientContextImpl uses system properties, falling back to environment variables
        String value = System.getProperty(key);
        if (value != null)
            return String.format("%-24s = '%s' (property)", key, value);
        value = System.getenv(key);
        if (value != null)
            return String.format("%-24s = '%s' (environment)", key, value);
        return String.format("%-24s - not set", key);
    }

    /** @return Singleton instance */
    public static synchronized PVA_Context getInstance() throws Exception
    {
        if (instance == null)
            instance = new PVA_Context();

        return instance;
    }

    /** @return {@link ChannelProvider} */
    public ChannelProvider getProvider()
    {
        return provider;
    }

    /** In tests, the context can be closed to check cleanup,
     *  but operationally the singleton will remain open.
     */
    public void close()
    {
        ClientFactory.stop();
    }
}
