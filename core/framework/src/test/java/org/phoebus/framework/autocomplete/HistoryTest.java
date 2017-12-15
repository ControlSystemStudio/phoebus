/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

@SuppressWarnings("nls")
public class HistoryTest
{
    private List<String> getValues(List<Proposal> proposals)
    {
        return proposals.stream()
                        .map(Proposal::getValue)
                        .collect(Collectors.toList());
    }

    @Test
    public void testPlainProposal()
    {
        Proposal proposal = new Proposal("Test1");
        assertThat(proposal.apply("est"), equalTo("Test1"));
    }

    @Test
    public void testHistory()
    {
        final History history = new History();

        // Starts out empty
        List<Proposal> proposals = history.lookup("test");
        assertThat(proposals.size(), equalTo(0));

        // Add two values
        history.add(new Proposal("Test1"));
        history.add(new Proposal("Other"));
        history.add(new Proposal("Test2"));

        // Now finding the most recent entry first
        proposals = history.lookup("test");
        assertThat(getValues(proposals), hasItems("Test1", "Test2"));
        assertThat(getValues(proposals), equalTo(List.of("Test2", "Test1")));

        proposals = history.lookup("ther");
        assertThat(getValues(proposals), equalTo(List.of("Other")));
        assertThat(proposals.get(0).apply("ther"), equalTo("Other"));
    }
}
