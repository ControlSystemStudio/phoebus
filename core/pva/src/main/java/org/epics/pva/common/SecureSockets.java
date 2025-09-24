/*******************************************************************************
 * Copyright (c) 2023-2025 Oak Ridge National Laboratory.
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
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.Level;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
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

        if (logger.isLoggable(Level.FINE))
        {
            try
            {   // Key sections of example server certificate chain:
                //
                // Certificate[1]:
                // Owner: O=xxx.site.org, C=US, CN=ioc
                // Issuer: OU=EPICS Certificate Authority, O=ca.epics.org, C=US, CN=EPICS Root CA
                //
                // #1: ObjectId: 1.3.6.1.4.1.37427.1 Criticality=false
                // 0000: 43 45 52 54 3A 53 54 41   54 55 53 3A 64 30 62 62  CERT:STATUS:d0bb...
                //
                // Certificate[2]:
                // Owner: OU=EPICS Certificate Authority, O=ca.epics.org, C=US, CN=EPICS Root CA
                // Issuer: OU=EPICS Certificate Authority, O=ca.epics.org, C=US, CN=EPICS Root CA
                //
                // #1: ObjectId: 1.3.6.1.4.1.37427.1 Criticality=false
                // 0000: 43 45 52 54 3A 53 54 41   54 55 53 3A 64 30 62 62  CERT:STATUS:d0bb...
                final SSLSession session = socket.getSession();
                logger.log(Level.FINE, "Server name: '" + getPrincipalCN(session.getPeerPrincipal()) + "'");
                for (Certificate cert : session.getPeerCertificates())
                    if (cert instanceof X509Certificate x509)
                    {
                        logger.log(Level.FINE, "* " + x509.getSubjectX500Principal());
                        if (session.getPeerPrincipal().equals(x509.getSubjectX500Principal()))
                            logger.log(Level.FINE, "  - Server/IOC CN");
                        if (x509.getBasicConstraints() >= 0)
                            logger.log(Level.FINE, "  - Certificate Authority");
                        logger.log(Level.FINE, "  - Expires " + x509.getNotAfter());
                        if (x509.getSubjectX500Principal().equals(x509.getIssuerX500Principal()))
                            logger.log(Level.FINE, "  - Self-signed");

                        byte[] value = x509.getExtensionValue("1.3.6.1.4.1.37427.1");
                        logger.log(Level.FINE, "  - Status PV: '" + decodeDERString(value) + "'");
                    }
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error while logging result of handshake with server", ex);
            }
        }

        return socket;
    }

    /** Decode DER String
     *  @param der_value
     *  @return String, never null
     *  @throws Exception on error
     */
    public static String decodeDERString(final byte[] der_value) throws Exception
    {
        if (der_value == null)
            return "";
        // https://en.wikipedia.org/wiki/X.690#DER_encoding:
        // Type 4, length 0..127, characters
        if (der_value.length < 2)
            throw new Exception("Need DER type and size, only received " + der_value.length + " bytes");
        if (der_value[0] != 0x04)
            throw new Exception(String.format("Expected DER Octet String 0x04, got 0x%02X", der_value[0]));
        if (der_value[1] < 0)
            throw new Exception("Can only handle strings of length 0-127, got " + der_value[1]);
        if (der_value[1] != der_value.length-2)
            throw new Exception("DER string length " + der_value[1] + " but " + (der_value.length-2) + " data items");
        return new String(der_value, 2, der_value[1]);
    }

    /** Get CN from principal
     *
     *  @param principal {@link Principal} that may have a CN
     *  @return CN value (without "CN=..") or <code>null</code>
     */
    public static String getPrincipalCN(final Principal principal)
    {
        try
        {
            final LdapName ldn = new LdapName(principal.getName());
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

        /** PV for client certificate status */
        public String status_pv_name;

        @Override
        public String toString()
        {
            return "Name " + name + ", host " + hostname + ", cert status PV " + status_pv_name;
        }

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
                // Log certificate chain, grep cert status PV name
                String status_pv_name = "";
                final SSLSession session = socket.getSession();
                logger.log(Level.FINER, "Client name: '" + SecureSockets.getPrincipalCN(session.getPeerPrincipal()) + "'");
                for (Certificate cert : session.getPeerCertificates())
                    if (cert instanceof X509Certificate x509)
                    {
                        // Is this the cert for the client principal, or one of the authorities?
                        boolean is_principal_cert = false;

                        logger.log(Level.FINER, "* " + x509.getSubjectX500Principal());
                        if (session.getPeerPrincipal().equals(x509.getSubjectX500Principal()))
                        {
                            logger.log(Level.FINER, "  - Client CN");
                            is_principal_cert = true;
                        }
                        if (x509.getBasicConstraints() >= 0)
                            logger.log(Level.FINER, "  - Certificate Authority");
                        logger.log(Level.FINER, "  - Expires " + x509.getNotAfter());
                        if (x509.getSubjectX500Principal().equals(x509.getIssuerX500Principal()))
                            logger.log(Level.FINER, "  - Self-signed");

                        byte[] value = x509.getExtensionValue("1.3.6.1.4.1.37427.1");
                        String pv_name = SecureSockets.decodeDERString(value);
                        logger.log(Level.FINER, "  - Status PV: '" + pv_name + "'");

                        if (is_principal_cert  &&  pv_name != null  &&  !pv_name.isBlank())
                            status_pv_name = pv_name;
                    }


                // No way to check if there is peer info (certificates, principal, ...)
                // other then success vs. exception..
                final Principal principal = session.getPeerPrincipal();
                String name = getPrincipalCN(principal);
                if (name == null)
                {
                    logger.log(Level.WARNING, "Peer " + socket.getInetAddress() + " sent '" + principal + "' as principal name, expected 'CN=...'");
                    name = principal.getName();
                }

                final TLSHandshakeInfo info = new TLSHandshakeInfo();
                info.name = name;
                info.hostname = socket.getInetAddress().getHostName();
                info.status_pv_name = status_pv_name;

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
