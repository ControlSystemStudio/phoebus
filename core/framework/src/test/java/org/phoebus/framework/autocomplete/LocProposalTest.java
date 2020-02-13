/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/** JUnit test of {@link LocProposal}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LocProposalTest
{
    @Test
    public void testArguments()
    {
        List<String> split = LocProposal.splitNameTypeAndInitialValues("loc://x");
        assertThat(split, equalTo(Arrays.asList("loc://x", null)));

        split = LocProposal.splitNameTypeAndInitialValues("loc://x(42)");
        assertThat(split, equalTo(Arrays.asList("loc://x", null, "42")));

        split = LocProposal.splitNameTypeAndInitialValues("loc://x<VLong>(42)");
        assertThat(split, equalTo(Arrays.asList("loc://x", "VLong", "42")));

        split = LocProposal.splitNameTypeAndInitialValues("loc://x(\"abc\")");
        assertThat(split, equalTo(Arrays.asList("loc://x", null, "\"abc\"")));

        split = LocProposal.splitNameTypeAndInitialValues("loc://x<VEnum>(1, \"a\", \"b\", \"c\")");
        assertThat(split, equalTo(Arrays.asList("loc://x", "VEnum", "1", "\"a\"", "\"b\"", "\"c\"")));
    }

    @Test
    public void testLocProposal()
    {
        Proposal proposal = new LocProposal("loc://x", null);

        assertThat(proposal.getDescription(),
                   equalTo("loc://x"));

        proposal = new LocProposal("loc://x", "Type", "initial value");
        assertThat(proposal.getDescription(),
                   equalTo("loc://x<Type>(initial value)"));

        proposal = new LocProposal("loc://x", "VLong", "initial value");
        assertThat(proposal.getDescription(),
                   equalTo("loc://x<VLong>(initial value)"));

        proposal = new LocProposal("loc://x", "VEnum", "1", "\"One\"", "\"Two\"", "\"Three\"");
        assertThat(proposal.getDescription(),
                   equalTo("loc://x<VEnum>(1, \"One\", \"Two\", \"Three\")"));
    }

    @Test
    public void testMatch()
    {
        Proposal proposal = new LocProposal("loc://x", "Type", "initial value");

        List<MatchSegment> match = proposal.getMatch("loc://x");
        assertThat(match, equalTo(List.of(
                MatchSegment.match("loc://x"),
                MatchSegment.comment("<VType>"),
                MatchSegment.comment("(initial value)"))));

        match = proposal.getMatch("");
        assertThat(match, equalTo(List.of(
                MatchSegment.normal("loc://x<Type>(initial value)"))));

        match = proposal.getMatch("loc://x<VLong>");
        assertThat(match, equalTo(List.of(
                MatchSegment.match("loc://x"),
                MatchSegment.normal("<Type>"),
                MatchSegment.comment("(initial value)"))));

        match = proposal.getMatch("loc://x(42)");
        assertThat(match, equalTo(List.of(
                MatchSegment.match("loc://x"),
                MatchSegment.comment("<VType>"),
                MatchSegment.match("(42)", "(initial value)"))));
    }
}
