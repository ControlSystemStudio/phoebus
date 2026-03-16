/*******************************************************************************
 * Copyright (c) 2025-2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.net.InetAddress;

import org.epics.pva.common.PVAAuth;

/** Determine authorization of a client to access a PV on this server
 *
 *  Authorization is checked at two levels.
 *
 *  First, a search for a PV name may be permitted or blocked
 *  based on for example name patterns and IP addresses configured
 *  in a <code>*.pvlist</code> file.
 *
 *  If a search is permitted, server and client establish a TCP connection
 *  and exchange authentication details. Write access to a PV is then checked
 *  based on the client authentication.
 *
 *  This implementation, the default, replies to any search
 *  and allows write access to any authenticated client,
 *  only blocking anonymous clients.
 *
 *  @see {@link PVAServer.configureAuthorization}
 *
 *  @author Kay Kasemir
 */
public class ServerAuthorization
{
    /** @param pv_name Searched channel
     *  @param client Host from which the client issued the search
     *  @return Does the client have search access, should the server reply to the search?
     */
    public boolean allowSearch(final String pv_name, final InetAddress client)
    {
        // Derived implementation can check name and client address
        return true;
    }

    /** @param pv_name Channel for which to check write access
     *  @param client_auth Client authentication
     *  @return Does client have write access?
     */
    public boolean hasWriteAccess(final String pv_name, final ClientAuthentication client_auth)
    {
        // Derived implementation can check name and client authentication
        return client_auth.getType() != PVAAuth.anonymous;
    }
}
