/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import static org.epics.pva.PVASettings.logger;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import org.epics.pva.common.CommandHandler;
import org.epics.pva.common.PVAHeader;
import org.epics.pva.data.PVAStatus;
import org.epics.pva.data.PVAString;

/** Handle response to client's CREATE CHANNEL command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class CreateChannelHandler implements CommandHandler<ServerTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_CREATE_CHANNEL;
    }

    @Override
    public void handleCommand(final ServerTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        // short count;
        final int count = Short.toUnsignedInt(buffer.getShort());
        // { int clientChannelID, string channelName } channels[]
        for (int i=0; i<count; ++i)
        {
            final int cid = buffer.getInt();
            final String name = PVAString.decodeString(buffer);
            final ServerPV pv = tcp.getServer().getPV(name);
            if (pv == null)
                logger.log(Level.WARNING, () ->  "Channel create request for unknown PV '" + name + "'");
            else
            {
                logger.log(Level.FINE, () ->  "Channel create request '" + name + "', cid " + cid);
                pv.addClient(tcp, cid);
                sendChannelCreated(tcp, pv, cid);
            }
        }
    }

    private void sendChannelCreated(final ServerTCPHandler tcp, final ServerPV pv, int cid) throws Exception
    {
        tcp.submit((version, buffer) ->
        {
            logger.log(Level.FINE, () ->  "Confirm channel creation " + pv + " [CID " + cid + "]");
            PVAHeader.encodeMessageHeader(buffer,
                    PVAHeader.FLAG_SERVER,
                    PVAHeader.CMD_CREATE_CHANNEL, 4+4+1);

            // int cid
            buffer.putInt(cid);
            // int sid
            buffer.putInt(pv.getSID());
            // status
            PVAStatus.StatusOK.encode(buffer);
        });
    }
}
