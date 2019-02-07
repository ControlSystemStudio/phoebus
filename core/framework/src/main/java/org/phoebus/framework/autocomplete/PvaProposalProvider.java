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

import org.phoebus.framework.spi.PVProposalProvider;

/** Provider of {@link PvaProposal}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PvaProposalProvider implements PVProposalProvider
{
    public static final PvaProposalProvider INSTANCE = new PvaProposalProvider();

    private static final List<Proposal> generic = List.of(new Proposal("pva://name"));

    private PvaProposalProvider()
    {
        // Singleton
    }

    @Override
    public String getName()
    {
        return "PV Access PV";
    }

    /** Get proposals
     *  @param text Text entered by user
     *  @return {@link Proposal}s that could be applied to the text
     */
    @Override
    public List<Proposal> lookup(String text)
    {
        if (! text.startsWith("pva://"))
            return generic;

        final List<Proposal> result = new ArrayList<>();

        // Use the entered name, but add "pva://".
        // Default to just "pva://path"
        if (text.isEmpty())
            text = "pva://path";
        else if (! text.startsWith("pva://"))
            text = "pva://" + text;

        result.add(new Proposal(text));
        // If there's at least "pva://" plus one character for the name,
        // mention that it's possible to address subfields
        if (text.length() > 6  &&  text.indexOf('/', 7) < 0)
        {
            result.add(new Proposal(text + "/subfield"));
            result.add(new Proposal(text + "/subfield/subelement"));
        }
        return result;
    }
}
