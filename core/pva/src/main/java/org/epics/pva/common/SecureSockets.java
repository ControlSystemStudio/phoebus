/*******************************************************************************
 * Copyright (c) 2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import static org.epics.pva.PVASettings.logger;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.logging.Level;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.epics.pva.PVASettings;

/** Helpers for creating secure sockets
 *
 *  By default, provide plain TCP sockets.
 *
 *  To enable TLS sockets, EPICS_PVAS_TLS_KEYCHAIN can be set to
 *  select a keystore for the server, and EPICS_PVA_TLS_KEYCHAIN can define
 *  a trust store for the client.
 *  The optional password for both is in EPICS_PVA_STOREPASS.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SecureSockets
{
    private static boolean initialized = false;
    private static ServerSocketFactory tls_server_sockets;
    private static SocketFactory tls_client_sockets;

    private static synchronized void initialize() throws Exception
    {
        if (initialized)
            return;

        final char[] password = PVASettings.EPICS_PVA_STOREPASS.isBlank() ? null : PVASettings.EPICS_PVA_STOREPASS.toCharArray();

        if (! PVASettings.EPICS_PVAS_TLS_KEYCHAIN.isBlank())
        {
            logger.log(Level.INFO, "Loading keystore '" + PVASettings.EPICS_PVAS_TLS_KEYCHAIN + "'");
            final KeyStore key_store = KeyStore.getInstance("PKCS12");
            key_store.load(new FileInputStream(PVASettings.EPICS_PVAS_TLS_KEYCHAIN), password);

            final KeyManagerFactory key_manager = KeyManagerFactory.getInstance("PKIX");
            key_manager.init(key_store, password);

            final SSLContext context = SSLContext.getInstance("TLS");
            context.init(key_manager.getKeyManagers(), null, null);

            tls_server_sockets = context.getServerSocketFactory();
        }

        if (! PVASettings.EPICS_PVA_TLS_KEYCHAIN.isBlank())
        {
            logger.log(Level.INFO, "Loading truststore '" + PVASettings.EPICS_PVA_TLS_KEYCHAIN + "'");
            final KeyStore trust_store = KeyStore.getInstance("PKCS12");
            trust_store.load(new FileInputStream(PVASettings.EPICS_PVA_TLS_KEYCHAIN), password);

            final TrustManagerFactory trust_manager = TrustManagerFactory.getInstance("PKIX");
            trust_manager.init(trust_store);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trust_manager.getTrustManagers(), null);

            tls_client_sockets = context.getSocketFactory();
        }
        initialized = true;
    }

    /** Create server socket
     *  @param address IP address and port to which the socket will be bound
     *  @param tls Use TLS socket? Otherwise plain TCP
     *  @return Plain or secure server socket
     *  @throws Exception on error
     */
    public static ServerSocket createServerSocket(final InetSocketAddress address, final boolean tls) throws Exception
    {
        initialize();
        final ServerSocket socket;
        if (tls)
        {
            if (tls_server_sockets == null)
                throw new Exception("TLS is not supported. Configure EPICS_PVAS_TLS_KEYCHAIN");
            socket = tls_server_sockets.createServerSocket();
        }
        else
            socket = new ServerSocket();

        try
        {
            socket.setReuseAddress(true);
            socket.bind(address);
        }
        catch (Exception ex)
        {
            socket.close();
            throw ex;
        }
        return socket;
    }

    /** Create client socket
     *  @param address IP address and port to which the socket will be bound
     *  @param tls Use TLS socket? Otherwise plain TCP
     *  @return Plain or secure client socket
     *  @throws Exception on error
     */
    public static Socket createClientSocket(final InetSocketAddress address, final boolean tls) throws Exception
    {
        initialize();
        if (! tls)
            return new Socket(address.getAddress(), address.getPort());

        if (tls_client_sockets == null)
            throw new Exception("TLS is not supported. Configure EPICS_PVA_TLS_KEYCHAIN");
        final SSLSocket socket = (SSLSocket) tls_client_sockets.createSocket(address.getAddress(), address.getPort());
        // PVXS prefers 1.3
        socket.setEnabledProtocols(new String[] { "TLSv1.3"});
        // Handshake starts when first writing, but that might delay SSL errors, so force handshake before we use the socket
        socket.startHandshake();
        return socket;
    }
}
