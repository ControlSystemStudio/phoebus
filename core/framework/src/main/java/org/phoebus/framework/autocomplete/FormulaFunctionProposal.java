/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.apputil.formula.spi.FormulaFunction;

/** Proposal that matches a FormulaFunction name and arguments
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class FormulaFunctionProposal extends Proposal
{
    private final FormulaFunction function;

    public FormulaFunctionProposal(final FormulaFunction function)
    {
        super(function.getName());
        this.function = function;
    }

    @Override
    public String getDescription()
    {
        return "=" + function.getSignature();
    }

    @Override
    public List<MatchSegment> getMatch(final String text)
    {
        // Does text contain the function name?
        final int match = text.indexOf(function.getName());
        if (match < 0)
            return List.of(MatchSegment.normal(getDescription()));

        // Copy text leading up to function name
        final List<MatchSegment> segs = new ArrayList<>();
        if (match > 0)
            segs.add(MatchSegment.normal(text.substring(0, match)));

        // Match function name
        segs.add(MatchSegment.match(function.getName()));

        // Is there a "(" to start arguments?
        final int len = text.length();
        int pos = match  + function.getName().length();
        if (pos < len  &&  text.charAt(pos) == '(')
        {
            if (function.isVarArgs())
            {
                int end = SimProposal.findClosingParenthesis(text, pos);
                if (end < len)
                {   // Located end, match complete arguments
                    segs.add(MatchSegment.match(text.substring(pos, end+1)));
                    pos = end+1;
                }
                else
                {   // Match opening '(' and args, suggest ')'
                    segs.add(MatchSegment.match(text.substring(pos, end)));
                    segs.add(MatchSegment.comment(")"));
                    pos = end;
                }
            }
            else
            {
                // Match provided arguments with arg. name as description
                segs.add(MatchSegment.match("("));
                ++pos;
                int end = SimProposal.nextSep(text, pos);
                for (int i=0;  i<function.getArguments().size();  ++i)
                {
                    if (i > 0)
                        segs.add(MatchSegment.normal(","));
                    if (pos > 0)
                    {
                        if (end > pos)
                        {
                            // Have text for this argument.
                            segs.add(MatchSegment.match(text.substring(pos, end),
                                                        function.getArguments().get(i)));
                            if (text.charAt(end) == ')')
                            {
                                segs.add(MatchSegment.match(")"));
                                pos = end + 1;
                                break;
                            }
                            else
                            {
                                pos = end + 1;
                                end = SimProposal.nextSep(text, pos);
                            }
                        }
                        else
                        {
                            segs.add(MatchSegment.comment(text.substring(pos),
                                                          function.getArguments().get(i)));
                            pos = end = -1;
                        }
                    }
                    else
                        segs.add(MatchSegment.comment(function.getArguments().get(i)));
                }
            }
        }
        else
        {
            // Show argument info as comment
            for (int i=0;  i<function.getArguments().size();  ++i)
            {
                if (i == 0)
                    segs.add(MatchSegment.comment("("));
                else
                    segs.add(MatchSegment.comment(","));
                segs.add(MatchSegment.comment(function.getArguments().get(i)));
            }
            segs.add(MatchSegment.comment(")"));
        }

        // Copy remaining text
        if (pos >= 0  &&  pos < len)
            segs.add(MatchSegment.normal(text.substring(pos)));

        return segs;
    }
}
