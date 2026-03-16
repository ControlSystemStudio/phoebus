/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.acf;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.epics.pva.common.Network;

/** Named list of hosts
 *
 *  May for example list all hosts in the control room
 *
 *  @author Kay Kasemir
 */
public record HostAccessGroup(String name, List<InetAddress> hosts)
{
    public HostAccessGroup(final String name, final List<InetAddress> hosts)
    {
        this.name = name;
        this.hosts = Collections.unmodifiableList(hosts);
    }

    @Override
    public String toString()
    {
        return "HAG(" + name + ") { " + hosts.stream()
                                             .map(Network::format)
                                             .collect(Collectors.joining(", "))
                              + " }";
    }
}
