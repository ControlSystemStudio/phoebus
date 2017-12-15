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

import org.phoebus.framework.spi.PVProposalProvider;

/** Provider of {@link LocProposal}s
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LocProposalProvider implements PVProposalProvider
{
    public static final LocProposalProvider INSTANCE = new LocProposalProvider();

    private static final List<Proposal> generic = List.of(new LocProposal("loc://name", "VType", "initial value..."));

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
            return generic;

        final List<Proposal> result = new ArrayList<>();
        final List<String> split = LocProposal.splitNameTypeAndInitialValues(text);

        // Use the entered name, but add "loc://".
        // Default to just "loc://name"
        String name = split.get(0).trim();
        if (name.isEmpty())
            name = "loc://name";
        else if (! name.startsWith("loc://"))
            name = "loc://" + name;

        // Use the entered type, or default to "VType"
        String type = split.get(1);
        if (type != null)
        {
            result.add(new LocProposal(name, "VDouble", "number"));
            result.add(new LocProposal(name, "VLong", "number"));
            result.add(new LocProposal(name, "VString", "\"string\""));
            result.add(new LocProposal(name, "VEnum", "index", "\"Label 1\"", "\"Label 2\", ..."));
            result.add(new LocProposal(name, "VDoubleArray", "number", "number, ..."));
            result.add(new LocProposal(name, "VStringArray", "\"string\"", "\"string\", ..."));
            result.add(new LocProposal(name, "VTable"));
        }
        else
        {
            result.add(new LocProposal(name, "VType", "number"));
            result.add(new LocProposal(name, "VType", "\"string\""));
        }
        return result;
    }
}
