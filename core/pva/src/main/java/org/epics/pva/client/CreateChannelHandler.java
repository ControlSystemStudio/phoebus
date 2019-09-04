/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
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
import org.epics.pva.data.PVAStatus;

/** Handle a server's CREATE_CHANNEL reply
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class CreateChannelHandler implements CommandHandler<ClientTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_CREATE_CHANNEL;
    }

    @Override
    public void handleCommand(final ClientTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        if (buffer.remaining() < 4+4+1)
            throw new Exception("Incomplete Create Channel Response");
        final int cid = buffer.getInt();
        final int sid = buffer.getInt();
        final PVAStatus status = PVAStatus.decode(buffer);

        final PVAChannel channel = tcp.getClient().getChannel(cid);
        if (channel == null)
        {
            logger.log(Level.WARNING, this + " received create channel response for unknown channel ID " + cid);
            return;
        }

        if (status.isSuccess())
            channel.completeConnection(sid);
        else
        {
            logger.log(Level.WARNING, "Failed to create channel " + channel + ": " + status);

            // Reset channel to init state and search again, after delay
            channel.setState(ClientChannelState.INIT);
            tcp.getClient().search.register(channel, false);
        }
    }
}
