/*******************************************************************************
 * Copyright (c) 2020-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.net.InetSocketAddress;

/** Invoked when client sends a generic 'list servers'
 *  as well as a specific PV name search
 */
@FunctionalInterface
public interface SearchHandler
{
    /** @param seq Client's search sequence
     *  @param cid Client channel ID or -1
     *  @param name Channel name or <code>null</code>
     *  @param addr Client's address and TCP port
     *  @return <code>true</code> if the search request was handled,
     *          i.e. the name was recognized and the request does not need
     *          to be forwarded or passed to anybody else
     */
    public boolean handleSearchRequest(int seq, int cid, String name, InetSocketAddress addr);
}
