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

/** JUnit test of {@link SimProposal}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimProposalTest
{

    @Test
    public void testArguments()
    {
        List<String> split = SimProposal.splitNameAndParameters("sim://sine");
        assertThat(split, equalTo(List.of("sim://sine")));

        split = SimProposal.splitNameAndParameters("abcxyz");
        assertThat(split, equalTo(List.of("abcxyz")));

        split = SimProposal.splitNameAndParameters("sim://sine( 1, 2, 3, \"Fred\")");
        assertThat(split, equalTo(List.of("sim://sine", " 1", " 2", " 3", " \"Fred\"")));

        // Ignore comma in quotes
        split = SimProposal.splitNameAndParameters("sim://bogus(\"Hello, Dolly\")");
        assertThat(split, equalTo(List.of("sim://bogus", "\"Hello, Dolly\"")));

        // Get same result when final ')' is missing
        split = SimProposal.splitNameAndParameters("sim://sine( 1, 2, 3, \"Fred\"");
        assertThat(split, equalTo(List.of("sim://sine", " 1", " 2", " 3", " \"Fred\"")));

        // Ignore comma in quotes
        split = SimProposal.splitNameAndParameters("sim://bogus(\"Hello, Dolly\"");
        assertThat(split, equalTo(List.of("sim://bogus", "\"Hello, Dolly\"")));

        assertThat(SimProposal.hasOpeningBacket("sim://sine( 1, 2, 3, \"Fred\")"), equalTo(true));
        assertThat(SimProposal.hasOpeningBacket("abcxyz"), equalTo(false));

        assertThat(SimProposal.hasClosingBacket("sim://sine( 1, 2, 3, \"Fred\")"), equalTo(true));
        assertThat(SimProposal.hasClosingBacket("sim://sine( 1, 2, 3, \"Fred\""), equalTo(false));
    }

    @Test
    public void testSimProposal()
    {
        // Proposal for sim://sine with arguments
        Proposal proposal = new SimProposal("sim://sine", "min", "max", "update_seconds");

        // Description list arguments
        assertThat(proposal.getDescription(),
                   equalTo("sim://sine(min, max, update_seconds)"));

        // Partial "sine" turns into complete PV
        assertThat(proposal.apply("sine"),
                   equalTo("sim://sine"));
        // Arguments are preserved
        assertThat(proposal.apply("sim://sine(-10, 10, 2)"),
                   equalTo("sim://sine(-10, 10, 2)"));

        // Partial "sin" with arguments preserves the args
        assertThat(proposal.apply("sin(-10, 10, 2)"),
                   equalTo("sim://sine(-10, 10, 2)"));

        // Adds missing ")"
        assertThat(proposal.apply("sin(-10, 10, 2"),
                   equalTo("sim://sine(-10, 10, 2)"));

        // The other form of sim://sine
        proposal = new SimProposal("sim://sine", "min", "max", "steps", "update_seconds");
        assertThat(proposal.getDescription(),
                   equalTo("sim://sine(min, max, steps, update_seconds)"));

        // Arguments are fully preserved, incl. spacing
        assertThat(proposal.apply("sin(-10,10,    0.1, 2  )"),
                   equalTo("sim://sine(-10,10,    0.1, 2  )"));
    }

    @Test
    public void testMatch()
    {
        Proposal proposal = new SimProposal("sim://sine", "min", "max", "update_seconds");

        List<MatchSegment> match = proposal.getMatch("sine");
        assertThat(match, equalTo(List.of(MatchSegment.normal("sim://"),
                                          MatchSegment.match("sine"),
                                          MatchSegment.comment("(min, max, update_seconds)"))));

        match = proposal.getMatch("sim://sine");
        assertThat(match, equalTo(List.of(MatchSegment.match("sim://sine"),
                                          MatchSegment.comment("(min, max, update_seconds)"))));

        match = proposal.getMatch("sine(2, 4,");
        System.out.println("sine(2, 4,");
        for (MatchSegment m : match)
            System.out.println(m);
        assertThat(match, equalTo(List.of(MatchSegment.normal("sim://"),
                                          MatchSegment.match("sine"),
                                          MatchSegment.match("(2,", "(min,"),
                                          MatchSegment.match(" 4,", "max,"),
                                          MatchSegment.comment("update_seconds)"))));

        match = proposal.getMatch("sim://sine(-10, 10, 2)");
        System.out.println("sim://sine(-10, 10, 2)");
        for (MatchSegment m : match)
            System.out.println(m);
        assertThat(match, equalTo(List.of(MatchSegment.match("sim://sine"),
                                          MatchSegment.match("(-10,", "(min,"),
                                          MatchSegment.match(" 10,", "max,"),
                                          MatchSegment.match(" 2)", "update_seconds)"))));
    }

    @Test
    public void testLookup()
    {
        // Basic lookup by name
        List<Proposal> proposals = SimProposalProvider.INSTANCE.lookup("ine");
        List<String> names = proposals.stream().map(Proposal::getValue).collect(Collectors.toList());
        List<String> descr = proposals.stream().map(Proposal::getDescription).collect(Collectors.toList());
        assertThat(names, hasItems("sim://sine"));
        assertThat(descr, hasItems("sim://sine(min, max, update_seconds)",
                                   "sim://sine(min, max, steps, update_seconds)"));

        proposals = SimProposalProvider.INSTANCE.lookup("op");
        names = proposals.stream().map(Proposal::getValue).collect(Collectors.toList());
        assertThat(names, hasItems("sim://flipflop"));

        // Check number of parameters
        proposals = SimProposalProvider.INSTANCE.lookup("ine(-50, 50, 0.5)");
        descr = proposals.stream().map(Proposal::getDescription).collect(Collectors.toList());
        assertThat(descr, equalTo(List.of("sim://sine(min, max, update_seconds)")));
    }
}
