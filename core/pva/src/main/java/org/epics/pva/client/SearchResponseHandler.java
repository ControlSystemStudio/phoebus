/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import org.epics.pva.common.CommandHandler;
import org.epics.pva.common.PVAHeader;
import org.epics.pva.common.SearchResponse;

/** Handle a server's SEARCH reply
 *
 *  <p>Registered to handle the TCP request to searches.
 *  See {@link ChannelSearch} and {@link ClientUDPHandler}
 *  for UDP search and reply.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class SearchResponseHandler implements CommandHandler<ClientTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_SEARCH_RESPONSE;
    }

    @Override
    public void handleCommand(final ClientTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        final SearchResponse response;
        try
        {
            response = SearchResponse.decode(buffer.remaining(), buffer);
        }
        catch (Exception ex)
        {
            throw new Exception("PVA Server " + tcp + " sent invalid search reply", ex);
        }

        // If reply is "*:port", then use this TCP connection's address.
        // Client will find existing TCP connection via that address and re-use it.
        // This is the expected case when directly connecting to a PVA server via
        // its TCP port.
        // Otherwise, in case we queried a name server which then points to the actual PVA server,
        // use address as provided, and connect to it or re-use in case we already have a TCP connection to that address:port.
        InetSocketAddress server = response.server;
        if (server.getAddress().isAnyLocalAddress())
            server = tcp.getRemoteAddress();

        if (response.found)
        {
            for (int cid : response.cid)
            {
                final PVAChannel channel = tcp.getClient().getChannel(cid);

                if (channel == null)
                    logger.log(Level.FINE, "Got search response for unknown CID " + cid + " from " + server + " " +
                               response.guid + " V" + response.version);
                else
                {
                    logger.log(Level.FINE, "Got search response for " + channel + " from " + response.server +
                               " (using " + server + ") " + response.guid + " V" + response.version);
                    tcp.getClient().handleSearchResponse(cid, server, response.version, response.guid);
                }
            }
        }

        tcp.markAlive();
    }
}
