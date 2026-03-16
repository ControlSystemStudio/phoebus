/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.acf;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Rule that describes READ or WRITE access
 *
 *  @author Kay Kasemir
 */
class AccessRule
{
    enum Mode { READ, WRITE };

    public static final List<String> MODES = Stream.of(Mode.values())
                                                     .map(m -> m.name())
                                                     .toList();

    final int level;
    final Mode mode;
    final List<UserAccessGroup> users = new ArrayList<>();
    final List<HostAccessGroup> hosts = new ArrayList<>();

    AccessRule(final int level, final Mode mode)
    {
        this.level = level;
        this.mode = mode;
    }

    void add(final UserAccessGroup uag)
    {
        users.add(uag);
    }

    void add(final HostAccessGroup hag)
    {
        hosts.add(hag);
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("    RULE(").append(level).append(", ").append(mode).append(")");
        if (!users.isEmpty() || !hosts.isEmpty())
        {
            buf.append("\n    {\n");
            for (var uag : users)
                buf.append("        UAG(").append(uag.name()).append(")\n");
            for (var hag : hosts)
                buf.append("        HAG(").append(hag.name()).append(")\n");
            buf.append("    }");
        }
        return buf.toString();
    }
}
