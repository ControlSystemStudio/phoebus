/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/** Predefined, named color mappings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NamedColorMappings
{
    private static final CopyOnWriteArrayList<NamedColorMapping> mappings = new CopyOnWriteArrayList<>();

    // Prevent instantiation, global singleton
    private NamedColorMappings() {}

    public static void add(final NamedColorMapping mapping)
    {
        if (getMapping(mapping.getName()) != null)
            throw new IllegalArgumentException(mapping.getName() + " already defined");
        mappings.add(mapping);
    }

    /** @return All currently defined mappings */
    public static Collection<NamedColorMapping> getMappings()
    {
        return mappings;
    }

    /** Get color mapping by name
     *  @param name Name
     *  @return {@link ColorMappingFunction} or <code>null</code> when not found
     */
    public static NamedColorMapping getMapping(final String name)
    {
        for (NamedColorMapping mapping : mappings)
            if (mapping.getName().equals(name))
                return mapping;
        return null;
    }
}
