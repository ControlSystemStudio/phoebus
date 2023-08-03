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

    public String getSignature()
    {
        return function.getSignature();
    }

    public String getName()
    {
        return function.getName();
    }

    public String[] getArguments()
    {
        return function.getArguments().toArray(new String[0]);
    }

    @Override
    public List<MatchSegment> getMatch(final String text)
    {
        // Does text contain parameters?
        String sub_text = text.substring(1);
        final List<String> split = SimProposal.splitNameAndParameters(sub_text);

        final List<MatchSegment> segs = new ArrayList<>(split.size());

        // First compare text up to optional parameters
        final String noparm_text = split.get(0);
        final String name = getName();
        final int match = name.toLowerCase().indexOf(noparm_text.toLowerCase());
        segs.add(MatchSegment.normal("="));
        // Text does not match the proposal??
        if (match < 0)
            segs.add(MatchSegment.normal(name));
        else
        {
            // Start of proposal ..
            if (match > 0)
                segs.add(MatchSegment.normal(name.substring(0, match)));

            // .. matching text ..
            segs.add(MatchSegment.match(getSignature().substring(match, match + noparm_text.length())));

            // .. rest of proposal
            final int rest = match + noparm_text.length();
            if (name.length() > rest)
                segs.add(MatchSegment.normal(name.substring(rest)));
        }

        String[] arguments = getArguments();

        final int common = Math.min(split.size()-1, arguments.length);
        int parm;
        for (parm = 0;  parm < common; ++parm)
        {
            final String another = parm < arguments.length-1 ? "," : ")";
            if (parm == 0)
                segs.add(MatchSegment.match("(" + split.get(parm+1) + another,
                        "(" + arguments[parm] + another));
            else
                segs.add(MatchSegment.match(split.get(parm+1) + another,
                        arguments[parm] + another));
        }

        // Add remaining parameters as COMMENT
        final StringBuilder buf = new StringBuilder();
        if (parm < arguments.length)
        {
            if (parm == 0)
                buf.append('(');
            for (/**/; parm<arguments.length; ++parm)
            {
                buf.append(arguments[parm]);
                if (parm < arguments.length-1)
                    buf.append(", ");
            }
            buf.append(')');
        }
        if (buf.length() > 0)
            segs.add(MatchSegment.comment(buf.toString()));

        return segs;
    }
}


