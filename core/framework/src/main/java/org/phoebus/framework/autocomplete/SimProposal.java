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
    
    public String[] getArguments()
    {
        return arguments;
    }
    
    /** @param text Sim PV text with optional parameters
     *  @return [ name, parameters-without-'(' or null ]
     */
    static String[] splitBaseAndParameters(final String text)
    {
        final int parm_start = text.indexOf('(');
        final String noparm_text = parm_start < 0 ? text : text.substring(0, parm_start);
        final String parm_text = parm_start < 0 ? null : text.substring(parm_start+1);
        return new String[] { noparm_text, parm_text };
    }

    /** @param text Parameter text
     *  @param start Start position of search
     *  @return End of parameter, i.e. location of next comma or end of text 
     */
    static int findSep(final String text, final int start)
    {
        // TODO Skip comma in quotes
        int comma = text.indexOf(',', start);

        if (comma < 0)
            return text.length()-1;
        return comma;
    }

    @Override
    public List<MatchSegment> getMatch(final String text)
    {
        final List<MatchSegment> segs = new ArrayList<>();

        // Does text contain parameters?
        String[] split = splitBaseAndParameters(text);
        final String noparm_text = split[0];
        String parm_text = split[1];

        // First compare text up to optional parameters
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
        if (parm_text != null)
        {
            // Handle parameters that already MATCH
            int sep = findSep(parm_text, 0);
            while (sep >= 0  &&  parm < arguments.length)
            {   // text matches another argument
                final String another = parm < arguments.length-1 ? ", " : ")";
                if (parm == 0)
                    segs.add(MatchSegment.match("(" + parm_text.substring(0, sep+1),
                                                "(" + arguments[parm] + another));
                else
                    segs.add(MatchSegment.match(parm_text.substring(0, sep+1),
                                                arguments[parm] + another));
                parm_text = parm_text.substring(sep+1);
                ++parm;
                sep = findSep(parm_text, 0);
            }
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
