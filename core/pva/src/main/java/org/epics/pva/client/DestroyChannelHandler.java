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

/** Handle a server's DESTROY_CHANNEL reply
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class DestroyChannelHandler implements CommandHandler<ClientTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_DESTROY_CHANNEL;
    }

    @Override
    public void handleCommand(final ClientTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        if (buffer.remaining() < 4+4)
            throw new Exception("Incomplete Destroy Channel Response");
        // Early protocol description suggested CID followed by SID,
        // but as of May 2019 both the C++ and Java server expect SID, CID
        final int sid = buffer.getInt();
        final int cid = buffer.getInt();

        final PVAChannel channel = tcp.getClient().getChannel(cid);
        if (channel == null)
        {
            logger.log(Level.WARNING, this + " received destroy channel response for unknown channel ID " + cid);
            return;
        }
        channel.channelDestroyed(sid);
    }
}
