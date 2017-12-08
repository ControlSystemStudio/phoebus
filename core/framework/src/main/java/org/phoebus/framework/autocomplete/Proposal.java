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

/** Basic value-based proposal
 *
 *  <p>Holds a plain string,
 *  which will be used to replace entered text.
 *
 *  @author Kay Kasemir
 */
public class Proposal
{
    protected final String value;

    /** @param value Value of the proposal */
    public Proposal(final String value)
    {
        this.value = value;
    }

    /** @return value Value of the proposal */
    public String getValue()
    {
        return value;
    }

    /** @return Text to display when showing the proposal for user to select it */
    public String getDescription()
    {
        return value;
    }

    // TODO Better name
    public List<MatchSegment> getMatch(final String text)
    {
        final int match = value.indexOf(text);
        // Text does not match the proposal??
        if (match < 0)
            return List.of(MatchSegment.normal(value));
        else
        {
            // Start of proposal ..
            final List<MatchSegment> segs = new ArrayList<>();
            if (match > 0)
                segs.add(MatchSegment.normal(value.substring(0, match)));

            // .. matching text ..
            segs.add(MatchSegment.match(text));

            // .. rest of proposal
            final int rest = match + text.length();
            if (value.length() > rest)
                segs.add(MatchSegment.normal(value.substring(rest)));
            return segs;
        }
    }

    /** Apply the proposal
     *
     *  @param text Original text entered by user
     *  @return Result of applying this proposal to the text
     */
    public String apply(final String text)
    {
        return value;
    }
}
