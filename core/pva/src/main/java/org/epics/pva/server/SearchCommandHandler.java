/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.nio.ByteBuffer;

import org.epics.pva.common.CommandHandler;
import org.epics.pva.common.PVAHeader;
import org.epics.pva.common.SearchRequest;

/** Handle client's SEARCH command received via TCP
 *  @author Kay Kasemir
 */
class SearchCommandHandler implements CommandHandler<ServerTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_SEARCH;
    }

    @Override
    public void handleCommand(final ServerTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        final byte version = buffer.get(PVAHeader.HEADER_OFFSET_VERSION);
        final int payload_size = buffer.getInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE);

        final SearchRequest search = SearchRequest.decode(tcp.getRemoteAddress(), version, payload_size, buffer);

        if (search.name != null)
            for (int i=0; i<search.name.length; ++i)
                tcp.getServer().handleSearchRequest(search.seq, search.cid[i], search.name[i],
                                                    search.client, tcp);
    }
}
