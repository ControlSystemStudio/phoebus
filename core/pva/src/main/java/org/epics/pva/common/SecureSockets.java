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

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.epics.pva.PVASettings;

/** Helpers for creating secure sockets
 *
 *  By default, provide plain TCP sockets.
 *
 *  To enable TLS sockets, EPICS_PVAS_TLS_KEYCHAIN can be set to
 *  select a key- and truststore for the server, and EPICS_PVA_TLS_KEYCHAIN can define
 *  one  for the client.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SecureSockets
{
    /** Supported protocols. PVXS prefers 1.3 */
    private static final String[] PROTOCOLS = new String[] { "TLSv1.3"};

    private static boolean initialized = false;
    private static SSLServerSocketFactory tls_server_sockets;
    private static SSLSocketFactory tls_client_sockets;

    /** @param keychain_setting "/path/to/keychain;password"
     *  @return {@link SSLContext} with 'keystore' and 'truststore' set to content of keystore
     *  @throws Exception on error
     */
    private static SSLContext createContext(final String keychain_setting) throws Exception
    {
        final String path;
        final char[] pass;

        // We support the default "" empty as well as actual passwords, but not null for no password
        final int sep = keychain_setting.indexOf(';');
        if (sep > 0)
        {
            path = keychain_setting.substring(0, sep);
            pass = keychain_setting.substring(sep+1).toCharArray();
        }
        else
        {
            path = keychain_setting;
            pass = "".toCharArray();
        }

        logger.log(Level.CONFIG, () -> "Loading keychain '" + path + "'");

        final KeyStore key_store = KeyStore.getInstance("PKCS12");
        key_store.load(new FileInputStream(path), pass);

        final KeyManagerFactory key_manager = KeyManagerFactory.getInstance("PKIX");
        key_manager.init(key_store, pass);

        final TrustManagerFactory trust_manager = TrustManagerFactory.getInstance("PKIX");
        trust_manager.init(key_store);

        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(key_manager.getKeyManagers(), trust_manager.getTrustManagers(), null);

        return context;
    }

    private static synchronized void initialize() throws Exception
    {
        if (initialized)
            return;

        if (! PVASettings.EPICS_PVAS_TLS_KEYCHAIN.isBlank())
        {
            final SSLContext context = createContext(PVASettings.EPICS_PVAS_TLS_KEYCHAIN);
            tls_server_sockets = context.getServerSocketFactory();
        }

        if (! PVASettings.EPICS_PVA_TLS_KEYCHAIN.isBlank())
        {
            final SSLContext context = createContext(PVASettings.EPICS_PVA_TLS_KEYCHAIN);
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
            final SSLServerSocket ssl = (SSLServerSocket) socket;

            // Do we require client's certificate with 'principal' name for x509 authentication,
            // and initial handshake will otherwise fail?
            // Or do we support a client certificate, but it's not essential?
            if (PVASettings.require_client_cert)
            {
                ssl.setNeedClientAuth(true);
                logger.log(Level.FINE, "Server requires client certificate");
            }
            else
                ssl.setWantClientAuth(true);
            ssl.setEnabledProtocols(PROTOCOLS);
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
        int connection_timeout = Math.max(1, PVASettings.EPICS_PVA_TCP_SOCKET_TMO) * 1000; // Use EPICS_PVA_TCP_SOCKET_TMO for socket connection timeout, but at least 1 second

        if (!tls) {
            Socket socket = new Socket();
            socket.connect(address, connection_timeout);
            return socket;
        }

        if (tls_client_sockets == null)
            throw new Exception("TLS is not supported. Configure EPICS_PVA_TLS_KEYCHAIN");
        final SSLSocket socket = (SSLSocket) tls_client_sockets.createSocket();
        socket.connect(address, connection_timeout);
        socket.setEnabledProtocols(PROTOCOLS);
        // Handshake starts when first writing, but that might delay SSL errors, so force handshake before we use the socket
        socket.startHandshake();
        return socket;
    }

    /** Get name from local principal
     *
     *  @param socket {@link SSLSocket} that may have local principal
     *  @return Name (without "CN=..") if socket has certificate to authenticate or <code>null</code>
     */
    public static String getLocalPrincipalName(final SSLSocket socket)
    {
        try
        {
            final LdapName ldn = new LdapName(socket.getSession().getLocalPrincipal().getName());
            for (Rdn rdn : ldn.getRdns())
                if (rdn.getType().equals("CN"))
                    return (String) rdn.getValue();
        }
        catch (Exception ex)
        {   // May not have certificate with name
        }
        return null;
    }

    /** Information from TLS socket handshake */
    public static class TLSHandshakeInfo
    {
        /** Name by which the peer identified */
        public String name;

        /** Host of the peer */
        public String hostname;


        /** Get TLS/SSH info from socket
         *  @param socket {@link SSLSocket}
         *  @return {@link TLSHandshakeInfo} or <code>null</code>
         *  @throws Exception on error
         */
        public static TLSHandshakeInfo fromSocket(final SSLSocket socket) throws Exception
        {
            // Start ASAP instead of waiting for first read/write on socket.
            // "This method is synchronous for the initial handshake on a connection
            // and returns when the negotiated handshake is complete",
            // so no need to addHandshakeCompletedListener()

            // If server socket was configured to require client authentication,
            // there will be an SSLHandshakeException with message "Empty client certificate chain",
            // but no obvious way to catch that
            socket.startHandshake();

            try
            {
                // No way to check if there is peer info (certificates, principal, ...)
                // other then success vs. exception..
                String name = socket.getSession().getPeerPrincipal().getName();
                if (name.startsWith("CN="))
                    name = name.substring(3);
                else
                    logger.log(Level.WARNING, "Peer " + socket.getInetAddress() + " sent '" + name + "' as principal name, expected 'CN=...'");
                final TLSHandshakeInfo info = new TLSHandshakeInfo();
                info.name = name;

                info.hostname = socket.getInetAddress().getHostName();

                return info;
            }
            catch (Exception ex)
            {
                // Clients may not have a certificate..
                // System.out.println("No x509 name from client");
                // ex.printStackTrace();
            }
            return null;
        }
    }
}
