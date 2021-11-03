/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import org.epics.pva.common.CommandHandler;
import org.epics.pva.common.PVAHeader;

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
        final SearchResponseDecoder response;
        try
        {
            response = new SearchResponseDecoder(buffer.remaining(), buffer);
        }
        catch (Exception ex)
        {
            throw new Exception("PVA Server " + tcp + " sent invalid search reply", ex);
        }

        if (response.found)
        {
            for (int cid : response.cid)
            {
                final PVAChannel channel = tcp.getClient().getChannel(cid);

                if (channel == null)
                    logger.log(Level.FINE, "Got search response for unknown CID " + cid + " from " + response.server + " " + response.guid);
                else
                    logger.log(Level.FINE, "Got search response for " + channel + " from " + response.server + " " + response.guid);
                // TODO search_response.handleSearchResponse(cid, server, version, guid);
            }
        }

        tcp.markAlive();
    }
}
