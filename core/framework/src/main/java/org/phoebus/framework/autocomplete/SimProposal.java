/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
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
 *  <p>Description includes the optional parameters,
 *  which are shown as {@link MatchSegment#COMMENT}
 *  until the user provides a value for a parameter.
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

    String[] getArguments()
    {
        return arguments;
    }

    /** Split complete PV into base name and each argument
     *
     *  @param text "name(a, b, c)"
     *  @return [ name, a, b, c ]
     */
    static List<String> splitNameAndParameters(final String text)
    {
        // Locate start of parameters
        int sep = text.indexOf('(');
        if (sep < 0)
            return List.of(text);

        // Collect all comma-separated parameters
        final List<String> result = new ArrayList<>();
        result.add(text.substring(0, sep));
        int pos = sep+1;
        sep = nextSep(text, pos);
        while (sep >= 0)
        {
            result.add(text.substring(pos, sep));
            pos = sep + 1;
            sep = nextSep(text, pos);
        }
        // Handle remaining "xyz" or "xyz)"
        if (pos < text.length())
        {
            final String rest = text.substring(pos);
            sep = rest.lastIndexOf(')');
            if (sep < 0)
                result.add(rest);
            else
                result.add(rest.substring(0, sep));
        }
        return result;
    }

    /** Locate end of parameter, i.e. next ',' or ')'
     *
     *  Skips quoted text and nested parentheses
     *
     *  @param text Text
     *  @param start Offset where to start looking
     *  @return Next separator or -1
     */
    static int nextSep(final String text, final int start)
    {
        final int N = text.length();
        for (int pos = start;  pos < N;  ++pos)
        {
            char c = text.charAt(pos);
            if (c == ','  ||  c == ')')
                return pos;
            // Skip "text, quoted"
            if (c == '"'  &&   (pos <= 0  ||  text.charAt(pos-1) != '\\'))
            {
                while (pos+1 < N)
                {
                    c = text.charAt(++pos);
                    if (c == '"'  &&   (pos <= 0  ||  text.charAt(pos-1) != '\\'))
                        break;
                }
                // Unterminated quote? pos >= N
            }
            else if (c == '(')
            {   // Skip to closing parenthesis or N
                pos = findClosingParenthesis(text, pos);
            }
        }
        return -1;
    }

    /** Locate matching closing parenthesis
     *
     *  @param text Text
     *  @param start Start offset, should be on opening parenthesis
     *  @return Position of closing parenthesis or <code>text.length()</code>
     */
    static int findClosingParenthesis(final String text, final int start)
    {
        final int N = text.length();
        int open = 0;
        for (int pos=start; pos < N; ++pos)
        {
            char c = text.charAt(pos);
            if (c == '(')
                ++open;
            else if (c == ')')
                --open;
            if (open <= 0)
                return pos;
        }
        return N;
    }

    static boolean hasOpeningBacket(final String text)
    {
        return text.indexOf('(') >= 0;
    }

    static boolean hasClosingBacket(final String text)
    {
        return text.trim().lastIndexOf(')') >= 0;
    }

    @Override
    public List<MatchSegment> getMatch(final String text)
    {
        // Does text contain parameters?
        final List<String> split = splitNameAndParameters(text);
        if (split.isEmpty())
            return List.of();

        final List<MatchSegment> segs = new ArrayList<>(split.size());

        // First compare text up to optional parameters
        final String noparm_text = split.get(0);
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
