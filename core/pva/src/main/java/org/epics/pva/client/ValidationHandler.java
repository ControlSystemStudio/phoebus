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
import java.util.ArrayList;
import java.util.List;

import org.epics.pva.common.CommandHandler;
import org.epics.pva.common.PVAHeader;
import org.epics.pva.data.PVASize;
import org.epics.pva.data.PVAString;

/** Handle a server's VALIDATION request
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ValidationHandler implements CommandHandler<ClientTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_CONNECTION_VALIDATION;
    }

    @Override
    public void handleCommand(final ClientTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        int payload_size = buffer.remaining();
        if (payload_size < 4+2+1)
            throw new Exception("Incomplete Validation Request");

        final int server_receive_buffer_size = buffer.getInt();
        final short server_introspection_registry_max_size = buffer.getShort();
        final List<String> auth = new ArrayList<>();
        final int size = PVASize.decodeSize(buffer);
        for (int i=0; i<size; ++i)
            auth.add(PVAString.decodeString(buffer));
        logger.fine("Received connection validation request");
        logger.finer(() -> "Server receive buffer size: " + server_receive_buffer_size);
        logger.finer(() -> "Server registry max size: " + server_introspection_registry_max_size);
        logger.finer(() -> "Server authentication methods: " + auth);

        // Support "ca" authorization, fall back to anonymouse
        final ClientAuthentication authentication;
        if (auth.contains("ca"))
            authentication = ClientAuthentication.CA;
        else
            authentication = ClientAuthentication.Anonymous;
        tcp.handleValidationRequest(server_receive_buffer_size,
                                    server_introspection_registry_max_size,
                                    authentication);
    }
}
