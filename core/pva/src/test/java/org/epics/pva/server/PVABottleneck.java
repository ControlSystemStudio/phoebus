/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.net.InetSocketAddress;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;

import org.epics.pva.PVASettings;
import org.epics.pva.client.MonitorListener;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVAStructure;

/** PVA Client/Server Demo that logs all received search requests
 *
 *  <p>Demo:
 *  Run  `softIocPVA -m N='' -d demo.db")`
 *  Check direct access:
 *  EPICS_PVA_BROADCAST_PORT=5076 pvget ramp
 *
 *  Then run bottleneck, and try
 *  EPICS_PVA_BROADCAST_PORT=5077 pvget ramp
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVABottleneck
{
    private PVAServer server;
    private PVAClient client;

    /** Latch for quitting */
    private final CountDownLatch quit = new CountDownLatch(1);

    /** Executor for background jobs */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final ConcurrentHashMap<String, Boolean> handled_pvs = new ConcurrentHashMap<>();

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

        if (handled_pvs.putIfAbsent(name, Boolean.TRUE) != Boolean.TRUE)
        {
            executor.submit(() -> handlePV(name));
        }

        // Proceed with default search handler
        return false;
    }

    private void handlePV(final String name)
    {
        // TODO Stage 1) Connect 2) Create server PV 3) Update server PV
        System.out.println("********* SEARCH FOR " + name);
        final PVAChannel pv = client.getChannel(name);

        final CountDownLatch updates = new CountDownLatch(1);

        final MonitorListener listener = (final PVAChannel channel,
                final BitSet changes,
                final BitSet overruns,
                final PVAStructure data) ->
        {
            System.out.println("********* " + channel.getName() + " = " + data);

            server.createPV(name, data);

            updates.countDown();
        };

        try
        {
            pv.connect().get();

            final AutoCloseable subscription = pv.subscribe("", listener);

            updates.await();

            subscription.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        pv.close();
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
        // TODO Remove
        System.setProperty("EPICS_PVAS_BROADCAST_PORT", "5077");
        System.setProperty("EPICS_PVA_SERVER_PORT", "5077");
        System.setProperty("EPICS_PVA_ADDR_LIST", "128.219.44.89");

        LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/pva_logging.properties"));

        new PVABottleneck().run();

        System.out.println("Done.");
    }
}
