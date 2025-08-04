/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.nio.ByteBuffer;

import org.epics.pva.common.PVAAuth;
import org.epics.pva.common.SecureSockets.TLSHandshakeInfo;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.PVATypeRegistry;

/** Determine authorization of a client connected to this server
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public abstract class ServerAuth
{
    /** @param channel Channel for which to check write access
     *  @return Does client have write access?
     */
    abstract public boolean hasWriteAccess(final String channel);

    // Must implement toString to describe auth
    @Override
    public abstract String toString();

    /** Decode authentication and then determine authorizations
     *  @param tcp TCP Handler
     *  @param buffer Buffer, positioned on String auth, optional detail
     *  @param tls_info {@link TLSHandshakeInfo}, may be <code>null</code>
     *  @return ClientAuthorization
     *  @throws Exception on error
     */
    public static ServerAuth decode(final ServerTCPHandler tcp, final ByteBuffer buffer, TLSHandshakeInfo tls_info) throws Exception
    {
        final String auth = PVAString.decodeString(buffer);

        if (buffer.remaining() < 1)
            throw new Exception("Missing authentication detail for '" + auth + "'");

        final PVATypeRegistry types = tcp.getClientTypes();
        final PVAData type = types.decodeType("", buffer);
        PVAStructure info = null;
        if (type instanceof PVAStructure)
        {
            info = (PVAStructure) type;
            info.decode(types, buffer);
        }

        if (PVAAuth.CA.equals(auth))
            return new CAServerAuth(info);

        if (info != null)
            throw new Exception("Expected no authentication detail for '" + auth + "' but got " + info);

        if (PVAAuth.X509.equals(auth))
            return new X509ServerAuth(tls_info);

        return Anonymous;
    }

    public static final ServerAuth Anonymous = new ServerAuth()
    {
        @Override
        public boolean hasWriteAccess(final String channel)
        {
            return false;
        }

        @Override
        public String toString()
        {
            return PVAAuth.ANONYMOUS;
        }
    };

    private static class CAServerAuth extends ServerAuth
    {
        private String user, host;

        public CAServerAuth(final PVAStructure info) throws Exception
        {
            PVAString element = info.get("user");
            if (element == null)
                throw new Exception("Missing 'ca' authentication 'user', got " + info);
            user = element.get();

            element = info.get("host");
            if (element == null)
                throw new Exception("Missing 'ca' authentication 'host', got " + info);
            host = element.get();
        }

        @Override
        public boolean hasWriteAccess(final String channel)
        {
            // TODO Implement access security based on `acf` type config file, checking channel for user and host
            return true;
        }

        @Override
        public String toString()
        {
            return "ca(" + user + "@" + host + ")";
        }
    }


    private static class X509ServerAuth extends ServerAuth
    {
        private String user, host;

        public X509ServerAuth(final TLSHandshakeInfo tls_info) throws Exception
        {
            if (tls_info == null)
                throw new Exception("x509 authentication requires principal name from TLS certificate");
            user = tls_info.name;
            host = tls_info.hostname;
        }

        @Override
        public boolean hasWriteAccess(final String channel)
        {
            // TODO Implement access security based on `acf` type config file, checking channel for user and host
            return true;
        }

        @Override
        public String toString()
        {
            return "x509(" + user + "@" + host + ")";
        }
    }
}
