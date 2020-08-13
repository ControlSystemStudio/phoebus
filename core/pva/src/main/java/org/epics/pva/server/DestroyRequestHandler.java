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

/** Handle client's DESTROY_REQUEST command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class DestroyRequestHandler implements CommandHandler<ServerTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_DESTROY_REQUEST;
    }

    @Override
    public void handleCommand(final ServerTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        final int sid = buffer.getInt();
        final int req = buffer.getInt();
        logger.log(Level.FINE, "Received request destroy command for SID " + sid + " request " + req);

        // Locate monitor subscription
        final ServerPV pv = tcp.getServer().getPV(sid);
        if (pv != null)
            pv.unregisterSubscription(tcp, req);
        else
            logger.log(Level.FINE, "Received request destroy command for unknown PV with SID " + sid + " request " + req);
    }
}
