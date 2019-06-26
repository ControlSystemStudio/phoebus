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
import java.util.BitSet;
import java.util.logging.Level;

import org.epics.pva.common.CommandHandler;
import org.epics.pva.common.PVAHeader;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAStatus;
import org.epics.pva.data.PVAStructure;

/** Handle client's RPC command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class RPCHandler implements CommandHandler<ServerTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_RPC;
    }

    @Override
    public void handleCommand(final ServerTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        if (buffer.remaining() < 4+4+1)
            throw new Exception("Incomplete RPC, only " + buffer.remaining());

        // int serverChannelID;
        final int sid = buffer.getInt();

        // int requestID
        final int req = buffer.getInt();

        // byte sub command = 0x08 for INIT
        final byte subcmd = buffer.get();

        final ServerPV pv = tcp.getServer().getPV(sid);
        if (pv == null)
        {
            sendError(tcp, PVAHeader.CMD_RPC, req, subcmd, "bad channel id");
            return;
        }

        if (subcmd == PVAHeader.CMD_SUB_INIT)
        {
            // FieldDesc pvRequestIF
            // PVField pvRequest
            final PVAData requested_type = tcp.getClientTypes().decodeType("", buffer);
            logger.log(Level.FINE, () -> "Received RPC INIT request for " + pv + " as\n" + requested_type.formatType());
            sendInitReply(tcp, PVAHeader.CMD_RPC, req, pv);
        }
        else
        {
            logger.log(Level.FINE, () -> "Received RPC call for " + pv);

            final PVAData data = tcp.getClientTypes().decodeType("", buffer);
            if (! (data instanceof PVAStructure))
                throw new Exception("Expected structure for RPC call to " + pv + ", got " + data);
            final PVAStructure parameters = (PVAStructure) data;
            parameters.decode(tcp.getClientTypes(), buffer);

            logger.log(Level.FINE, () -> "RPC call parameters:\n" + parameters);
            final PVAStructure result = pv.call(parameters);

            sendRCPReply(tcp, req, pv, result);
        }
    }

    static void sendError(final ServerTCPHandler tcp, final byte command, final int req, final byte subcmd, final String message)
    {
        tcp.submit((version, buffer) ->
        {
            logger.log(Level.FINE, () -> "Sending error: " + message);

            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, command, 0);
            final int payload_start = buffer.position();
            buffer.putInt(req);
            buffer.put(subcmd);
            final PVAStatus error = new PVAStatus(PVAStatus.Type.ERROR, message, "");
            error.encode(buffer);

            buffer.putInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE, buffer.position() - payload_start);
        });
    }

    static void sendInitReply(final ServerTCPHandler tcp, final byte command, final int req, final ServerPV pv)
    {
        tcp.submit((version, buffer) ->
        {
            logger.log(Level.FINE, () -> "Sending RPC INIT reply for " + pv);

            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, command, 4+1+1);
            // int requestID
            buffer.putInt(req);
            // byte subcommand
            buffer.put(PVAHeader.CMD_SUB_INIT);
            // Status status
            PVAStatus.StatusOK.encode(buffer);
        });
    }

    private void sendRCPReply(final ServerTCPHandler tcp, final int req, final ServerPV pv, final PVAStructure result)
    {
        tcp.submit((version, buffer) ->
        {
            logger.log(Level.FINE, () -> "Sending RPC reply for " + pv + ":\n" + result);

            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, PVAHeader.CMD_RPC, 0);
            final int payload_start = buffer.position();
            // int requestID
            buffer.putInt(req);
            // byte subcommand
            buffer.put((byte)0);
            // Status status
            PVAStatus.StatusOK.encode(buffer);
            // Result type and value
            result.encodeType(buffer, new BitSet());
            result.encode(buffer);

            // Correct payload size
            final int payload_end = buffer.position();
            buffer.putInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE, payload_end - payload_start);
        });
    }
}
