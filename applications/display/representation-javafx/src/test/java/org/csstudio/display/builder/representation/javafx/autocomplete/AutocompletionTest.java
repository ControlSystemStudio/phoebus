/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.autocomplete;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.junit.Test;

/** JUnit test of {@link AutocompletionProvider}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AutocompletionTest
{
    static List<String> getValues(final List<Suggestion> entries)
    {
        return entries.stream().map(Suggestion::getValue).collect(Collectors.toList());
    }

    @Test
    public void testHistory()
    {
        final AutocompletionHistory history = new AutocompletionHistory();
        List<Suggestion> entries = history.getEntries("e");
        assertThat(entries, equalTo(List.of()));

        history.add("Test 1");
        history.add("Test 2");
        history.add("Xavor"); // contains no 'e'
        history.add("Fred");

        entries = history.getEntries("e");
        // List is most-recent-first
        assertThat(getValues(entries), equalTo(List.of("Fred", "Test 2", "Test 1")));

        history.add("Test 1");
        entries = history.getEntries("e");
        // List is most-recent-first
        assertThat(getValues(entries), equalTo(List.of("Test 1", "Fred", "Test 2")));


        // History is size-limited
        for (int i=0; i<100; ++i)
            history.add(String.format("Test %03d", i));
        // Should include "Test 099" entered last
        entries = history.getEntries("Test");
        // List is most-recent-first
        assertThat(getValues(entries), hasItems("Test 099"));
        // "Test 000" should have dropped off the list
        assertThat(getValues(entries), not(hasItems("Test 000")));
    }

    @Test
    public void testSimPVs()
    {
        final AutocompletionProvider auto = SimPVAutocompletion.INSTANCE;
        List<String> entries = getValues(auto.getEntries("e"));
        System.out.println(entries);
        assertThat(entries, hasItems("sim://sine"));

        entries = getValues(auto.getEntries("flop"));
        System.out.println(entries);
        assertThat(entries, hasItems("sim://flipflop"));
    }

    // Simulate slow AutocompletionProvider
    private static class Slowdown implements AutocompletionProvider
    {
        private final AutocompletionProvider base;

        public Slowdown(AutocompletionProvider base)
        {
            this.base = base;
        }

        @Override
        public String getName()
        {
            return base.getName();
        }

        @Override
        public List<Suggestion> getEntries(final String text)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                // Ignore
            }
            return base.getEntries(text);
        }
    }

    @Test
    public void testService() throws Exception
    {
        final AutocompletionService service = new AutocompletionService(new Slowdown(SimPVAutocompletion.INSTANCE));

        // Populate some historic entries
        service.addToHistory("Test 2");
        service.addToHistory("Test 1");

        final CopyOnWriteArrayList<String> result = new CopyOnWriteArrayList<>();
        final AutocompletionService.Handler response_handler = (name, priority, entries) ->
        {
            System.out.println(name + "(" + priority + ") returns " + entries);
            for (Suggestion suggestion : entries)
                result.add(suggestion.getValue());
        };

        service.lookup("est", response_handler);
        service.awaitCompletion();
        assertThat(result, hasItems("Test 1", "Test 2"));
        result.clear();

        service.lookup("flip", response_handler);
        service.awaitCompletion();
        assertThat(result, hasItems("sim://flipflop"));
        result.clear();

        service.lookup("e", response_handler);
        service.awaitCompletion();
        assertThat(result, hasItems("sim://sine", "Test 1", "Test 2"));
        result.clear();
    }
}
