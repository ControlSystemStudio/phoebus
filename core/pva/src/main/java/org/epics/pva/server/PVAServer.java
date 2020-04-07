/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import static org.epics.pva.PVASettings.logger;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;

import org.epics.pva.data.PVAStructure;

/** PVA Server
 *
 *  <p>Server is identified by a unique ID and
 *  holds {@link ServerPV}s.
 *
 *  <p>Each {@link ServerPV} is created with initial
 *  value and can then be updated to a new value.
 *
 *  <p>Server listens to PV name searches via UDP,
 *  and establishes a {@link ServerTCPHandler}
 *  for each connected client.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAServer implements AutoCloseable
{
    public static ForkJoinPool POOL = ForkJoinPool.commonPool();

    private final Guid guid = new Guid();

    /** Served PVs by name */
    private final ConcurrentHashMap<String, ServerPV> pv_by_name = new ConcurrentHashMap<>();

    /** Served PVs by server ID */
    private final ConcurrentHashMap<Integer, ServerPV> pv_by_sid = new ConcurrentHashMap<>();

    /** UDP handler, listens to name searches */
    private final ServerUDPHandler udp;

    /** TCP connection listener, creates {@link ServerTCPHandler} for each connecting client */
    private final ServerTCPListener tcp;

    /** Handlers for the TCP connections clients established to this server */
    private final KeySetView<ServerTCPHandler, Boolean> tcp_handlers = ConcurrentHashMap.newKeySet();

    /** Create PVA Server
     *  @throws Exception on error
     */
    public PVAServer() throws Exception
    {
        logger.log(Level.CONFIG, "PVA Server " + guid);
        udp = new ServerUDPHandler(this::handleSearchRequest);
        tcp = new ServerTCPListener(this);
    }

    /** Create PVA Server with custom search handler
     *
     *  <p>Search requests will be passed to the search handler.
     *  If that search handler returns <code>true</code>,
     *  the request is considered handled.
     *  If the {@link SearchHandler} returns <code>false</code>,
     *  the default handler will then reply as usual,
     *  i.e. report served PVs.
     *
     *  @param search_handler Search handler
     *  @throws Exception on error
     */
    public PVAServer(final SearchHandler search_handler) throws Exception
    {
        logger.log(Level.CONFIG, "PVA Server " + guid);

        final SearchHandler combined_handler = (seq, cid, name, addr) ->
        {
            // Does custom handler consume the search request?
            if (search_handler.handleSearchRequest(seq, cid, name, addr))
                return true;
            // Fall back to default handler
            return handleSearchRequest(seq, cid, name, addr);
        };

        udp = new ServerUDPHandler(combined_handler);
        tcp = new ServerTCPListener(this);
    }


    /** Create a read-only PV which serves data to clients
     *
     *  <p>Creates a thread-safe copy of the initial value.
     *  To update the data, see {@link ServerPV#update(PVAStructure)}
     *
     *  @param name PV Name
     *  @param data Type definition and initial value
     *  @return {@link ServerPV}
     */
    public ServerPV createPV(final String name, final PVAStructure data)
    {
        return createPV(name, data, ServerPV.READONLY_WRITE_HANDLER);
    }

    /** Create a writable PV which serves data to clients
     *
     *  <p>Creates a thread-safe copy of the initial value.
     *  To update the data, see {@link ServerPV#update(PVAStructure)}
     *
     *  @param name PV Name
     *  @param data Type definition and initial value
     *  @param write_handler {@link WriteEventHandler}
     *  @return {@link ServerPV}
     */
    public ServerPV createPV(final String name, final PVAStructure data, final WriteEventHandler write_handler)
    {
        final ServerPV pv = new ServerPV(this, name, data, write_handler);
        pv_by_name.put(name, pv);
        pv_by_sid.put(pv.getSID(), pv);
        return pv;
    }

    /** Create a PV for an RPC service
     *
     *  @param name PV Name
     *  @param rpc {@link RPCService} that handles client invocations
     *  @return {@link ServerPV}
     */
    public ServerPV createPV(final String name, final RPCService rpc)
    {
        final ServerPV pv = new ServerPV(this, name, rpc);
        pv_by_name.put(name, pv);
        pv_by_sid.put(pv.getSID(), pv);
        return pv;
    }

    /** Get existing PV
     *  @param name PV name
     *  @return PV or <code>null</code> when unknown
     */
    ServerPV getPV(final String name)
    {
        return pv_by_name.get(name);
    }

    ServerPV getPV(final int sid)
    {
        return pv_by_sid.get(sid);
    }

    private boolean handleSearchRequest(final int seq, final int cid, final String name, final InetSocketAddress addr)
    {
        if (cid < 0)
        {   // 'List servers' search, no specific name
            POOL.execute(() -> udp.sendSearchReply(guid, 0, -1, tcp, addr));
            return true;
        }
        else
        {
            // Known channel?
            final ServerPV pv = getPV(name);
            if (pv != null)
            {
                // Reply with TCP connection info
                logger.log(Level.FINE, () -> "Received Search for known PV " + pv);
                POOL.execute(() -> udp.sendSearchReply(guid, seq, cid, tcp, addr));
                return true;
            }
            else
                logger.log(Level.FINE, () -> "Ignoring search for unknown PV '" + name + "'");
        }
        return false;
    }

    /** @param tcp_connection Newly created {@link ServerTCPHandler} */
    void register(final ServerTCPHandler tcp_connection)
    {
        tcp_handlers.add(tcp_connection);
    }

    /** @param tcp_connection {@link ServerTCPHandler} that experienced error or client closed it */
    void shutdownConnection(final ServerTCPHandler tcp_connection)
    {
        for (ServerPV pv : pv_by_name.values())
        {
            pv.removeClient(tcp_connection, -1);
            pv.unregisterSubscription(tcp_connection, -1);
        }

        // If this is still a known handler, close it, but don't wait
        if (tcp_handlers.remove(tcp_connection))
            tcp_connection.close(false);
    }

    /** @param pv PV to remove from server */
    void deletePV(final ServerPV pv)
    {
        pv_by_name.remove(pv.getName());
        pv_by_sid.remove(pv.getSID());
    }

    /** Close all connections */
    @Override
    public void close()
    {
        // Stop listening to searches
        udp.close();

        // Stop listening to connections
        tcp.close();

        // Close established connections
        for (ServerTCPHandler handler : tcp_handlers)
            handler.close(true);
        tcp_handlers.clear();
    }
}
