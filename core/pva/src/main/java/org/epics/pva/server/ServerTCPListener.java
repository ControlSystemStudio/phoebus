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
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.epics.pva.PVASettings;

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

        server_socket = createSocket();

        final InetSocketAddress local_address = (InetSocketAddress) server_socket.getLocalAddress();
        response_address = local_address.getAddress();
        response_port = local_address.getPort();
        logger.log(Level.CONFIG, "Listening on TCP " + local_address);

        // Start accepting connections
        listen_thread = new Thread(this::listen, "TCP-listener " + response_address + ":" + response_port);
        listen_thread.setDaemon(true);
        listen_thread.start();
    }

    // How to check if the desired TCP server port is already in use?
    //
    // For IPv4, bind(desired_port) would fail, causing us to move to an automatically assigned free port.
    // With IPv6, the socket defaults to a 'tcp46' type.
    // Binding it to the desired port will succeed even if `netstat` already shows an existing 'tcp4' socket
    // bound to that port.
    //
    // Could use a ServerSocketChannel.open(StandardProtocolFamily.INET) to test binding
    // to tcp4, but that is only available from JDK15 on, and the following plain open() or open(INET6)
    // will each again create a 'tcp46' type of socket that might miss an already bound tcp4 channel.
    //
    // Workaround: Try to connect to 127.0.0.1 at desired port.
    // Seems a wasteful, but servers tend to run for a long time, so the initial overhead hardly matters.
    // It's noted that we can still miss an existing tcp4 server simply because the connection
    // takes too long and we time out, or the tcp4 server starts up just after we checked.
    /** Attempt to check for a tcp4 server on port
     *  @param desired_port
     *  @return <code>true</code> if a tcp4 server was found
     */
    private static boolean checkForIPv4Server(final int desired_port)
    {
        try
        {
            final InetSocketAddress existing_server = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), desired_port);
            try (Socket check = new Socket())
            {
                check.setReuseAddress(true);
                try
                {
                    // Check for 1 second. Local connection is supposed to be much faster.
                    check.connect(existing_server, 1000);
                    // Managed to connect? Suggests existing IPv4 server on that port
                    return true;
                }
                catch (Exception ex)
                {
                    // Cannot connect, assume there is no existing IPv4 server
                }
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot check for existing IPv4 server on port", ex);
        }
        return false;
    }

    /** Create server's TCP socket
     *
     *  @return Socket bound to EPICS_PVA_SERVER_PORT or unused port
     *  @throws Exception on error
     */
    private static ServerSocketChannel createSocket() throws Exception
    {
        if (checkForIPv4Server(PVASettings.EPICS_PVA_SERVER_PORT))
            logger.log(Level.FINE, "Found existing IPv4 server on port " + PVASettings.EPICS_PVA_SERVER_PORT);
        else
        {   // Try to bind to desired port
            try
            {
                return createBoundSocket(new InetSocketAddress(PVASettings.EPICS_PVA_SERVER_PORT));
            }
            catch (BindException ex)
            {
                logger.log(Level.INFO, "TCP port " + PVASettings.EPICS_PVA_SERVER_PORT + " already in use, switching to automatically assigned port");
            }
        }

        // Fall back to automatically assigned port
        final InetSocketAddress any = new InetSocketAddress(0);
        try
        {
            return createBoundSocket(any);
        }
        catch (Exception e)
        {
            throw new Exception("Cannot bind to automatically assigned port " + any, e);
        }
    }

    /** Try to create socket that's bound to an address
     *  @param addr Desired address
     *  @return {@link ServerSocketChannel}
     *  @throws Exception on error
     */
    private static ServerSocketChannel createBoundSocket(final InetSocketAddress addr) throws Exception
    {
        ServerSocketChannel socket = ServerSocketChannel.open();
        try
        {
            socket.configureBlocking(true);
            socket.socket().setReuseAddress(true);
            socket.bind(addr);
        }
        catch (Exception ex)
        {
            socket.close();
            throw ex;
        }
        return socket;
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