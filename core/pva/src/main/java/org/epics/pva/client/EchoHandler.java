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
import java.util.Arrays;
import java.util.logging.Level;

import org.epics.pva.common.CommandHandler;
import org.epics.pva.common.PVAHeader;
import org.epics.pva.data.Hexdump;

/** Handle a server's ECHO reply
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class EchoHandler implements CommandHandler<ClientTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_ECHO;
    }

    @Override
    public void handleCommand(final ClientTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        int payload_size = buffer.remaining();
        if (payload_size > 0)
        {
            final byte[] payload = new byte[payload_size];
            buffer.get(payload);
            if (Arrays.equals(payload, EchoRequest.CHECK))
                logger.log(Level.FINE, () -> "Received ECHO:\n" + Hexdump.toHexdump(payload));
            else
            {
                logger.log(Level.WARNING, this + " received invalid echo reply:\n" +
                                          Hexdump.toHexdump(payload));
                return;
            }
        }
        else
            logger.log(Level.FINE, "Received ECHO (no content)");
        tcp.markAlive();
    }
}
