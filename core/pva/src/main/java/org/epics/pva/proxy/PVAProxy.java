/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.proxy;

import java.net.InetSocketAddress;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.epics.pva.PVASettings;
import org.epics.pva.client.ClientChannelState;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.server.PVAServer;
import org.epics.pva.server.ServerPV;

/** PVA proxy that forwards search requests and data
 *
 *  <p>Configuration:
 *
 *  Set environment variables or Java properties for the following:
 *  EPICS_PVA_ADDR_LIST - Where proxy searches for PVs
 *  EPICS_PVAS_BROADCAST_PORT, EPICS_PVA_SERVER_PORT - Where proxy makes those PVs available
 *
 *  For a 'local' test, set both
 *  EPICS_PVAS_BROADCAST_PORT and EPICS_PVA_SERVER_PORT to 5077.
 *  The proxy will then listen to search requests on this non-standard port,
 *  and locate PVs via the default port (5076).
 *
 *  Run  `softIocPVA -m N='' -d demo.db`
 *  Check direct access:
 *  EPICS_PVA_BROADCAST_PORT=5076 pvget ramp saw rnd
 *
 *  Then run bottleneck, and try
 *  EPICS_PVA_BROADCAST_PORT=5077 pvget -m ramp saw rnd
 *
 *  To stop,
 *  EPICS_PVA_BROADCAST_PORT=5077 pvget QUIT
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAProxy
{
    public static Logger logger = Logger.getLogger(PVAProxy.class.getPackageName());

    private PVAServer server;
    private PVAClient client;

    /** Latch for quitting */
    private final CountDownLatch quit = new CountDownLatch(1);

    /** Executor for background jobs */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private class ProxyChannel
    {
        private PVAChannel client_pv;
        private ServerPV server_pv;
        private AutoCloseable subscription  = null;

        ProxyChannel(final String name)
        {
            executor.submit(() -> connect(name));
        }

        private void connect(final String name)
        {
            logger.log(Level.INFO, "********* SEARCH FOR " + name);
            client_pv = client.getChannel(name, this::channelStateChanged);
        }

        private void channelStateChanged(final PVAChannel channel, final ClientChannelState state)
        {
            logger.log(Level.INFO, "********* STATE UPDATE FOR " + channel);
            if (channel.isConnected()  &&  subscription == null)
            {
                try
                {
                    subscription = channel.subscribe("", this::valueChanged);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot subscribe to " + channel, ex);
                }
            }
            // TODO Indicate if proxy is disconnected
        }

        private void valueChanged(final PVAChannel channel,
                                  final BitSet changes,
                                  final BitSet overruns,
                                  final PVAStructure data)
        {
            logger.log(Level.INFO, "********* " + channel.getName() + " = " + data);

            if (server_pv == null)
                server_pv = server.createPV(channel.getName(), data);
            else
                try
                {
                    server_pv.update(data);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot publish update to " + server_pv, ex);
                }
        }

        //  subscription.close();
        //  pv.close();
    }

    /** Map of PV name to proxy */
    private final ConcurrentHashMap<String, ProxyChannel> proxies = new ConcurrentHashMap<>();


    /** @param seq Client's search sequence
     *  @param cid Client channel ID or -1
     *  @param name Channel name or <code>null</code>
     *  @param addr Client's address and TCP port
     *  @return <code>true</code> if the search request was handled
     */
    private boolean handleSearchRequest(int seq, int cid, String name, InetSocketAddress addr)
    {
        System.out.println(addr + " searches for " + name + " (seq " + seq + ")");
        if (name.equals("QUIT"))
        {
            quit.countDown();
            return true;
        }

        proxies.computeIfAbsent(name, ProxyChannel::new);

        // Proceed with default search handler
        return false;
    }

    public void run() throws Exception
    {
        client = new PVAClient();
        server = new PVAServer(this::handleSearchRequest);
        try
        {
            quit.await();
        }
        finally
        {
            server.close();
            client.close();
        }
    }

    public static void main(String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));

        new PVAProxy().run();

        System.out.println("Done.");
    }
}
