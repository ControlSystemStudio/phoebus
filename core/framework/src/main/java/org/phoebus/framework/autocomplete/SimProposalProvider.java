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

/** Provider of {@link SimProposal}s
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimProposalProvider implements ProposalProvider
{
    public static final SimProposalProvider INSTANCE = new SimProposalProvider();

    private static final List<SimProposal> proposals = List.of(
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

    private SimProposalProvider()
    {
        // Singleton
    }

    /** Get proposals
     *
     *  @param text Text entered by user
     *  @return {@link Proposal}s that could be applied to the text
     */
    @Override
    public List<Proposal> lookup(final String text)
    {
        final List<Proposal> result = new ArrayList<>();

        // Does text contain parameters?
        final List<String> split = SimProposal.splitNameAndParameters(text);
        final String noparm_text = split.get(0);

        final int given = SimProposal.hasOpeningBacket(text)
                        ? split.size() - 1
                        : -1;
        final boolean complete_args = SimProposal.hasClosingBacket(text);

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

        return result;
    }
}
