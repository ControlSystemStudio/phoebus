/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import java.nio.ByteBuffer;

/** Handler for a PVA command received via {@link TCPHandler}
 *  @author Kay Kasemir
 */
public interface CommandHandler<TCP extends TCPHandler>
{
    /** @return {@link PVAHeader} <code>CMD_...</code> Command code that this handler handles */
    public byte getCommand();

    /** Handle a command
     *
     *  <p>Implementation has ownership of the 'receive'
     *  buffer while inside this method.
     *  When implementation returns, the {@link TCPHandler}
     *  re-uses the buffer.
     *
     *  @param tcp {@link TCPHandler} that received the message
     *  @param buffer Buffer positioned on message for command
     *  @throws Exception on error
     */
    public void handleCommand(final TCP tcp, ByteBuffer buffer) throws Exception;
}
