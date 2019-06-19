/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/** Helper for handling commands
 *
 *  <p>Dispatches a command to a {@link CommandHandler}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CommandHandlers<TCP extends TCPHandler>
{
    // Fastest lookup of command handlers is a list so that
    // dispatch.get(command) is the handler for a command.
    //
    // Relies on the fact that commands are currently
    // small numbers (up to ~0x15).
    // If commands covered a wider range,
    //    Map<Integer, CommandHandler>
    // would be the next option.
    private final List<CommandHandler<TCP>> dispatch = new ArrayList<>();

    /** Build command dispatcher
     *  @param handlers {@link CommandHandler}s
     */
    @SafeVarargs
    public CommandHandlers(final CommandHandler<TCP>... handlers)
    {
        for (CommandHandler<TCP> handler : handlers)
        {
            final byte command = handler.getCommand();
            // Can only handle commands in range 0..127
            if (command < 0)
                throw new IllegalStateException("Command " + command);
            while (dispatch.size() <= command)
                dispatch.add(null);
            dispatch.set(command, handler);
        }
    }

    /** Invoke handler for a command
     *
     *  @param command The command
     *  @param tcp The {@link TCPHandler} that can be used to e.g. submit another request
     *  @param buffer Buffer positioned on command header
     *  @return Was the command handled? Or is there no registered handler?
     *  @throws Exception on error
     */
    public boolean handleCommand(final byte command, final TCP tcp, ByteBuffer buffer) throws Exception
    {
        if (command >=0  &&  command < dispatch.size())
        {
            final CommandHandler<TCP> handler = dispatch.get(command);
            if (handler != null)
            {
                handler.handleCommand(tcp, buffer);
                return true;
            }
        }
        return false;
    }
}
