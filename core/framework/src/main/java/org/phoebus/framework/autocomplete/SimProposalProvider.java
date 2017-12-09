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
public class SimProposalProvider implements ProposalProvider
{
    public static final SimProposalProvider INSTANCE = new SimProposalProvider();
    
    private static final List<SimProposal> proposals = List.of(
        new SimProposal("sim://sine", "min", "max", "update_seconds"),
        new SimProposal("sim://sine", "min", "max", "steps", "update_seconds"),
        new SimProposal("sim://ramp", "min", "max", "update_seconds"),
        new SimProposal("sim://ramp", "min", "max", "steps", "update_seconds"),
        new SimProposal("sim://flipflop", "update_seconds"),
        new SimProposal("sim://noise", "min", "max", "update_seconds"));

    private SimProposalProvider()
    {
        // Singleton
    }
    
    private static int countArgs(final String text)
    {
        int count = 1;
        int start = 0;
        int sep = SimProposal.findSep(text, start);
        while (sep >= 0  &&  sep < text.length()-1)
        {
            ++count;
            start = sep+1;
            sep = SimProposal.findSep(text, start);
        }
        return count;
    }
    
    /** Get proposals
     *
     *  @param text Text entered by user
     *  @return {@link Proposal}s that could be applied to the text
     */
    public List<Proposal> lookup(String text)
    {
        final List<Proposal> result = new ArrayList<>();
        
        // Does text contain parameters?
        final String[] split = SimProposal.splitBaseAndParameters(text);
        final String noparm_text = split[0];
        final String parm_text = split[1];

        // First compare text up to optional parameters
        for (SimProposal proposal : proposals)
            if (proposal.getValue().contains(noparm_text))
            {
                // If text contains arguments, check them
                if (parm_text != null)
                {
                    final int given = countArgs(parm_text);
                    final int required = proposal.getArguments().length;
                    // Skip if text contains more arguments than proposal allows
                    if (given > required)
                        continue;
                    // Skip if text contains complete arguments "(...)" but wrong number
                    if (given != required  &&  text.trim().endsWith(")"))
                        continue;
                    // Text has fewer arguments, or not ending in "..)"
                }
                result.add(proposal);
            }
        
        return result;
    }
}
