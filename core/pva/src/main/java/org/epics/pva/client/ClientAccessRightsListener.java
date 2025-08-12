/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

/** Listener to a {@link PVAChannel} access rights
 *  @author Kay Kasemir
 */
@FunctionalInterface
public interface ClientAccessRightsListener
{
    /** Invoked when the channel access rights change
     *
     *  <p>Will be called as soon as possible, i.e. within
     *  the thread that handles the network communication.
     *
     *  <p>Client code <b>must not</b> block.
     *
     *  @param channel      Channel with updated permissions
     *  @param is_writable  May we write to the channel?
     */
    public void channelAccessRightsChanged(PVAChannel channel, boolean is_writable);
}
