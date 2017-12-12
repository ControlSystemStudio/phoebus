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

/** Autocompletion Service
*
*  <p>When asked to lookup suggestions for some partial text,
*  the service will query one or more providers,
*  and return their replies as they arrive.
*
*  @author Kay Kasemir
*/
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
    public void addToHistory(final String entry)
    {
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


    /** Await completion of ongoing lookup
     *
     *  <p>For testing.
     *
     *  <p>Code that uses the service should simply
     *  handle calls to the response handler.
     */
    public synchronized void awaitCompletion() throws Exception
    {
        for (Future<?> running : submitted)
            running.get();
    }

    private static void lookup(final ProposalProvider provider,
                               final String text,
                               final int priority,
                               final Handler response_handler)
    {
        final List<Proposal> entries = provider.lookup(text);
        if (! Thread.currentThread().isInterrupted())
            response_handler.handleProposals(provider.getName(), priority, entries);
    }
}
