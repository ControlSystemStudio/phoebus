/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.acf;

import java.io.Reader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;

/** Access security configuration
 *
 *  @author Kay Kasemir
 */
public class AccessConfig
{
    private final Map<String, UserAccessGroup> user_groups;
    private final Map<String, HostAccessGroup> host_groups;
    private final Map<String, AccessSecurityGroup> access_groups;

    /** @return Default configuration where DEFAULT grants write access to anybody
     *  @throws Exception on error
     */
    public static AccessConfig getDefault() throws Exception
    {
        final Reader reader = new InputStreamReader(AccessConfig.class.getResourceAsStream("default.acf"));
        return new AccessConfigParser().parse("default.acf", reader);
    }

    AccessConfig(Map<String, UserAccessGroup> user_groups,
                 Map<String, HostAccessGroup> host_groups,
                 Map<String, AccessSecurityGroup> access_groups)
    {
        this.user_groups = user_groups;
        this.host_groups = host_groups;
        this.access_groups = access_groups;
    }

    /** @return Names of user groups */
    public Collection<String> getUserGroupNames()
    {
        return user_groups.keySet();
    }

    /** @param name User group name
     *  @return {@link UserAccessGroup} or <code>null</code>
     */
    public UserAccessGroup getUserGroup(final String name)
    {
        return user_groups.get(name);
    }

    /** @return Names of host groups */
    public Collection<String> getHostGroupNames()
    {
        return host_groups.keySet();
    }

    /** @param name Host group name
     *  @return {@link HostAccessGroup} or <code>null</code>
     */
    public HostAccessGroup getHostGroup(final String name)
    {
        return host_groups.get(name);
    }

    /** @return Names of access security groups */
    public Collection<String> getAccessGroupNames()
    {
        return access_groups.keySet();
    }

    /** @param name Access security group name
     *  @return {@link AccessSecurityGroup} or <code>null</code>
     */
    public AccessSecurityGroup getAccessGroup(final String name)
    {
        return access_groups.get(name);
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        for (var uag : user_groups.values())
            buf.append(uag).append("\n");
        for (var hag : host_groups.values())
            buf.append(hag).append("\n");
        for (var asg : access_groups.values())
            buf.append(asg).append("\n");
        return buf.toString();
    }
}
