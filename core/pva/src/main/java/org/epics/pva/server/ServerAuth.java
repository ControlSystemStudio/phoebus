/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.nio.ByteBuffer;

import org.epics.pva.common.PVAAuth;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.PVATypeRegistry;

/** Determine authorization of a client connected to this server
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract class ServerAuth
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
     *  @return ClientAuthorization
     *  @throws Exception on error
     */
    public static ServerAuth decode(final ServerTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        final String auth = PVAString.decodeString(buffer);
        if (PVAAuth.CA.equals(auth))
            return new CAServerAuth(tcp, buffer);
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

        public CAServerAuth(final ServerTCPHandler tcp, final ByteBuffer buffer) throws Exception
        {
            final PVATypeRegistry types = tcp.getClientTypes();

            if (buffer.remaining() < 1)
                throw new Exception("Missing 'ca' authentication info");

            final PVAData data = types.decodeType("", buffer);
            if (! (data instanceof PVAStructure))
                throw new Exception("Expected structure for 'ca' authentication info, got " + data);

            final PVAStructure info = (PVAStructure) data;
            info.decode(types, buffer);

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
            // TODO Implement access security
            if (channel.contains("demo"))
                return true;
            return false;
        }

        @Override
        public String toString()
        {
            return "ca(" + user + "@" + host + ")";
        }
    }
}
