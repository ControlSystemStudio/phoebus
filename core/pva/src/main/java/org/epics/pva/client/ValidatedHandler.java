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
import org.epics.pva.data.PVAStatus;

/** Handle a server's VALIDATED confirmation
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ValidatedHandler implements CommandHandler<ClientTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_CONNECTION_VALIDATED;
    }

    @Override
    public void handleCommand(final ClientTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        final PVAStatus status = PVAStatus.decode(buffer);
        logger.log(Level.FINE, "Received server connection validation: " + status);
        // Mark connection as validated, allow sending data
        if (status.isSuccess())
            tcp.markValid();
    }
}
