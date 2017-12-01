/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.autocomplete;

import java.util.Collection;
import java.util.LinkedList;

/** Autocompletion based on previously entered values
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AutocompletionHistory extends CollectionBasedAutocompletionProvider
{
    private final LinkedList<String> history = new LinkedList<>();
    private final int max_size;

    public AutocompletionHistory()
    {
        this(20);
    }

    public AutocompletionHistory(final int max_size)
    {
        super("History");
        this.max_size = max_size;
    }

    @Override
    protected synchronized Collection<String> getAllEntries()
    {
        return history;
    }

    public synchronized void add(final String entry)
    {
        if (history.size() >= max_size)
            history.removeLast();
        history.remove(entry);
        history.addFirst(entry);
    }
}