/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.autocomplete;

import java.util.List;
import java.util.function.BiConsumer;

/** Autocompletion Provider
 *  @author Kay Kasemir
 */
public class AutocompletionService
{
    private final AutocompletionHistory history = new AutocompletionHistory();
    private final List<AutocompletionProvider> providers;

    /** Create service that queries history and optional additional providers
     *  @param providers Optional additional providers
     */
    public AutocompletionService(final AutocompletionProvider... providers)
    {
        this.providers = List.of(providers);
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
    public void lookup(final String text, final BiConsumer<String, List<String>> response_handler)
    {
        List<String> entries = history.getEntries(text);
        response_handler.accept(history.getName(), entries);

        for (AutocompletionProvider provider : providers)
        {
            entries = provider.getEntries(text);
            response_handler.accept(provider.getName(), entries);
        }
    }
}