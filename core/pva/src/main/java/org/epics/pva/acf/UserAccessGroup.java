/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.acf;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Named list of users
 *
 *  May for example list all "expert" users
 *
 *  @author Kay Kasemir
 */
public record UserAccessGroup(String name, List<String> users)
{
    public UserAccessGroup(final String name, final List<String> users)
    {
        this.name = name;
        this.users = Collections.unmodifiableList(users);
    }

    @Override
    public String toString()
    {
        return "UAG(" + name + ") { " + users.stream()
                                             .collect(Collectors.joining(", "))
                              + " }";
    }
}
