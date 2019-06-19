/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
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

/** Handle response to client's ECHO command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class EchoHandler implements CommandHandler<ServerTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_ECHO;
    }

    @Override
    public void handleCommand(final ServerTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        final int payload_size = buffer.getInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE);
        // byte[] somePayload;
        final byte[] payload = new byte[payload_size];
        buffer.get(payload);
        logger.log(Level.FINE, () ->  "Received echo");
        sendEcho(tcp, payload);
    }

    private void sendEcho(final ServerTCPHandler tcp, final byte[] payload) throws Exception
    {
        tcp.submit((version, buffer) ->
        {
            logger.log(Level.FINE, () ->  "Replying to echo");
            PVAHeader.encodeMessageHeader(buffer,
                    PVAHeader.FLAG_SERVER,
                    PVAHeader.CMD_ECHO, payload.length);
            buffer.put(payload);
        });
    }
}
