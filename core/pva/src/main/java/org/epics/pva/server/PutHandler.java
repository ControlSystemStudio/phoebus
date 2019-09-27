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
import org.epics.pva.data.PVABitSet;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAStatus;
import org.epics.pva.data.PVAStructure;

/** Handle client's PUT command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class PutHandler implements CommandHandler<ServerTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_PUT;
    }

    @Override
    public void handleCommand(final ServerTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        // 0000 - CA 02 80 0B 00 00 00 0C 00 00 00 03 00 00 00 02 - ................
        // 0010 - 08 FE 00 02                                     - ....

        if (buffer.remaining() < 4+4+1)
            throw new Exception("Incomplete PUT, only " + buffer.remaining());

        // int serverChannelID;
        final int sid = buffer.getInt();

        // int requestID
        final int req = buffer.getInt();

        // byte sub command = 0x08 for INIT
        final byte subcmd = buffer.get();

        final ServerPV pv = tcp.getServer().getPV(sid);
        if (pv == null)
        {
            GetHandler.sendError(tcp, PVAHeader.CMD_PUT, req, subcmd, "bad channel id");
            return;
        }

        if (subcmd == PVAHeader.CMD_SUB_INIT)
        {
            // Client starts 'PUT' request, reply with data description
            // FieldDesc pvRequestIF
            // PVField pvRequest
            final PVAData requested_type = tcp.getClientTypes().decodeType("", buffer);
            logger.log(Level.FINE, () -> "Received PUT INIT request for " + pv + " as\n" + requested_type.formatType());
            GetHandler.sendDataInitReply(tcp, PVAHeader.CMD_PUT, req, pv, requested_type);
        }
        else if (subcmd == PVAHeader.CMD_SUB_GET)
        {
            // Client fetches current value before actually writing (optional)
            logger.log(Level.FINE, () -> "Received GET-PUT for " + pv);
            GetHandler.sendGetReply(tcp, PVAHeader.CMD_PUT, PVAHeader.CMD_SUB_GET, req, pv);
        }
        else if (subcmd == 0  ||  subcmd == PVAHeader.CMD_SUB_DESTROY)
        {
            // Client wrote
            logger.log(Level.FINE, () -> "Received PUT for " + pv + ", subcommand " + String.format("0x%02X ", subcmd));

            final BitSet written = PVABitSet.decodeBitSet(buffer);

            // Check write access in general and for this client
            if (! (pv.isWritable() && tcp.getAuth().hasWriteAccess(pv.getName())))
            {
                GetHandler.sendError(tcp, PVAHeader.CMD_PUT, req, subcmd, "No write access to " + pv.getName());
                return;
            }

            // BitSet toPutBitSet;
            // PVField pvPutStructureData;
            // XXX Directly update pv.data, not getData() clone?
            final PVAStructure data = pv.getData();
            data.decodeElements(written, tcp.getClientTypes(), buffer);

            if (logger.isLoggable(Level.FINE))
            {
                for (int index = written.nextSetBit(0);
                     index >= 0;
                     index = written.nextSetBit(index + 1))
                {
                    final PVAData element = data.get(index);
                    logger.log(Level.FINE, "Client wrote element " + index + ": " + element);
                    // Javadoc for nextSetBit() suggests checking for MAX_VALUE
                    // to avoid index + 1 overflow and thus starting over with first bit
                    if (index == Integer.MAX_VALUE)
                        break;
                }
            }

            // Notify PV
            try
            {
                pv.wrote(written, data);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Write failed to " + pv, ex);
                // XXX Send stack trace with error status?
                GetHandler.sendError(tcp, PVAHeader.CMD_PUT, req, subcmd, "Write failed");
                return;
            }

            sendPutDone(tcp, req, subcmd);
        }
    }

    private static void sendPutDone(final ServerTCPHandler tcp, final int req, final byte subcmd)
    {
        tcp.submit((version, buffer) ->
        {
            logger.log(Level.FINE, () -> "Sending PUT response");
            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, PVAHeader.CMD_PUT, 4+1+1);
            // int requestID
            buffer.putInt(req);
            // byte subcommand
            buffer.put(subcmd);
            // Status status
            PVAStatus.StatusOK.encode(buffer);
        });
    }
}
