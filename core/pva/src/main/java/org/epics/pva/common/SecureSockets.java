/*******************************************************************************
 * Copyright (c) 2023-2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import static org.epics.pva.PVASettings.logger;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
public class SecureSockets
{
    /** Supported protocols. PVXS prefers 1.3 */
    private static final String[] PROTOCOLS = new String[] { "TLSv1.3"};

    /** Initialize only once */
    private static boolean initialized = false;

    /** Factory for secure server sockets */
    private static SSLServerSocketFactory tls_server_sockets;

    /** Factory for secure client sockets */
    private static SSLSocketFactory tls_client_sockets;

    /** X509 certificates loaded from the keychain mapped by principal name of the certificate */
    public static Map<String, X509Certificate> keychain_x509_certificates = new ConcurrentHashMap<>();

    /** Own certificate info extracted from the client keychain during context creation, or null */
    private static volatile OwnCertInfo client_own_cert_info;

    /** Own certificate info extracted from the server keychain during context creation, or null */
    private static volatile OwnCertInfo server_own_cert_info;

    /** Info about the local (own) certificate's status PV extension and live subscription */
    public static class OwnCertInfo
    {
        public final X509Certificate certificate;
        public final String status_pv_name;

        /** Live cert status subscription, set after subscribing in initialize(). Null before subscription. */
        public volatile CertificateStatus cert_status;

        OwnCertInfo(final X509Certificate certificate, final String status_pv_name)
        {
            this.certificate = certificate;
            this.status_pv_name = status_pv_name;
        }
    }

    /** @return Own-cert info for client TLS context, or null if no status PV extension */
    public static OwnCertInfo getClientOwnCertInfo()
    {
        return client_own_cert_info;
    }

    /** @return Own-cert info for server TLS context, or null if no status PV extension */
    public static OwnCertInfo getServerOwnCertInfo()
    {
        return server_own_cert_info;
    }

    /** Result of creating an SSL context, including own-cert status PV info */
    private static class ContextInfo
    {
        final SSLContext context;
        final OwnCertInfo own_cert_info;

        ContextInfo(final SSLContext context, final OwnCertInfo own_cert_info)
        {
            this.context = context;
            this.own_cert_info = own_cert_info;
        }
    }

    /** @param keychain_setting "/path/to/keychain", "/path/to/keychain;password",
     *         or just "/path/to/keychain" with password in a separate *_PWD_FILE
     *  @param is_server true for server keychain (uses EPICS_PVAS_TLS_KEYCHAIN_PWD_FILE),
     *                   false for client (uses EPICS_PVA_TLS_KEYCHAIN_PWD_FILE)
     *  @return {@link ContextInfo} with SSLContext and optional own-cert status PV info
     *  @throws Exception on error
     */
    private static ContextInfo createContext(final String keychain_setting, final boolean is_server) throws Exception
    {
        final String path;
        final char[] pass;

        final int sep = keychain_setting.indexOf(';');
        if (sep > 0)
        {
            path = keychain_setting.substring(0, sep);
            pass = keychain_setting.substring(sep+1).toCharArray();
        }
        else
        {
            path = keychain_setting;
            pass = readKeychainPassword(is_server);
        }

        logger.log(Level.FINE, () -> "Loading keychain '" + path + "'");

        final KeyStore key_store = KeyStore.getInstance("PKCS12");
        key_store.load(new FileInputStream(path), pass);

        // Track each loaded certificate by its principal name,
        // and extract own-cert status PV extension from key entries
        OwnCertInfo own_cert_info = null;

        for (String alias : Collections.list(key_store.aliases()))
        {
            if (key_store.isCertificateEntry(alias))
            {
                final Certificate cert = key_store.getCertificate(alias);
                if (cert instanceof X509Certificate x509)
                {
                    final String principal = x509.getSubjectX500Principal().toString();
                    logger.log(Level.FINE, "Keychain alias '" + alias + "' is X509 certificate for " + principal);
                    keychain_x509_certificates.put(principal, x509);
                    // Could print 'cert', but jdk.event.security logger already does that at FINE level
                }
            }
            if (key_store.isKeyEntry(alias))
            {
                // final Key key = key_store.getKey(alias, pass);
                final Certificate cert = key_store.getCertificate(alias);
                if (cert instanceof X509Certificate x509)
                {
                    final String principal = x509.getSubjectX500Principal().toString();
                    logger.log(Level.FINE, "Keychain alias '" + alias + "' is X509 key and certificate for " + principal);
                    keychain_x509_certificates.put(principal, x509);

                    // Extract certificate-status-PV extension (OID 1.3.6.1.4.1.37427.1) from own cert
                    if (own_cert_info == null)
                    {
                        try
                        {
                            final byte[] ext_value = x509.getExtensionValue("1.3.6.1.4.1.37427.1");
                            final String status_pv = decodeDERString(ext_value);
                            if (! status_pv.isEmpty())
                            {
                                own_cert_info = new OwnCertInfo(x509, status_pv);
                                logger.log(Level.FINE, "Own certificate status PV: '" + status_pv + "'");
                            }
                        }
                        catch (Exception ex)
                        {
                            logger.log(Level.WARNING, "Error extracting status PV from own certificate", ex);
                        }
                    }

                    // Add CA certs from the key entry's chain as trusted entries.
                    // Java's TrustManagerFactory only trusts trustedCertEntry aliases,
                    // not the CA chain attached to a keyEntry.
                    // PVXS does the equivalent in extractCAs() (openssl.cpp).
                    final Certificate[] chain = key_store.getCertificateChain(alias);
                    if (chain != null)
                    {
                        for (int i = 1; i < chain.length; i++)
                        {
                            if (chain[i] instanceof X509Certificate ca_cert)
                            {
                                final String ca_alias = "ca-chain-" + alias + "-" + i;
                                if (! key_store.containsAlias(ca_alias))
                                {
                                    key_store.setCertificateEntry(ca_alias, ca_cert);
                                    final String ca_name = ca_cert.getSubjectX500Principal().toString();
                                    logger.log(Level.FINE, "Added CA from chain as trusted: " + ca_name);
                                    keychain_x509_certificates.put(ca_name, ca_cert);
                                }
                            }
                        }
                    }
                }
                // Could print 'key', but jdk.event.security logger already logs the cert at FINE level
                // and logging the key would show the private key
            }
        }

        final KeyManagerFactory key_manager = KeyManagerFactory.getInstance("PKIX");
        key_manager.init(key_store, pass);

        final TrustManagerFactory trust_manager = TrustManagerFactory.getInstance("PKIX");
        trust_manager.init(key_store);

        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(key_manager.getKeyManagers(), trust_manager.getTrustManagers(), null);

        return new ContextInfo(context, own_cert_info);
    }

    private static char[] readKeychainPassword(final boolean is_server)
    {
        final String env_name = is_server ? "EPICS_PVAS_TLS_KEYCHAIN_PWD_FILE"
                                          : "EPICS_PVA_TLS_KEYCHAIN_PWD_FILE";
        final String pwd_file = PVASettings.get(env_name, "");
        if (! pwd_file.isEmpty())
        {
            try
            {
                final String password = Files.readString(Path.of(pwd_file)).trim();
                logger.log(Level.FINE, () -> "Read keychain password from " + pwd_file);
                return password.toCharArray();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error reading password file " + pwd_file, ex);
            }
        }
        // Java PKCS12: null skips encrypted sections (loses CA certs).
        // Empty array attempts decryption with retry via NUL char fallback.
        return new char[0];
    }

    private static synchronized void initialize() throws Exception
    {
        if (initialized)
            return;

        if (! PVASettings.EPICS_PVAS_TLS_KEYCHAIN.isBlank())
        {
            final ContextInfo info = createContext(PVASettings.EPICS_PVAS_TLS_KEYCHAIN, true);
            tls_server_sockets = info.context.getServerSocketFactory();
            server_own_cert_info = info.own_cert_info;
            subscribeOwnCertStatus(server_own_cert_info, "Server");
        }

        if (! PVASettings.EPICS_PVA_TLS_KEYCHAIN.isBlank())
        {
            final ContextInfo info = createContext(PVASettings.EPICS_PVA_TLS_KEYCHAIN, false);
            tls_client_sockets = info.context.getSocketFactory();
            client_own_cert_info = info.own_cert_info;
            subscribeOwnCertStatus(client_own_cert_info, "Client");
        }
        initialized = true;
    }

    /** Subscribe to own cert status PV immediately at keychain-read time.
     *  @param own_info OwnCertInfo extracted from keychain, or null
     *  @param label "Client" or "Server" for logging
     */
    private static void subscribeOwnCertStatus(final OwnCertInfo own_info, final String label)
    {
        if (own_info == null)
            return;
        logger.log(Level.FINE, () -> label + " subscribing to own cert status PV: " + own_info.status_pv_name);
        own_info.cert_status = CertificateStatusMonitor.instance().checkCertStatus(
                own_info.certificate, own_info.status_pv_name, update ->
                {
                    if (update.isValid())
                        logger.log(Level.FINE, () -> label + " own cert status VALID");
                    else if (update.isUnrecoverable())
                        logger.log(Level.SEVERE, () -> label + " own cert status " + (update.isRevoked() ? "REVOKED" : "EXPIRED"));
                    else
                        logger.log(Level.WARNING, () -> label + " own cert status UNKNOWN");
                });
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
        // X509Certificate.getExtensionValue() returns a DER OCTET STRING
        // that wraps the actual extension content.
        // The extension content itself is a DER-encoded string
        // (OCTET STRING 0x04 or UTF8String 0x0C), so we must unwrap two layers:
        //   Outer: 0x04 <len> <inner DER>
        //   Inner: 0x04|0x0C <len> <actual string bytes>
        // https://en.wikipedia.org/wiki/X.690#DER_encoding
        if (der_value.length < 2)
            throw new Exception("Need DER type and size, only received " + der_value.length + " bytes");
        if (der_value[0] != 0x04)
            throw new Exception(String.format("Expected DER Octet String 0x04, got 0x%02X", der_value[0]));
        if (der_value[1] < 0)
            throw new Exception("Can only handle strings of length 0-127, got " + der_value[1]);
        if (der_value[1] != der_value.length-2)
            throw new Exception("DER string length " + der_value[1] + " but " + (der_value.length-2) + " data items");

        // Unwrap outer OCTET STRING to get the inner DER-encoded string
        final int inner_offset = 2;
        final int inner_len = der_value.length - 2;
        if (inner_len < 2)
            throw new Exception("Inner DER too short: " + inner_len + " bytes");
        final byte inner_tag = der_value[inner_offset];
        // Accept OCTET STRING (0x04), UTF8String (0x0C), or IA5String (0x16) as inner type
        if (inner_tag != 0x04  &&  inner_tag != 0x0C  &&  inner_tag != 0x16)
            throw new Exception(String.format("Expected inner DER string type 0x04, 0x0C, or 0x16, got 0x%02X", inner_tag));
        final int str_len = der_value[inner_offset + 1] & 0xFF;
        if (str_len != inner_len - 2)
            throw new Exception("Inner DER string length " + str_len + " but " + (inner_len-2) + " data bytes");
        return new String(der_value, inner_offset + 2, str_len);
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
        /** Certificate of the peer */
        public final X509Certificate peer_cert;

        /** Name by which the peer identified */
        public final String name;

        /** Host of the peer */
        public final InetAddress host;

        /** PV for client certificate status */
        public final String status_pv_name;

        TLSHandshakeInfo(final X509Certificate peer_cert, final String name, final InetAddress host, final String status_pv_name)
        {
            this.peer_cert = peer_cert;
            this.name = name;
            this.host = host;
            this.status_pv_name = status_pv_name;
        }

        @Override
        public String toString()
        {
            return "Name " + name + ", host " + host + ", cert status PV " + status_pv_name;
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

            return extractPeerInfo(socket);
        }

        /** Extract peer certificate info from an already-handshaken SSL socket.
         *
         *  <p>Unlike {@link #fromSocket}, this does not call startHandshake()
         *  and is safe to use when the handshake was already performed.
         *
         *  @param socket {@link SSLSocket} that has completed handshake
         *  @return {@link TLSHandshakeInfo} or <code>null</code>
         */
        public static TLSHandshakeInfo fromHandshakenSocket(final SSLSocket socket)
        {
            return extractPeerInfo(socket);
        }

        private static TLSHandshakeInfo extractPeerInfo(final SSLSocket socket)
        {
            try
            {
                // Log certificate chain, grep cert status PV name
                X509Certificate peer_cert = null;
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
                        {
                            peer_cert = x509;
                            status_pv_name = pv_name;
                        }
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

                final TLSHandshakeInfo info = new TLSHandshakeInfo(peer_cert, name, socket.getInetAddress(), status_pv_name);

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
