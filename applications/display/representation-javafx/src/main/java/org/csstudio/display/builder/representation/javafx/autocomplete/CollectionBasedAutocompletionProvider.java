/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.autocomplete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Helper for creating a collection-based {@link AutocompletionProvider}
 *  @author Kay Kasemir
 */
public abstract class CollectionBasedAutocompletionProvider implements AutocompletionProvider
{
    private final String name;

    /** @param name Name of this {@link AutocompletionProvider} */
    public CollectionBasedAutocompletionProvider(final String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    /** @return Entries from which matching entries will be fetched */
    protected abstract Collection<String> getAllEntries();

    @Override
    public List<String> getEntries(final String text)
    {
        final List<String> matches = new ArrayList<>();
        for (String item : getAllEntries())
            if (item.contains(text))
                matches.add(item);
        return matches;
    }
}
