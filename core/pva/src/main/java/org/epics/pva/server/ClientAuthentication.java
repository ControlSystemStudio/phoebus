/*******************************************************************************
 * Copyright (c) 2025-2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import org.epics.pva.common.PVAAuth;
import org.epics.pva.common.SecureSockets.TLSHandshakeInfo;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.PVATypeRegistry;

/** Describe authentication of a client connected to this server
 *  @author Kay Kasemir
 */
public class ClientAuthentication
{
    final static ClientAuthentication Anonymous = new ClientAuthentication(PVAAuth.anonymous, "nobody", InetAddress.getLoopbackAddress());

    private final PVAAuth type;
    private final String  user;
    private final InetAddress host;

    ClientAuthentication(final PVAAuth type, final String user, final InetAddress host)
    {
        this.type = type;
        this.user = user;
        this.host = host;
    }

    /** @return Type of authentication */
    public PVAAuth getType()
    {
        return type;
    }

    /** @return User name */
    public String getUser()
    {
        return user;
    }

    /** @return Client's host */
    public InetAddress getHost()
    {
        return host;
    }

    @Override
    public String toString()
    {
        return type + "(" + user + "@" + host + ")";
    }

    /** Decode authentication
     *  @param tcp TCP Handler
     *  @param buffer Buffer, positioned on String auth, followed by optional detail
     *  @param tls_info {@link TLSHandshakeInfo}, may be <code>null</code>
     *  @return {@link ClientAuthentication}
     *  @throws Exception on error
     */
    static ClientAuthentication decode(final ServerTCPHandler tcp, final ByteBuffer buffer, TLSHandshakeInfo tls_info) throws Exception
    {
        final String auth = PVAString.decodeString(buffer);

        if (buffer.remaining() < 1)
            throw new Exception("Missing authentication detail for '" + auth + "'");

        final PVATypeRegistry types = tcp.getClientTypes();
        final PVAData type = types.decodeType("", buffer);
        if (type instanceof PVAStructure info)
        {
            info.decode(types, buffer);

            // CA authentication gets details from info structure
            if (PVAAuth.ca.name().equals(auth))
            {
                PVAString element = info.get("user");
                if (element == null)
                    throw new Exception("Missing 'ca' authentication 'user', got " + info);
                final String user = element.get();

                element = info.get("host");
                if (element == null)
                    throw new Exception("Missing 'ca' authentication 'host', got " + info);
                final String host = element.get();
                final InetAddress addr = InetAddress.getByName(host);
                return new ClientAuthentication(PVAAuth.ca, user, addr);
            }
            else // For other authentication methods, there should be no additional info structure
                if (info != null)
                    throw new Exception("Expected no authentication detail for '" + auth + "' but got " + info);
        }

        if (PVAAuth.x509.name().equals(auth))
            return new ClientAuthentication(PVAAuth.x509, tls_info.name, tls_info.host);

        return new ClientAuthentication(PVAAuth.anonymous, "nobody", tcp.getRemoteAddress().getAddress());
    }
}
