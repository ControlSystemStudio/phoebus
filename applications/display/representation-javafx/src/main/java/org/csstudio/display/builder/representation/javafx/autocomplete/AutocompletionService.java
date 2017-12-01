/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

/** Autocompletion Provider
 *  @author Kay Kasemir
 */
public class AutocompletionService
{
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final AutocompletionHistory history = new AutocompletionHistory();
    private final List<AutocompletionProvider> providers;
    private final List<Future<?>> submitted = new ArrayList<>();

    /** Create service that queries history and optional additional providers
     *  @param providers Optional additional providers
     */
    public AutocompletionService(final AutocompletionProvider... providers)
    {
        this.providers = new ArrayList<>(providers.length + 1);
        this.providers.add(history);
        this.providers.addAll(List.of(providers));
    }

    /** @param entry Entry that user selected so it needs to be added to history */
    public void addToHistory(final String entry)
    {
        history.add(entry);
    }

    /** Perform lookup of completions
     *  @param text Text entered by user
     *  @param response_handler will be called with name of provider and suggested completions
     */
    public synchronized void lookup(final String text, final BiConsumer<String, List<String>> response_handler)
    {
        // Typically called from the UI thread
        // when user enters text, but sync'ed
        // in case it's also called from other threads.

        // Cancel previous lookup
        for (Future<?> running : submitted)
            running.cancel(true);
        submitted.clear();

        // Start new lookup
        for (AutocompletionProvider provider : providers)
            submitted.add(pool.submit(() -> lookup(provider, text, response_handler)));
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

    private static void lookup(final AutocompletionProvider provider,
                               final String text,
                               final BiConsumer<String, List<String>> response_handler)
    {
        final List<String> entries = provider.getEntries(text);
        if (! Thread.currentThread().isInterrupted())
            response_handler.accept(provider.getName(), entries);
    }
}