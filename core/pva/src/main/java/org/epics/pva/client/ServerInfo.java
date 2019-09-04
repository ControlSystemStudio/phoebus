/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.epics.pva.server.Guid;

/** Info for a server
 *
 *  <p>Obtained via {@link PVAClient#list(java.util.concurrent.TimeUnit, long)}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ServerInfo
{
    final Guid guid;
    volatile int version = -1;
    final Set<InetSocketAddress> addresses = ConcurrentHashMap.newKeySet();

    ServerInfo(final Guid guid)
    {
        this.guid = guid;
    }

    public Guid getGuid()
    {
        return guid;
    }

    public int getVersion()
    {
        return version;
    }

    public Collection<InetSocketAddress> getAddresses()
    {
        return addresses;
    }

    @Override
    public String toString()
    {
        return guid + " version " + version + ": tcp@" + addresses;
    }
}
