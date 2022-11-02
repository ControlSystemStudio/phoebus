/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("nls")
public class ProposalTest
{
    @Test
    public void testPlainProposal()
    {
        // Plain proposal replaces what was entered
        Proposal proposal = new Proposal("Test1");
        assertThat(proposal.apply("est"),      equalTo("Test1"));
        assertThat(proposal.apply("anything"), equalTo("Test1"));
    }

    @Test
    public void testMatch()
    {
        Proposal proposal = new Proposal("Test1");

        List<MatchSegment> match = proposal.getMatch("es");
        assertThat(match, equalTo(List.of(MatchSegment.normal("T"),
                                          MatchSegment.match("es"),
                                          MatchSegment.normal("t1"))));

        match = proposal.getMatch("Tes");
        assertThat(match, equalTo(List.of(MatchSegment.match("Tes"),
                                          MatchSegment.normal("t1"))));
    }
}
