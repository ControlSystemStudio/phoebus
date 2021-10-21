/*******************************************************************************
 * Copyright (c) 2019-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import static org.epics.pva.PVASettings.logger;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.epics.pva.PVASettings;
import org.epics.pva.common.AddressInfo;
import org.epics.pva.common.Network;

/** Listen to TCP connections
 *
 *  <p>Creates {@link ServerTCPHandler} for
 *  each connecting client.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ServerTCPListener
{
    private final ExecutorService thread_pool = Executors.newCachedThreadPool(runnable ->
    {
        final Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });

    private final PVAServer server;

    /** TCP channel on which we listen for connections */
    private final ServerSocketChannel server_socket;

    /** Server's TCP address on which clients can connect */
    final InetAddress response_address;

    /** Server's TCP port on which clients can connect */
    final int response_port;

    private volatile boolean running = true;
    private volatile Thread listen_thread;

    public ServerTCPListener(final PVAServer server) throws Exception
    {
        this.server = server;

        // If any server address uses IPv6, create the TCP socket for IPv6,
        // which will be backwards-compatible with IPv4.
        // Otherwise support only IPv4
        ProtocolFamily type = StandardProtocolFamily.INET;
        for (AddressInfo info : Network.parseAddresses(PVASettings.EPICS_PVAS_INTF_ADDR_LIST))
            if (info.isIPv6())
            {
                type = StandardProtocolFamily.INET6;
                break;
            }
        server_socket = createSocket(type );

        final InetSocketAddress local_address = (InetSocketAddress) server_socket.getLocalAddress();
        response_address = local_address.getAddress();
        response_port = local_address.getPort();
        logger.log(Level.CONFIG, "Listening on TCP " + local_address);

        // Start accepting connections
        listen_thread = new Thread(this::listen, "TCP-listener " + response_address + ":" + response_port);
        listen_thread.setDaemon(true);
        listen_thread.start();
    }

    /** @param family INET for only IPv4, or INET6 to support both
     *  @return Socket bound to EPICS_PVA_SERVER_PORT or unused port
     */
    private static ServerSocketChannel createSocket(final ProtocolFamily family) throws Exception
    {
        ServerSocketChannel socket = ServerSocketChannel.open(family);
        socket.configureBlocking(true);
        socket.socket().setReuseAddress(true);
        try
        {
            socket.bind(new InetSocketAddress(PVASettings.EPICS_PVA_SERVER_PORT));
            return socket;
        }
        catch (BindException ex)
        {
            logger.log(Level.INFO, "TCP port " + PVASettings.EPICS_PVA_SERVER_PORT + " already in use, switching to automatically assigned port");
            final InetSocketAddress any = new InetSocketAddress(0);
            try
            {   // Must create new socket after bind() failed, cannot re-use
                socket = ServerSocketChannel.open(family);
                socket.configureBlocking(true);
                socket.socket().setReuseAddress(true);
                socket.bind(any);
                return socket;
            }
            catch (Exception e)
            {
                throw new Exception("Cannot bind to automatically assigned port " + any, e);
            }
        }
    }

    private void listen()
    {
        try
        {
            logger.log(Level.FINER, Thread.currentThread().getName() + " started");
            while (running)
            {
                final SocketChannel client = server_socket.accept();
                new ServerTCPHandler(server, client);
            }
        }
        catch (Exception ex)
        {
            if (running)
                logger.log(Level.WARNING, Thread.currentThread().getName() + " exits because of error", ex);
        }
        logger.log(Level.FINER, Thread.currentThread().getName() + " done.");
    }

    public void close()
    {
        running = false;
        // Close sockets, wait a little for threads to exit
        try
        {
            server_socket.close();

            if (listen_thread != null)
                listen_thread.join(5000);

            thread_pool.shutdownNow();
            thread_pool.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (Exception ex)
        {
            // Ignore
        }
    }
}