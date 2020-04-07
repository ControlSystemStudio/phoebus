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

/** Handle client's DESTROY_CHANNEL command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class DestroyChannelHandler implements CommandHandler<ServerTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_DESTROY_CHANNEL;
    }

    @Override
    public void handleCommand(final ServerTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        final int sid = buffer.getInt();
        final int cid = buffer.getInt();
        final ServerPV pv = tcp.getServer().getPV(sid);
        if (pv == null)
        {
            logger.log(Level.WARNING, "Received destroy channel request for unknown SID " + sid + ", cid " + cid);
            return;
        }

        logger.log(Level.FINE, "Received destroy channel request for " + pv);
        // Doesn't delete the PV on the server,
        // only the client's idea of the channel connection
        pv.removeClient(tcp, cid);
        sendChannelDetroyed(tcp, cid, sid);
    }

    private void sendChannelDetroyed(final ServerTCPHandler tcp, final int cid, final int sid)
    {
        tcp.submit((version, buffer) ->
        {
            logger.log(Level.FINE, () -> "Sending destroy channel confirmation for SID " + sid + ", cid " + cid);
            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, PVAHeader.CMD_DESTROY_CHANNEL, 4+4);
            buffer.putInt(sid);
            buffer.putInt(cid);
        });
    }
}
