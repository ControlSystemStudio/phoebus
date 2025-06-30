/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.epics.pva.common.PVAHeader;
import org.epics.pva.common.RequestEncoder;

/** Send a 'ECHO' request to server
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class EchoRequest implements RequestEncoder
{
    // Random number so replies don't all start with 'echo00'
    private final AtomicInteger counter = new AtomicInteger((int)(Math.random() * 100));
    private volatile String active_check = "";

    @Override
    public void encodeRequest(final byte version, final ByteBuffer buffer) throws Exception
    {
        // Protocol always required server to echo the payload, but version 1 servers didn't
        if (version < 2)
        {
            logger.log(Level.FINE, () -> "Sending ECHO request (Version " + version + " without content)");
            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_ECHO, 0);
        }
        else
        {   // Issue next 'echo12'
            final int count = counter.incrementAndGet();
            active_check = String.format("echo%02d",  count % 100);
            final byte[] check = active_check.getBytes();
            logger.log(Level.FINE, () -> "Sending ECHO request (Version " + version + ")");
            PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_ECHO, check.length);
            buffer.put(check);
        }
    }

    /** @return Most recently sent echo request */
    public String getActiveRequest()
    {
        return active_check;
    }
}
