/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;

import org.epics.pva.PVASettings;
import org.epics.pva.common.SecureSockets;
import org.epics.pva.common.SecureSockets.TLSHandshakeInfo;;

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

    /** Open TCP socket on which we listen for clients */
    private final ServerSocket tcp_server_socket;

    /** Secure TCP socket on which we listen for clients */
    private final ServerSocket tls_server_socket;

    private volatile boolean running = true;
    private volatile Thread listen_thread;

    public ServerTCPListener(final PVAServer server) throws Exception
    {
        this.server = server;

        // Is TLS configured?
        final boolean tls = !PVASettings.EPICS_PVAS_TLS_KEYCHAIN.isBlank();

        // Support open TCP, maybe also TLS
        tcp_server_socket = createSocket(PVASettings.EPICS_PVA_SERVER_PORT, false);
        InetSocketAddress local_address = (InetSocketAddress) tcp_server_socket.getLocalSocketAddress();
        logger.log(Level.CONFIG, "Listening on TCP " + local_address);
        String name = "TCP-listener " + local_address.getAddress() + ":" + local_address.getPort();

        if (tls)
        {
            tls_server_socket = createSocket(PVASettings.EPICS_PVAS_TLS_PORT, true);
            local_address = (InetSocketAddress) tls_server_socket.getLocalSocketAddress();
            logger.log(Level.CONFIG, "Listening on TLS " + local_address);
            name += ", TLS:" + local_address.getPort();
        }
        else
            tls_server_socket = null;

        // Start accepting connections
        listen_thread = new Thread(this::listen, name);
        listen_thread.setDaemon(true);
        listen_thread.start();
    }

    /** @param tls Request TLS or plain TCP address?
     *  @return Server's TCP address on which clients can connect
     */
    public InetSocketAddress getResponseAddress(final boolean tls)
    {
        if (tls  &&  tls_server_socket != null)
            return (InetSocketAddress) tls_server_socket.getLocalSocketAddress();

        return (InetSocketAddress) tcp_server_socket.getLocalSocketAddress();
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
    // Seems wasteful, but servers tend to run for a long time, so the initial overhead hardly matters.
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
                    // If it is indeed a PVA server, it will send validation request.
                    // But might also be other type of server, so best send nothing
                    // and close connection
                    check.shutdownInput();
                    check.shutdownOutput();
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
     *  @param port Preferred TCP port
     *  @param tls Use TLS?
     *  @return Socket bound to preferred port or unused port
     *  @throws Exception on error
     */
    private static ServerSocket createSocket(final int port, final boolean tls) throws Exception
    {
        if (checkForIPv4Server(port))
            logger.log(Level.FINE, "Found existing IPv4 server on port " + port);
        else
        {   // Try to bind to desired port
            try
            {
                return SecureSockets.createServerSocket(new InetSocketAddress(port), tls);
            }
            catch (BindException ex)
            {
                logger.log(Level.INFO, (tls ? "TLS" : "TCP") + " port " + port + " already in use, switching to automatically assigned port");
            }
        }

        // Fall back to automatically assigned port
        final InetSocketAddress any = new InetSocketAddress(0);
        try
        {
            return SecureSockets.createServerSocket(any, tls);
        }
        catch (Exception e)
        {
            throw new Exception("Cannot bind to automatically assigned port " + any, e);
        }
    }

    private void listen()
    {
        try
        {
            logger.log(Level.FINER, Thread.currentThread().getName() + " started");

            // Assume that open TCP, secure TLS, or both are configured.
            // Need to accept clients on any.
            // ServerSocketChannel allows using the Selector,
            // but there is no SSLServerSocketChannel.
            // SSLContext only creates (SSL)ServerSocket, and no easy way to
            // turn (SSL)ServerSocket into (SSL)ServerSocketChannel
            // https://stackoverflow.com/questions/37763038/is-there-any-way-to-use-sslcontext-with-serversocketchannel
            //
            // As a workaround we configure a (short) timeout on the sockets
            // and then take turns 'accept'ing from them
            if (tcp_server_socket != null)
                tcp_server_socket.setSoTimeout(10);
            if (tls_server_socket != null)
                tls_server_socket.setSoTimeout(10);
            while (running)
            {
                if (tcp_server_socket != null)
                {
                    try
                    {   // Check TCP
                        final Socket client = tcp_server_socket.accept();
                        logger.log(Level.FINE, () -> Thread.currentThread().getName() + " accepted TCP client " + client.getRemoteSocketAddress());
                        new ServerTCPHandler(server, client, null);
                    }
                    catch (SocketTimeoutException timeout)
                    {   // Ignore
                    }
                }
                if (tls_server_socket != null)
                {
                    try
                    {   // Check TLS
                        final Socket client = tls_server_socket.accept();
                        TLSHandshakeInfo tls_info = null;
                        if (client instanceof SSLSocket)
                        {
                            logger.log(Level.FINE, () -> Thread.currentThread().getName() + " accepted TLS client " + client.getRemoteSocketAddress());
                            try
                            {
                                tls_info = TLSHandshakeInfo.fromSocket((SSLSocket) client);
                            }
                            catch (SSLHandshakeException ssl)
                            {
                                logger.log(Level.FINE, "SSL Handshake error for " + client.getRemoteSocketAddress(), ssl);
                                continue;
                            }
                        }
                        else
                            logger.log(Level.WARNING, () -> Thread.currentThread().getName() + " expected TLS client " + client.getRemoteSocketAddress() + " but did not get SSLSocket");
                        new ServerTCPHandler(server, client, tls_info);
                    }
                    catch (SocketTimeoutException timeout)
                    {   // Ignore
                    }
                }
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
            if (tcp_server_socket != null)
                tcp_server_socket.close();
            if (tls_server_socket != null)
                tls_server_socket.close();

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