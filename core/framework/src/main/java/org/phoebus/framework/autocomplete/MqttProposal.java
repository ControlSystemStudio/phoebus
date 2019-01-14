/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

import java.util.ArrayList;
import java.util.List;

/** Proposal for "mqtt://..." PVs
 *
 *  <p>Description includes the optional type,
 *  which are shown as {@link MatchSegment#COMMENT}
 *  until the user provides parameters for them.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MqttProposal extends Proposal
{
    private final String type;

    public MqttProposal(final String path, final String type)
    {
        super(path);
        this.type = type;
    }

    @Override
    public String getDescription()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(value);
        if (type != null)
            buf.append('<').append(type).append('>');
        return buf.toString();
    }

    /** Split complete PV into path, type
     *
     *  @param text
     *  @return
     */
    static List<String> splitPathType(final String text)
    {
        final List<String> result = new ArrayList<>();

        // Check for <type in mqtt://path<type>
        int sep = text.indexOf('<');
        if (sep >= 0)
        {   // "path"
            result.add(text.substring(0, sep).trim());

            // Find end of <type>
            int pos = text.indexOf('>', sep+1);
            if (pos < 0)
                // <type... is not terminated, use all as type
                result.add(text.substring(sep+1).trim());
            else
                // <"type">
                result.add(text.substring(sep+1, pos).trim());
        }
        else
        {   // Just path, no type
            result.add(text.trim());
            result.add(null);
        }
        return result;
    }

    @Override
    public List<MatchSegment> getMatch(final String text)
    {
        // Does text contain anything?
        final List<String> split = splitPathType(text);
        if (split.size() < 1)
            return List.of();

        final List<MatchSegment> segs = new ArrayList<>(split.size());

        String path = split.get(0).trim();
        if (path.equals("mqtt://"))
        {   // Just "mqtt://" matches, add "path"
            segs.add(MatchSegment.match(path));
            segs.add(MatchSegment.normal("path"));
        }
        else // Show (partial) match between entered path and this proposal
            segs.addAll(super.getMatch(path));

        // No type provided?
        if (split.get(1) == null)
            segs.add(MatchSegment.comment("<VType>"));
        else if (type.toLowerCase().indexOf(split.get(1).toLowerCase()) >= 0)
            // Recognize piece of type, accept for full type
            segs.add(MatchSegment.match("<" + type + ">"));
        else
            // No type entered, would use this proposal's type when accepted
            segs.add(MatchSegment.normal("<" + type + ">"));

        return segs;
    }
}
