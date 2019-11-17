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

@SuppressWarnings("nls")
class DestroyChannelRequest implements RequestEncoder
{
    private final PVAChannel channel;

    public DestroyChannelRequest(final PVAChannel channel)
    {
        this.channel = channel;
    }

    @Override
    public void encodeRequest(final byte version, final ByteBuffer buffer) throws Exception
    {
        logger.log(Level.FINE, () -> "Sending destroy channel request for " + channel);

        PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_DESTROY_CHANNEL, 4+4);
        // Protocol description claims CID followed by SID,
        // but as of May 2019 both the C++ and Java server expect SID, CID
        buffer.putInt(channel.getSID());
        buffer.putInt(channel.getCID());
    }
}
