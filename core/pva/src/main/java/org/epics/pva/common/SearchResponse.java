/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.epics.pva.data.PVAAddress;
import org.epics.pva.data.PVABool;
import org.epics.pva.data.PVAString;
import org.epics.pva.server.Guid;

/** Decode a server's SEARCH reply
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SearchResponse
{
    /** Server's protocol version */
    public int version;

    /** Server GUID */
    public Guid guid;

    /** Search Sequence ID */
    public int seq;

    /** Server's address, may be  all zero (any local) */
    public InetSocketAddress server;

    /** Did server reply that channels are found, or did it send negative response? */
    public boolean found;

    /** Channel IDs (client IDs) that server reports as found */
    public int[] cid;

    /** Decode search response
     *
     *  @param payload Size of valid payload (may be less than buffer.remaining())
     *  @param buffer {@link ByteBuffer}
     *  @return {@link SearchResponse}
     *  @throws Exception on error
     */
    public static SearchResponse decode(final int payload, final ByteBuffer buffer) throws Exception
    {
        final SearchResponse result = new SearchResponse();
        // Get 'version' from within the PV
        int pos = buffer.position();
        if (pos < PVAHeader.HEADER_SIZE)
            throw new Exception("Cannot peek into PVA header of search reply, size is " + pos);
        result.version = buffer.get(pos - PVAHeader.HEADER_SIZE + PVAHeader.HEADER_OFFSET_VERSION);

        // Expect GUID + seqID + IP address + port + "tcp" + found + count ( + int[count] )
        if (payload < 12 + 4 + 16 + 2 + 4 + 1 + 2)
            throw new Exception("PVA Server sent only " + payload + " bytes for search reply");

        // Server GUID
        result.guid = new Guid(buffer);

        // Search Sequence ID
        result.seq = buffer.getInt();

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
            result.server = new InetSocketAddress(port);
        else
            result.server = new InetSocketAddress(addr, port);

        final String protocol = PVAString.decodeString(buffer);
        if (! "tcp".equals(protocol))
            throw new Exception("PVA Server sent search reply #" + result.seq + " for protocol '" + protocol + "'");

        // Server may reply with list of PVs that it does _not_ have...
        result.found = PVABool.decodeBoolean(buffer);

        final int count = Short.toUnsignedInt(buffer.getShort());
        result.cid = new int[count];
        for (int i=0; i<count; ++i)
            result.cid[i] = buffer.getInt();

        return result;
    }

    /** Encode a search response
     *  @param guid This server's GUID
     *  @param seq Client search request sequence number
     *  @param cid Client's channel ID or -1
     *  @param address Address where client can connect to access the channel
     *  @param port Associated TCP port
     *  @param buffer Buffer into which search response will be encoded
     */
    public static void encode(final Guid guid, final int seq, final int cid,
                              final InetAddress address, final int port,
                              final ByteBuffer buffer)
    {
        PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, PVAHeader.CMD_SEARCH_RESPONSE, 12+4+16+2+4+1+2+ (cid < 0 ? 0 : 4));

        // Server GUID
        guid.encode(buffer);

        // Search Sequence ID
        buffer.putInt(seq);

        // Server's address and port
        PVAAddress.encode(address, buffer);
        buffer.putShort((short)port);

        // Protocol
        PVAString.encodeString("tcp", buffer);

        // Found
        PVABool.encodeBoolean(cid >= 0, buffer);

        // int[] cid;
        if (cid < 0)
            buffer.putShort((short)0);
        else
        {
            buffer.putShort((short)1);
            buffer.putInt(cid);
        }
    }
}
