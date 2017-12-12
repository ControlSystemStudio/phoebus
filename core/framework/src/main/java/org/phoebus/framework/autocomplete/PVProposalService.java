/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

/** Autocompletion Service for PVs
 *  @author Kay Kasemir
 */
public class PVProposalService extends ProposalService
{
    public static final PVProposalService INSTANCE = new PVProposalService();

    private PVProposalService()
    {
        super(SimProposalProvider.INSTANCE);
    }
}
