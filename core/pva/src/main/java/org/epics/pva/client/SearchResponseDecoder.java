/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.epics.pva.common.PVAHeader;
import org.epics.pva.data.PVAAddress;
import org.epics.pva.data.PVABool;
import org.epics.pva.data.PVAString;
import org.epics.pva.server.Guid;

/** Decode a server's SEARCH reply
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class SearchResponseDecoder
{
    /** Server's protocol version */
    public final int version;

    /** Server GUID */
    public final Guid guid;

    /** Search Sequence ID */
    public final int seq;

    /** Server's address, may be  all zero (any local) */
    public final InetSocketAddress server;

    /** Did server reply that channels are found, or did it send negative response? */
    public final boolean found;

    /** Channel IDs (client IDs) that server reports as found */
    public final int[] cid;

    /** Decode search response
     *
     *  @param payload Size of valid payload (may be less than buffer.remaining())
     *  @param buffer {@link ByteBuffer}
     *  @throws Exception on error
     */
    public SearchResponseDecoder(final int payload, final ByteBuffer buffer) throws Exception
    {
        // Get 'version' from within the PV
        int pos = buffer.position();
        if (pos < PVAHeader.HEADER_SIZE)
            throw new Exception("Cannot peek into PVA header of search reply, size is " + pos);
        buffer.position(pos - PVAHeader.HEADER_SIZE);
        version = buffer.get(1);
        buffer.position(pos);

        // Expect GUID + seqID + IP address + port + "tcp" + found + count ( + int[count] )
        if (payload < 12 + 4 + 16 + 2 + 4 + 1 + 2)
            throw new Exception("PVA Server sent only " + payload + " bytes for search reply");

        // Server GUID
        guid = new Guid(buffer);

        // Search Sequence ID
        seq = buffer.getInt();

        // Server's address and port
        final InetAddress addr;
        try
        {
            addr = PVAAddress.decode(buffer);
        }
        catch (Exception ex)
        {
            throw new Exception("PVA Server sent search reply with invalid address", ex);
        }
        final int port = Short.toUnsignedInt(buffer.getShort());

        // Use address from reply unless it's a generic local address
        if (addr.isAnyLocalAddress())
            server = new InetSocketAddress(port);
        else
            server = new InetSocketAddress(addr, port);

        final String protocol = PVAString.decodeString(buffer);
        if (! "tcp".equals(protocol))
            throw new Exception("PVA Server sent search reply #" + seq + " for protocol '" + protocol + "'");

        // Server may reply with list of PVs that it does _not_ have...
        found = PVABool.decodeBoolean(buffer);

        final int count = Short.toUnsignedInt(buffer.getShort());
        cid = new int[count];
        for (int i=0; i<count; ++i)
            cid[i] = buffer.getInt();
    }
}
