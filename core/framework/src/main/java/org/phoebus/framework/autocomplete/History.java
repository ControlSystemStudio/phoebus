/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** Autocompletion based on previously entered values
 *
 *  <p>Remembers the exact value of a previously
 *  entered text and offers that as a proposal
 *  when a sub-section matches.
 *
 *  <p>History has limited size.
 *  New entries are always returned first,
 *  older entries are dropped off the list.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class History implements ProposalProvider
{
    private final LinkedList<String> history = new LinkedList<>();
    private final int max_size;

    public History()
    {
        this(10);
    }

    /** @param max_size Number of entries to keep in history */
    public History(final int max_size)
    {
        this.max_size = max_size;
    }

    @Override
    public String getName()
    {
        return "History";
    }

    /** @param proposal Proposal to add to history */
    public synchronized void add(final Proposal proposal)
    {
        final String value = proposal.getValue();
        if (history.size() >= max_size)
            history.removeLast();
        history.remove(value);
        history.addFirst(value);

    }

    @Override
    public synchronized List<Proposal> lookup(String text)
    {
        // Ignore case in search
        text = text.toLowerCase();
        final List<Proposal> matches = new ArrayList<>();
        for (String item : history)
            if (item.toLowerCase().contains(text))
                matches.add(new Proposal(item));
        return matches;
    }
}
