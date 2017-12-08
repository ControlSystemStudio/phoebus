/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import java.util.ArrayList;
import java.util.List;

/** Proposal for "sim://..." PVs
 *
 *  <p>Description includes the optional parameters.
 *
 *  <p>When applied to user text,
 *  it will preserve parameters that the user had
 *  already entered.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimProposal extends Proposal
{
    private final String[] arguments;

    public SimProposal(final String name, final String... arguments)
    {
        super(name);
        this.arguments = arguments;
    }

    @Override
    public String getDescription()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(value);
        if (arguments.length > 0)
        {
            buf.append('(');
            for (int i=0; i<arguments.length; ++i)
                if (i>0)
                    buf.append(", ").append(arguments[i]);
                else
                    buf.append(arguments[i]);
            buf.append(')');
        }
        return buf.toString();
    }

    @Override
    public List<MatchSegment> getMatch(final String text)
    {
        final List<MatchSegment> segs = new ArrayList<>();

        // Does text contain parameters?
        final int parm_start = text.indexOf('(');

        // First compare text up to optional parameters
        final String noparm_text = parm_start < 0 ? text : text.substring(0, parm_start);

        final int match = value.indexOf(noparm_text);
        // Text does not match the proposal??
        if (match < 0)
            segs.add(MatchSegment.normal(value));
        else
        {
            // Start of proposal ..
            if (match > 0)
                segs.add(MatchSegment.normal(value.substring(0, match)));

            // .. matching text ..
            segs.add(MatchSegment.match(noparm_text));

            // .. rest of proposal
            final int rest = match + noparm_text.length();
            if (value.length() > rest)
                segs.add(MatchSegment.normal(value.substring(rest)));
        }

        int parm = 0;
        if (parm_start >= 0)
        {
            // Handle parameters that already MATCH
            final StringBuilder buf = new StringBuilder();
            buf.append("(");
            String parm_text = text.substring(parm_start+1);
            int sep = findSep(parm_text);
            while (sep >= 0  &&  parm < arguments.length)
            {   // text contains parameter for another argument
                buf.append(parm_text.substring(0, sep+1));
                parm_text = parm_text.substring(sep+1);
                ++parm;
                sep = findSep(parm_text);
            }
            if (buf.length() > 0)
                segs.add(MatchSegment.match(buf.toString()));
        }

        // Add remaining parameters as COMMENT
        final StringBuilder buf = new StringBuilder();
        if (arguments.length > 0)
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

    private static int findSep(final String text)
    {
        // TODO Skip comma in quotes
        return text.indexOf(',');
    }

    @Override
    public String apply(String text)
    {
        // Text could be "sine" or "sim://sine(-10, 10, 2)"
        final int arg_start = text.indexOf('(');
        // No arguments?
        if (arg_start < 0)
            return super.apply(text);

        // Preserve arguments
        return value + text.substring(arg_start);
    }
}
