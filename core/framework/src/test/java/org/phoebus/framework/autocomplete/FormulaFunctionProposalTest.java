/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.csstudio.apputil.formula.math.Sin;
import org.csstudio.apputil.formula.string.StringFunction;
import org.junit.Test;

/** JUnit test of {@link FormulaFunctionProposal}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaFunctionProposalTest
{
    @Test
    public void testMatch()
    {
        Proposal proposal = new FormulaFunctionProposal(new Sin());

        List<MatchSegment> match = proposal.getMatch("=sin(");
        assertThat(match, equalTo(List.of(MatchSegment.normal("="),
                                          MatchSegment.match("sin"),
                                          MatchSegment.match("("),
                                          MatchSegment.comment("", "angle"))));

        match = proposal.getMatch("=sin(2)");
        assertThat(match, equalTo(List.of(MatchSegment.normal("="),
                                          MatchSegment.match("sin"),
                                          MatchSegment.match("("),
                                          MatchSegment.match("2", "angle"),
                                          MatchSegment.match(")"))));

        match = proposal.getMatch("=2 + sin(1-1) + 3");
        assertThat(match, equalTo(List.of(MatchSegment.normal("=2 + "),
                                          MatchSegment.match("sin"),
                                          MatchSegment.match("("),
                                          MatchSegment.match("1-1", "angle"),
                                          MatchSegment.match(")"),
                                          MatchSegment.normal(" + 3"))));
    }

    @Test
    public void testVarArg()
    {
        Proposal proposal = new FormulaFunctionProposal(new StringFunction());

        List<MatchSegment> match = proposal.getMatch("=concat(a, b) + 2");
        assertThat(match, equalTo(List.of(MatchSegment.normal("="),
                                          MatchSegment.match("concat"),
                                          MatchSegment.match("(a, b)"),
                                          MatchSegment.normal(" + 2"))));

        match = proposal.getMatch("=concat(a, b + 2");
        assertThat(match, equalTo(List.of(MatchSegment.normal("="),
                                          MatchSegment.match("concat"),
                                          MatchSegment.match("(a, b + 2"),
                                          MatchSegment.comment(")"))));

    }
}
