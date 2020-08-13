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
import org.epics.pva.data.PVAData;

/** Handle client's MONITOR command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class MonitorHandler implements CommandHandler<ServerTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_MONITOR;
    }

    @Override
    public void handleCommand(final ServerTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        if (buffer.remaining() < 4+4+1)
            throw new Exception("Incomplete MONITOR, only " + buffer.remaining());

        // int serverChannelID;
        final int sid = buffer.getInt();

        // int requestID
        final int req = buffer.getInt();

        // byte sub command = 0x08 for INIT
        final byte subcmd = buffer.get();

        final ServerPV pv = tcp.getServer().getPV(sid);
        if (pv == null)
        {
            GetHandler.sendError(tcp, PVAHeader.CMD_MONITOR, req, subcmd, "bad channel id");
            return;
        }

        if (subcmd == PVAHeader.CMD_SUB_INIT)
        {
            // FieldDesc pvRequestIF
            // PVField pvRequest
            final PVAData requested_type = tcp.getClientTypes().decodeType("", buffer);
            logger.log(Level.FINE, () -> "Received MONITOR INIT request for " + pv + " as\n" + requested_type.formatType());
            GetHandler.sendDataInitReply(tcp, PVAHeader.CMD_MONITOR, req, pv, requested_type);
        }
        else if (subcmd == PVAHeader.CMD_SUB_START)
        {
            logger.log(Level.FINE, () -> "Received MONITOR START for " + pv);
            // Register monitor to PV can keep sending updates as data changes
            pv.registerSubscription(new MonitorSubscription(req, pv, tcp));
        }
        else if (subcmd == PVAHeader.CMD_SUB_STOP  ||
                 subcmd == PVAHeader.CMD_SUB_DESTROY)
        {
            logger.log(Level.FINE, () -> "Received MONITOR STOP/DESTROY for  " + pv);
            // Stop/cancel/remove subscription
            pv.unregisterSubscription(tcp, req);
        }
        else
        {
            logger.log(Level.WARNING, () -> "Ignoring MONITOR request for " + pv + ", subcommand 0x" + Integer.toHexString(Byte.toUnsignedInt(subcmd)));
        }
    }
}
