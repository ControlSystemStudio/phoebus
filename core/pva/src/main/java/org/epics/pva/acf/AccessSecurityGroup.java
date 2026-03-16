/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.acf;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.epics.pva.acf.AccessRule.Mode;

/** Access security Group
 *
 *  PVs are in a named group,
 *  or the 'DEFAULT' group if not specifically assigned.
 *
 *  @author Kay Kasemir
 */
public class AccessSecurityGroup
{
    private final String name;
    private final List<AccessRule> rules = new ArrayList<>();

    AccessSecurityGroup(final String name)
    {
        this.name = name;
    }

    /** @return Name of the group */
    public String getName()
    {
        return name;
    }

    void add(final AccessRule rule)
    {
        rules.add(rule);
    }

    /** @param user Name of client that wants to write the PV
     *  @param host Host from which the client wants to write the PV
     *  @return May the client write the PV?
     */
    public boolean mayWrite(final String user, final InetAddress host)
    {
        boolean user_may_write = false;
        boolean host_may_write = false;

        for (AccessRule rule : rules)
        {
            if (rule.mode == Mode.WRITE)
            {
                // No uag listed -> Any user is accepted
                if (rule.users.isEmpty())
                    user_may_write = true;
                else
                    for (UserAccessGroup uag : rule.users)
                        if (uag.users().contains(user))
                        {
                            user_may_write = true;
                            break;
                        }

                // No hag listed -> Any host is accepted
                if (rule.hosts.isEmpty())
                    host_may_write = true;
                else
                    for (HostAccessGroup hag : rule.hosts)
                        if (hag.hosts().contains(host))
                        {
                            host_may_write = true;
                            break;
                        }

                if (user_may_write && host_may_write)
                    return true;
            }
        }

        return false;
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("ASG(").append(name).append(")");
        buf.append("\n{\n");
        for (var rule : rules)
            buf.append(rule).append("\n");
        buf.append("}");
        return buf.toString();
    }
}
