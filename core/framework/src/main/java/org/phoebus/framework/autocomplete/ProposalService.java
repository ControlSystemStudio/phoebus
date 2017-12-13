/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Proposal Service
 *
 *  <p>When asked to lookup proposals for some text entered by user,
 *  the service will query one or more providers,
 *  and return their replies as they arrive.
 *
 *  @see PVProposalService
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ProposalService
{
    @FunctionalInterface
    public interface Handler
    {
        /** Called when the {@link ProposalService} has proposals for a lookup
         *
         *  @param name Name of the suggestion provider ("History", "Simulated PVs", ..)
         *  @param priority Suggested priority for listing this result compared to others
         *  @param proposals {@link Proposal}s
         */
        public void handleProposals(String name, int priority, List<Proposal> proposals);
    }

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final History history = new History();
    protected final List<ProposalProvider> providers;
    private final List<Future<?>> submitted = new ArrayList<>();

    protected ProposalService(final ProposalProvider... providers)
    {
        this.providers = new ArrayList<>(providers.length + 1);
        this.providers.add(history);
        this.providers.addAll(List.of(providers));
    }

    /** @param entry Entry that user selected so it needs to be added to history */
    public void addToHistory(String entry)
    {
        entry = entry.trim();
        if (! entry.isEmpty()  &&  ! entry.startsWith("#"))
            history.add(new Proposal(entry));
    }

    /** Perform lookup of completions
     *  @param text Text entered by user
     *  @param response_handler will be called with name of provider and suggested completions
     */
    public synchronized void lookup(final String text, final Handler response_handler)
    {
        // Typically called from the UI thread
        // when user enters text, but sync'ed
        // in case it's also called from other threads.

        // Cancel previous lookup
        for (Future<?> running : submitted)
            running.cancel(true);
        submitted.clear();

        // Start new lookup
        int i = 0;
        for (ProposalProvider provider : providers)
        {
            final int priority = i++;
            submitted.add(pool.submit(() -> lookup(provider, text, priority, response_handler)));
        }
    }

    /** Lookup text in one provider
     *
     *  <p>Called from pool
     *
     *  @param provider {@link ProposalProvider} to use
     *  @param text Text to look up
     *  @param priority Priority of the results
     *  @param response_handler {@link Handler} to call
     */
    private static void lookup(final ProposalProvider provider,
                               final String text,
                               final int priority,
                               final Handler response_handler)
    {
        final List<Proposal> entries = provider.lookup(text);
        if (entries.size() > 0  &&  ! Thread.currentThread().isInterrupted())
            response_handler.handleProposals(provider.getName(), priority, entries);
    }
}
