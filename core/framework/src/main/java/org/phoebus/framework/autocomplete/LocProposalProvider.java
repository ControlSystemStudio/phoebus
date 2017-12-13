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

/** Provider of {@link LocProposal}s
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LocProposalProvider implements ProposalProvider
{
    public static final LocProposalProvider INSTANCE = new LocProposalProvider();

    private static final List<Proposal> GENERIC = List.of(new LocProposal("loc://name", "VType", "initial values..."));

    private LocProposalProvider()
    {
        // Singleton
    }

    @Override
    public String getName()
    {
        return "Local PV";
    }

    /** Get proposals
     *
     *  @param text Text entered by user
     *  @return {@link Proposal}s that could be applied to the text
     */
    @Override
    public List<Proposal> lookup(final String text)
    {
        if (! text.startsWith("loc://"))
            return GENERIC;

        final List<Proposal> result = new ArrayList<>();
        final List<String> split = LocProposal.splitNameTypeAndInitialValues(text);
        String name = split.get(0).trim();
        if (name.isEmpty())
            name = "loc://name";
        else if (! name.startsWith("loc://"))
            name = "loc://" + name;

        final String type = split.get(1);
        if (type == null)
        {
            result.add(new LocProposal(name, null, "number"));
            result.add(new LocProposal(name, null, "\"string\""));
        }

        return result;
    }
}
