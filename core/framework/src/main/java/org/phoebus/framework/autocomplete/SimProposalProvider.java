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

import org.phoebus.framework.spi.PVProposalProvider;

/** Provider of {@link SimProposal}s
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimProposalProvider implements PVProposalProvider
{
    public static final SimProposalProvider INSTANCE = new SimProposalProvider();

    private static final List<Proposal> generic = List.of(
        new SimProposal("sim://name", "parameters..."));

    /** All the simulated PVs with supported variations of their arguments */
    private static final List<SimProposal> all_proposals = List.of(
        new SimProposal("sim://const", "value"),
        new SimProposal("sim://flipflop", "update_seconds"),
        new SimProposal("sim://gaussianNoise", "center", "std_dev", "update_seconds"),
        new SimProposal("sim://gaussianwave", "period_seconds", "std_dev", "size", "update_seconds"),
        new SimProposal("sim://intermittent", "update_seconds" ),
        new SimProposal("sim://intermittent", "update_seconds", "value" ),
        new SimProposal("sim://intermittent", "update_seconds", "min", "max" ),
        new SimProposal("sim://noise", "min", "max", "update_seconds"),
        new SimProposal("sim://noisewave", "min", "max", "update_seconds"),
        new SimProposal("sim://noisewave", "min", "max", "size", "update_seconds"),
        new SimProposal("sim://ramp", "min", "max", "update_seconds"),
        new SimProposal("sim://ramp", "min", "max", "steps", "update_seconds"),
        new SimProposal("sim://sawtooth", "period_seconds", "wavelength", "size", "update_seconds"),
        new SimProposal("sim://sawtooth", "period_seconds", "wavelength", "size", "update_seconds", "min", "max"),
        new SimProposal("sim://sine", "min", "max", "update_seconds"),
        new SimProposal("sim://sine", "min", "max", "steps", "update_seconds"),
        new SimProposal("sim://sinewave", "period_seconds", "wavelength", "size", "update_seconds"),
        new SimProposal("sim://sinewave", "period_seconds", "wavelength", "size", "update_seconds", "min", "max"),
        new SimProposal("sim://strings", "update_seconds"),
        new SimProposal("sim://strings", "length", "update_seconds"));

    /** All SimProposals but using only the version with shortest argument list */
    private static final List<SimProposal> essential_proposals;

    /** Search lists from 'all' to smaller ones */
    private static final List<List<SimProposal>> search_lists;

    static
    {
        essential_proposals = new ArrayList<>();

        check_proposals:
        for (SimProposal proposal : all_proposals)
        {
            // Is there already a proposal by that name?
            for (int i=0; i<essential_proposals.size(); ++i)
                if (essential_proposals.get(i).getValue().equals(proposal.getValue()))
                {   // Does new one have a shorter argument list?
                    if (proposal.getArguments().length < essential_proposals.get(i).getArguments().length)
                        essential_proposals.set(i, proposal);
                    // Check next proposal, done with this one
                    continue check_proposals;
                }
            // New proposal, add
            essential_proposals.add(proposal);
        }

        search_lists = List.of(all_proposals, essential_proposals);
    }

    private SimProposalProvider()
    {
        // Singleton
    }

    @Override
    public String getName()
    {
        return "Simulated PV";
    }

    /** Get proposals
     *
     *  @param text Text entered by user
     *  @return {@link Proposal}s that could be applied to the text
     */
    @Override
    public List<Proposal> lookup(final String text)
    {
        // When nothing has been entered, suggest a generic simulated PV
        if (text.isEmpty())
            return generic;

        // Does text contain parameters?
        final List<String> split = SimProposal.splitNameAndParameters(text);
        final String noparm_text = split.get(0);

        final int given = SimProposal.hasOpeningBacket(text)
                        ? split.size() - 1
                        : -1;
        final boolean complete_args = SimProposal.hasClosingBacket(text);

        // Search 'all' proposals for match
        final List<Proposal> result = new ArrayList<>();

        for (List<SimProposal> proposals : search_lists)
        {
            // First compare text up to optional parameters
            for (SimProposal proposal : proposals)
                if (proposal.getValue().contains(noparm_text))
                {
                    // If text contains arguments, check them
                    if (given >= 0)
                    {
                        final int required = proposal.getArguments().length;
                        // Skip if text contains more arguments than proposal allows
                        if (given > required)
                            continue;
                        // Skip if text contains complete arguments "(...)" but wrong number
                        if (given != required  &&  complete_args)
                            continue;
                        // Text has fewer arguments, or not ending in "..)"
                    }
                    result.add(proposal);
                }

            if (result.size() <= essential_proposals.size())
                break;
            // If too many results, try again with shorter list
            result.clear();
        }
        return result;
    }
}
