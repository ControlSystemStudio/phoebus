/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;


import java.util.ServiceLoader;

import org.phoebus.framework.spi.PVProposalProvider;

/** Autocompletion Service for PVs
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVProposalService extends ProposalService
{
    public static final PVProposalService INSTANCE = new PVProposalService();

    private PVProposalService()
    {
        super(SimProposalProvider.INSTANCE, LocProposalProvider.INSTANCE);

        // Use SPI to add site-specific PV name providers
        for (PVProposalProvider add : ServiceLoader.load(PVProposalProvider.class))
        {
            logger.config("Adding PV Proposal Provider '" + add.getName() + "'");
            providers.add(add);
        }
    }
}
