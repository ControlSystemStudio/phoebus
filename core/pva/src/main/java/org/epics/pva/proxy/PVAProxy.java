/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.proxy;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.epics.pva.PVASettings;
import org.epics.pva.client.ClientChannelState;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.nt.PVATimeStamp;
import org.epics.pva.server.PVAServer;
import org.epics.pva.server.ServerPV;

/** PVA proxy that forwards search requests and data
 *
 *  <p>Environment variables or Java properties:
 *
 *  <ul>
 *  <li>EPICS_PVA_ADDR_LIST, EPICS_PVA_BROADCAST_PORT - Where proxy searches for PVs
 *  <li>EPICS_PVAS_BROADCAST_PORT, EPICS_PVA_SERVER_PORT - Where proxy makes those PVs available
 *  <li>PREFIX - Prefix for internal PVs
 *  </ul>
 *
 *  <p>For a 'local' test and to debug, assert that IOC, proxy and client each use unique ports.
 *
 *  <p>Run  `softIocPVA -m N='' -d demo.db`
 *
 *  It will default to UDP 5076
 *  Check direct access:
 *  EPICS_PVA_BROADCAST_PORT=5076 pvget ramp saw rnd
 *
 *  <p>Then run PVAProxy with
 *  EPICS_PVA_BROADCAST_PORT=5076
 *  EPICS_PVAS_BROADCAST_PORT=5077
 *  EPICS_PVA_SERVER_PORT=5077
 *  so it connects to the soft IOC and serves on UDP 5077.
 *
 *  <p>For a client to use the proxy, use
 *  EPICS_PVA_BROADCAST_PORT=5078
 *  EPICS_PVA_ADDR_LIST=<correct IP>:5077
 *  pvget monitor ramp saw rnd
 *
 *  The ADDR_LIST tells the client to read via the proxy.
 *  The BROADCAST_PORT makes the client send search requests from 5078,
 *  so that it can receive replies.
 *
 *  <p>The following internal PVs are supported:
 *
 *  <ul>
 *  <li>$(PREFIX)count - Number of proxied PVs
 *  <li>$(PREFIX)QUIT - Reading this will stop the proxy
 *  </ul>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAProxy
{
    public static Logger logger = Logger.getLogger(PVAProxy.class.getPackage().getName());

    // Developed to test if the PVA server and client API
    // provides what is necessary to implement a basic 'gateway'.
    // Compared to a full featured 'gateway', however,
    // this may be more of a 'bottleneck'.

    private String prefix = "";

    private PVAServer server;
    private PVAClient client;

    /** Latch for quitting */
    private final CountDownLatch quit = new CountDownLatch(1);

    /** Executor for background jobs */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** Timer for checking timeouts etc. */
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    /** Timestamp of 'count' PV */
    private PVATimeStamp count_time = new PVATimeStamp();
    /** Value of 'count' PV */
    private PVAStructure count_value = new PVAStructure("count", "",
                                                        new PVAInt("value", 0),
                                                        count_time);
    /** 'count' PV */
    private ServerPV count_channel;

    /** Handler for one proxied PV */
    private class ProxyChannel implements AutoCloseable
    {
        // client_pv etc. also have a name reference, but they're null in some lifecycle states
        private final String name;
        private volatile PVAChannel client_pv;
        private volatile ServerPV server_pv;
        private volatile AutoCloseable subscription  = null;

        ProxyChannel(final String name)
        {
            logger.log(Level.INFO, () -> "++++ New Server Proxy " + name);
            this.name = name;
            executor.submit(() -> connect(name));
        }

        private void connect(final String name)
        {
            logger.log(Level.INFO, () -> "Search for " + name);
            client_pv = client.getChannel(name, this::channelStateChanged);
            timer.schedule(this::connectionTimeout, 5000, TimeUnit.MILLISECONDS);

            updateChannelCount();
        }

        private void connectionTimeout()
        {
            if (subscription != null)
            {
                // Channel connected and then subscribed OK.
                // XXXX We could cancel the connection check when subscribing...
                logger.log(Level.INFO, () -> "Successful connection check for " + name);
                return;
            }

            logger.log(Level.INFO, () -> "Connection timeout for " + name);
            try
            {
                close();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failed to cancel search for " + name, ex);
            }
        }

        private void channelStateChanged(final PVAChannel channel, final ClientChannelState state)
        {
            logger.log(Level.INFO, () -> "State update for " + channel);
            if (channel.isConnected()  &&  subscription == null)
            {
                try
                {
                    subscription = channel.subscribe("", this::valueChanged);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot subscribe to " + name, ex);
                }
            }
            else if (state == ClientChannelState.INIT)
            {
                // If proxy is disconnected, close the proxy so our clients also see a disconnect
                logger.log(Level.INFO, () -> "PV " + name + " disconnected, closing proxy");
                close();
            }
        }

        private void valueChanged(final PVAChannel channel,
                                  final BitSet changes,
                                  final BitSet overruns,
                                  final PVAStructure data)
        {
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "Value update for " + name + " = " + data);
            else
                logger.log(Level.FINE, () -> "Value update for " + name);

            if (server_pv == null)
            {
                server_pv = server.createPV(channel.getName(), data);
                logger.log(Level.INFO, () -> "Now serving " + server_pv);
            }
            else
            {
                // TODO Periodically check how many clients the ServerPV has, close if unused for a while
                if (! server_pv.isSubscribed())
                    System.out.println("****** UNUSED Proxy " + channel.getName());

                try
                {
                    server_pv.update(data);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot publish update to " + server_pv, ex);
                }
            }
        }

        @Override
        public void close()
        {
            if (subscription != null)
            {
                try
                {
                    subscription.close();
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot close subscription of " + name, ex);
                }
                subscription = null;
            }
            if (client_pv != null)
            {
                client_pv.close();
                client_pv = null;
            }
            if (server_pv != null)
            {
                server_pv.close();
                server_pv = null;
            }

            // Remove proxy. A new client search for this name will re-create a proxy
            proxies.remove(name, this);
            updateChannelCount();
            logger.log(Level.INFO, () -> "++++ Closed Server Proxy " + name);
        }
    }

    /** Map of PV name to proxy */
    private final ConcurrentHashMap<String, ProxyChannel> proxies = new ConcurrentHashMap<>();

    public PVAProxy()
    {
        prefix = PVASettings.get("PREFIX", prefix);
    }

    /** @param seq Client's search sequence
     *  @param cid Client channel ID or -1
     *  @param name Channel name or <code>null</code>
     *  @param addr Client's address and TCP port
     *  @return <code>true</code> if the search request was handled
     */
    private boolean handleSearchRequest(final int seq, final int cid, final String name, final InetSocketAddress addr)
    {
        logger.log(Level.INFO, () -> addr + " searches for " + name + " (CID " + cid + ", seq " + seq + ")");
        if (name.equals(prefix+"QUIT"))
        {
            quit.countDown();
            return true;
        }

        // Unless it's an internal PV, setup proxy
        if (!count_channel.getName().equals(name))
            proxies.computeIfAbsent(name, ProxyChannel::new);

        // Proceed with default search handler
        return false;
    }

    private void updateChannelCount()
    {
        try
        {
            count_value.get("value").setValue(proxies.size());
            count_time.set(Instant.now());
            count_channel.update(count_value);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot update channel count", ex);
        }
    }

    public void run() throws Exception
    {
        System.out.println("PVA Proxy");
        System.out.println("");

        System.out.println("Client config:");
        System.out.println("EPICS_PVA_ADDR_LIST=" + PVASettings.EPICS_PVA_ADDR_LIST);
        System.out.println("EPICS_PVA_AUTO_ADDR_LIST=" + PVASettings.EPICS_PVA_AUTO_ADDR_LIST);
        System.out.println("EPICS_PVA_BROADCAST_PORT=" + PVASettings.EPICS_PVA_BROADCAST_PORT);
        client = new PVAClient();

        System.out.println("");
        System.out.println("Server config:");
        System.out.println("EPICS_PVAS_BROADCAST_PORT=" + PVASettings.EPICS_PVAS_BROADCAST_PORT);
        System.out.println("EPICS_PVA_SERVER_PORT=" + PVASettings.EPICS_PVA_SERVER_PORT);
        server = new PVAServer(this::handleSearchRequest);

        System.out.println("");
        System.out.println("Info PVs:");
        count_channel = server.createPV(prefix + "count", count_value);
        System.out.println(count_channel.getName());

        try
        {
            quit.await();
        }
        finally
        {
            timer.shutdownNow();
            executor.shutdownNow();
            server.close();
            client.close();
        }
    }

    public static void main(String[] args) throws Exception
    {
        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));

        new PVAProxy().run();
    }
}
