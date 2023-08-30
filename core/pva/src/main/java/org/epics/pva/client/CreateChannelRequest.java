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

import org.epics.pva.common.PVAHeader;
import org.epics.pva.common.RequestEncoder;
import org.epics.pva.data.PVAString;

@SuppressWarnings("nls")
class CreateChannelRequest implements RequestEncoder
{
    private final PVAChannel channel;

    public CreateChannelRequest(final PVAChannel channel)
    {
        this.channel = channel;
    }

    @Override
    public void encodeRequest(final byte version, final ByteBuffer buffer) throws Exception
    {
        logger.log(Level.FINE, () -> "Sending create channel request for " + channel);

        PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_CREATE_CHANNEL, 2+4+PVAString.getEncodedSize(channel.getName()));
        // Not using PVASize, and only '1' is supported
        buffer.putShort((short)1);
        buffer.putInt(channel.getCID());
        PVAString.encodeString(channel.getName(), buffer);
    }
}
