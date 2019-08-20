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

/** Provider of "sys://" PVs proposals
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SysProposalProvider implements PVProposalProvider
{
    public static final SysProposalProvider INSTANCE = new SysProposalProvider();

    private static final List<Proposal> generic = List.of(
        new Proposal("sys://name"));

    /** All the system PVs */
    private static final List<Proposal> all_proposals = List.of(
        new Proposal("sys://time"));

    private SysProposalProvider()
    {
        // Singleton
    }

    @Override
    public String getName()
    {
        return "System PV";
    }

    /** Get proposals
     *
     *  @param text Text entered by user
     *  @return {@link Proposal}s that could be applied to the text
     */
    @Override
    public List<Proposal> lookup(final String text)
    {
        // When nothing has been entered, suggest a generic system PV
        if (text.isEmpty())
            return generic;

        // Search 'all' proposals for match
        final List<Proposal> result = new ArrayList<>();

        for (Proposal proposal : all_proposals)
            if (proposal.getValue().contains(text))
                result.add(proposal);

        return result;
    }
}
