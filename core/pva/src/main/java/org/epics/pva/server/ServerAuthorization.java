/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

/** Determine authorization of a client connected to this server
 *  @author Kay Kasemir
 */
public class ServerAuthorization
{
    /** @param pv_name Channel for which to check write access
     *  @param client_auth Client authentication
     *  @return Does client have write access?
     */
    public boolean hasWriteAccess(final String pv_name, final ClientAuthentication client_auth)
    {
        // TODO Implement authorization based on for example an ACF file
        return true;
    }
}
