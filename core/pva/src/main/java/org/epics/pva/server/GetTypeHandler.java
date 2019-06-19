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
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;

/** Handle client's GET_TYPE command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class GetTypeHandler implements CommandHandler<ServerTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_GET_TYPE;
    }

    @Override
    public void handleCommand(final ServerTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        if (buffer.remaining() < 4+4+1)
            throw new Exception("Incomplete GET TYPE, only " + buffer.remaining());

        // int serverChannelID;
        final int sid = buffer.getInt();
        final ServerPV pv = tcp.getServer().getPV(sid);
        if (pv == null)
            throw new Exception("GET request for unknown PV sid " + sid);

        // int requestID
        final int req = buffer.getInt();

        // string subFieldName
        final String subfield = PVAString.decodeString(buffer);

        PVAData type = pv.getData();
        if (! subfield.isEmpty())
            type = ((PVAStructure)type).get(subfield);
        sendTypeInfoReply(tcp, req, pv, type);
    }

    static void sendTypeInfoReply(final ServerTCPHandler tcp, final int req, final ServerPV pv, final PVAData type)
    {
        tcp.submit((version, buffer) ->
        {
            logger.log(Level.FINE, () -> "Sending GET TYPE reply for " + pv + " as\n" + type.formatType());

            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, PVAHeader.CMD_GET_TYPE, 0);
            final int payload_start = buffer.position();
            // int requestID
            buffer.putInt(req);
            // Status status
            PVAStatus.StatusOK.encode(buffer);
            // FieldDesc
            final BitSet described = new BitSet();
            type.encodeType(buffer, described);
            final int payload_end = buffer.position();
            buffer.putInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE, payload_end - payload_start);
        });
    }
}
