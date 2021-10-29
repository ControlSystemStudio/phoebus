/*******************************************************************************
 * Copyright (c) 2019-2021 Oak Ridge National Laboratory.
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

/** Handle a server's MONITOR reply
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class MonitorHandler implements CommandHandler<ClientTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_MONITOR;
    }

    @Override
    public void handleCommand(final ClientTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        // Dispatch to the initiating MonitorRequest
        if (buffer.remaining() < 4)
            throw new Exception("Incomplete Monitor Response");
        final int request_id = buffer.getInt(buffer.position());

        final ResponseHandler handler = tcp.getResponseHandler(request_id);
        if (handler != null)
            handler.handleResponse(buffer);
        else
        {
            // Is this a late update that was 'in flight' when the subscription got cancelled?
            // Check if the request_id is in the range of values that we have used.
            final int last_request = tcp.getClient().getLastRequestID();
            if (request_id > 0  &&  request_id <= last_request)
                logger.log(Level.FINE, "Received late Monitor Response for request " + request_id);
            else
                throw new Exception("Received Monitor Response for out-of-range request " + request_id);
        }
        tcp.markAlive();
    }
}
