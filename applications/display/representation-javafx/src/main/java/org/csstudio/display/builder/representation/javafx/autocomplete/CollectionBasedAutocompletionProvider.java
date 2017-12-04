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
    protected abstract Collection<Suggestion> getAllEntries();

    @Override
    public List<Suggestion> getEntries(String text)
    {
        text = text.toLowerCase();
        final List<Suggestion> matches = new ArrayList<>();
        for (Suggestion suggestion : getAllEntries())
            if (suggestion.getValue().toLowerCase().contains(text))
                matches.add(suggestion);
        return matches;
    }
}
