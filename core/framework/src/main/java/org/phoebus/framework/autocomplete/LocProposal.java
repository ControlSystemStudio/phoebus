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

/** Proposal for "loc://..." PVs
 *
 *  <p>Description includes the optional type and initial value,
 *  which are shown as {@link MatchSegment#COMMENT}
 *  until the user provides parameters for them.
 *
 *  <p>When applied to user text,
 *  it will preserve parameters that the user had
 *  already entered.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LocProposal extends Proposal
{
    private final String type;
    private final String[] initial_values;

    public LocProposal(final String name, final String type, final String... initial_values)
    {
        super(name);
        this.type = type;
        this.initial_values = initial_values;
    }

    @Override
    public String getDescription()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(value);
        if (type != null)
            buf.append('<').append(type).append('>');
        if (initial_values.length > 0)
        {
            buf.append('(');//

            for (int i=0; i<initial_values.length; ++i)
                if (i>0)
                    buf.append(", ").append(initial_values[i]);
                else
                    buf.append(initial_values[i]);
            buf.append(')');
        }
        return buf.toString();
    }

    /** Split complete PV into base name, type, initial values
     *
     *  @param text
     *  @return
     */
    static List<String> splitNameTypeAndInitialValues(final String text)
    {
        final List<String> result = new ArrayList<>();

        // Check for <type in loc://name<type>(v1, v2, ..)
        int sep = text.indexOf('<');
        if (sep >= 0)
        {   // "name"
            result.add(text.substring(0, sep).trim());

            // Find end of <type>
            int pos = text.indexOf('>', sep+1);
            if (pos < 0)
                // <type... is not terminated, use all as type
                result.add(text.substring(sep+1).trim());
            else
            {
                // <"type">
                result.add(text.substring(sep+1, pos).trim());

                // Check for initial values
                sep = text.indexOf('(', pos);
                if (sep >= 0)
                    addInitialValues(result, text.substring(sep+1));
            }
        }
        else
        {
            // No type.
            // Check for initial values
            sep = text.indexOf('(');
            if (sep >= 0)
            {   // Name, no type, values
                result.add(text.substring(0, sep).trim());
                result.add(null);
                addInitialValues(result, text.substring(sep+1));
            }
            else
            {
                // Just name, no type
                result.add(text.trim());
                result.add(null);
            }
        }
        return result;
    }

    private static void addInitialValues(final List<String> result, final String text)
    {
        // Collect all comma-separated parameters
        int pos = 0;
        int sep = SimProposal.nextSep(text, pos);
        while (sep >= 0)
        {
            result.add(text.substring(pos, sep).trim());
            pos = sep + 1;
            sep = SimProposal.nextSep(text, pos);
        }
        // Handle remaining "xyz" or "xyz)"
        if (pos < text.length())
        {
            final String rest = text.substring(pos);
            sep = rest.lastIndexOf(')');
            if (sep < 0)
                result.add(rest.trim());
            else
                result.add(rest.substring(0, sep).trim());
        }
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
        // Don't bother looking for '<type>' or '(initial)' when it's not a loc:// PV
        if (! text.startsWith("loc"))
            return List.of(MatchSegment.normal(getDescription()));

        // Does text contain parameters?
        final List<String> split = splitNameTypeAndInitialValues(text);
        if (split.size() < 1)
            return List.of();

        final List<MatchSegment> segs = new ArrayList<>(split.size());

        String name = split.get(0).trim();
        if (name.equals("loc://"))
        {   // Just "loc://" matches, add "name"
            segs.add(MatchSegment.match(name));
            segs.add(MatchSegment.normal("name"));
        }
        else // Show (partial) match between entered name and this proposal
            segs.addAll(super.getMatch(name));

        // No type provided?
        if (split.get(1) == null)
            segs.add(MatchSegment.comment("<VType>"));
        else if (type.toLowerCase().indexOf(split.get(1).toLowerCase()) >= 0)
            // Recognize piece of type, accept for full type
            segs.add(MatchSegment.match("<" + type + ">"));
        else
            // No type entered, would use this proposal's type when accepted
            segs.add(MatchSegment.normal("<" + type + ">"));

        // Add initial values
        final int common = Math.min(split.size()-2, initial_values.length);
        int parm;
        for (parm = 0;  parm < common; ++parm)
        {
            final String another = parm < initial_values.length-1 ? "," : ")";
            if (parm == 0)
                segs.add(MatchSegment.match("(" + split.get(parm+2) + another,
                                            "(" + initial_values[parm] + another));
            else
                segs.add(MatchSegment.match(split.get(parm+2) + another,
                                            initial_values[parm] + another));
        }

        // Add remaining init.values as COMMENT
        final StringBuilder buf = new StringBuilder();
        if (parm < initial_values.length)
        {
            if (parm == 0)
                buf.append('(');
            for (/**/; parm<initial_values.length; ++parm)
            {
                buf.append(initial_values[parm]);
                if (parm < initial_values.length-1)
                    buf.append(", ");
            }
            buf.append(')');
        }
        if (buf.length() > 0)
            segs.add(MatchSegment.comment(buf.toString()));

        return segs;
    }
}
