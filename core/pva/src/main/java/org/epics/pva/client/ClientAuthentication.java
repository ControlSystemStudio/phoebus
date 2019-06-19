/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.logging.Level;

import org.epics.pva.common.PVAAuth;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;

/** PVA Client authentication modes
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract class ClientAuthentication
{
    /** @param buffer Buffer to which client's authentication info is added
     *  @throws Exception on error
     */
    public abstract void encode(ByteBuffer buffer) throws Exception;

    // Must implement toString to describe authentication
    @Override
    public abstract String toString();


    /** Anonymous authentication */
    public static final ClientAuthentication Anonymous = new ClientAuthentication()
    {
        @Override
        public void encode(final ByteBuffer buffer) throws Exception
        {
            PVAString.encodeString(PVAAuth.ANONYMOUS, buffer);
        }

        @Override
        public String toString()
        {
            return PVAAuth.ANONYMOUS;
        }
    };


    /** CA authentication based on user name and host */
    public static final ClientAuthentication CA = new CAAuthentication();

    private static class CAAuthentication extends ClientAuthentication
    {
        private final String user;
        private String host;
        private final PVAStructure identity;

        CAAuthentication()
        {
            user = System.getProperty("user.name");
            host = "localhost";
            try
            {
                host = InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException ex)
            {
                logger.log(Level.WARNING, "Cannot determine host name", ex);
            }
            identity = new PVAStructure("", "",
                    new PVAString("user", user),
                    new PVAString("host", host));
        }

        @Override
        public void encode(final ByteBuffer buffer) throws Exception
        {
            PVAString.encodeString(PVAAuth.CA, buffer);
            identity.encodeType(buffer, new BitSet());
            identity.encode(buffer);
        }

        @Override
        public String toString()
        {
            return "ca(" + user + "@" + host + ")";
        }
    }
}
