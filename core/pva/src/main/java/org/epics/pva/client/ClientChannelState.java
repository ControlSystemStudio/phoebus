/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

/** State of a {@link PVAChannel}
 *  @author Kay Kasemir
 */
public enum ClientChannelState
{
    /** Initial state, channel has just been created */
    INIT,

    /** Actively looking for PVA servers that host this channel */
    SEARCHING,

    /** Found a PVA server, establishing TCP connection */
    FOUND,

    /** Connected via TCP */
    CONNECTED,

    /** Channel is closing, cannot be used again */
    CLOSING,

    /** Channel closing was confirmed by server, cannot be used again */
    CLOSED;

    /** @param state State to check
     *  @return <code>true</code> if state is searching, connected, ..,
     *          <code>false</code> if CLOSING or CLOSED and thus done.
     */
    public static boolean isActive(final ClientChannelState state)
    {
        return state.ordinal() < CLOSING.ordinal();
    }
};
