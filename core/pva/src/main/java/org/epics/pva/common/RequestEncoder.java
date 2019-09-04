/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import java.nio.ByteBuffer;

/** Encode request to be sent via TCP.
 *
 *  <p>Submitted to {@link TCPHandler}.
 *
 *  @author Kay Kasemir
 */
@FunctionalInterface
public interface RequestEncoder
{
    /** Encode item to send via TCP
     *
     *  <p>Client or Server's {@link TCPHandler} calls this when it's ready to send
     *  a message to the server respectively client.
     *
     *  <p>Implementation has ownership of the 'send'
     *  buffer while inside this method.
     *  When implementation returns, the {@link TCPHandler} flips
     *  and sends the buffer content, then re-uses the buffer.
     *
     *  @param version Protocol version used by the server
     *  @param buffer Send buffer into which to encode item to send
     *  @throws Exception on error
     */
    public void encodeRequest(byte version, ByteBuffer buffer) throws Exception;
}
