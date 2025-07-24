/*******************************************************************************
 * Copyright (c) 2020-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

/** Invoked when client sends a generic 'list servers'
 *  as well as a specific PV name search
 */
@FunctionalInterface
public interface SearchHandler
{
    /** Server invokes this for every received name search.
     *
     *  <p>Implementation can check the name and either
     *  ignore the search, or invoke the reply callback
     *  with the TCP address of the server that provides
     *  the requested PV.
     *
     *  <p>Will also be invoked for "List all PVs" searches
     *  (name = null), but can only handle searches with
     *  actual names.
     *
     *  @param seq Client's search sequence
     *  @param cid Client channel ID or -1
     *  @param name Channel name or <code>null</code>
     *  @param client Client's address
     *  @param reply_sender Callback for TCP address of server.
     *                      Name servers that can resolve the name
     *                      will return the address of the PV's server.
     *                      If <code>null</code>, this server will
     *                      reply with its own address,
     *                      for usage by a gateway that wants so
     *                      indicate that it can now proxy that PV.
     *  @return <code>true</code> if the search request was handled,
     *          i.e. the name was recognized and the request does not need
     *          to be forwarded or passed to anybody else
     */
    public boolean handleSearchRequest(int seq, int cid, String name, InetSocketAddress client, Consumer<InetSocketAddress> reply_sender);
}
