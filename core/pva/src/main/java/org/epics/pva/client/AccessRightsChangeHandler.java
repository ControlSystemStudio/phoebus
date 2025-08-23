/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import org.epics.pva.common.AccessRightsChange;
import org.epics.pva.common.CommandHandler;
import org.epics.pva.common.PVAHeader;

/** Handle a server's CMD_ACL_CHANGE message
 *  @author Kay Kasemir
 */
class AccessRightsChangeHandler implements CommandHandler<ClientTCPHandler>
{
    @Override
    public byte getCommand()
    {
        return PVAHeader.CMD_ACL_CHANGE;
    }

    @Override
    public void handleCommand(final ClientTCPHandler tcp, final ByteBuffer buffer) throws Exception
    {
        final AccessRightsChange acl = AccessRightsChange.decode(tcp.getRemoteAddress(), buffer.remaining(), buffer);
        if (acl == null)
            return;
        final PVAChannel channel = tcp.getClient().getChannel(acl.cid);
        if (channel == null)
        {
            logger.log(Level.WARNING, this + " received CMD_ACL_CHANGE for unknown channel ID " + acl.cid);
            return;
        }

        logger.log(Level.FINE, () -> "Received '" + channel.getName() + "' " + acl);
        channel.updateAccessRights(acl.havePUTaccess());
    }
}
