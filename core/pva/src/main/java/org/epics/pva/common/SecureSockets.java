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
import java.security.KeyStore;
import java.util.logging.Level;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

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
    private static ServerSocketFactory server_sockets;
    private static SocketFactory client_sockets;

    private static synchronized void initialize() throws Exception
    {
        final String pass = System.getenv("EPICS_PVA_STOREPASS");
        final char[] password = pass != null ? pass.toCharArray() : null;

        final String keyfile = System.getenv("EPICS_PVAS_TLS_KEYCHAIN");
        if (keyfile != null  &&  keyfile.length() > 0)
        {
            logger.log(Level.INFO, "Loading keystore '" + keyfile + "'");
            final KeyStore key_store = KeyStore.getInstance("PKCS12");
            key_store.load(new FileInputStream(keyfile), password);

            final KeyManagerFactory key_manager = KeyManagerFactory.getInstance("PKIX");
            key_manager.init(key_store, password);

            final SSLContext context = SSLContext.getInstance("TLS");
            context.init(key_manager.getKeyManagers(), null, null);

            server_sockets = context.getServerSocketFactory();
        }
        else
            server_sockets = ServerSocketFactory.getDefault();

        final String trustfile = System.getenv("EPICS_PVA_TLS_KEYCHAIN");
        if (trustfile != null  &&  trustfile.length() > 0)
        {
            logger.log(Level.INFO, "Loading truststore '" + trustfile + "'");
            final KeyStore trust_store = KeyStore.getInstance("PKCS12");
            trust_store.load(new FileInputStream(trustfile), password);

            final TrustManagerFactory trust_manager = TrustManagerFactory.getInstance("PKIX");
            trust_manager.init(trust_store);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trust_manager.getTrustManagers(), null);

            client_sockets = context.getSocketFactory();
        }
        else
            client_sockets = SocketFactory.getDefault();
    }

    /** @return Factory for plain or secure server sockets
     *  @throws Exception on error
     */
    public static ServerSocketFactory getServerFactory() throws Exception
    {
        initialize();
        return server_sockets;
    }

    /** @return Factory for plain or secure client sockets
     *  @throws Exception on error
     */
    public static SocketFactory getClientFactory() throws Exception
    {
        initialize();
        return client_sockets;
    }
}
