/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.spi;

import org.phoebus.framework.autocomplete.PVProposalService;
import org.phoebus.framework.autocomplete.Proposal;
import org.phoebus.framework.autocomplete.ProposalProvider;

/** Provider of {@link Proposal}s for PV names
 *
 *  {@link PVProposalService} uses SPI for this interface
 *  to add site-specific PV name lookup.
 *
 *  @author Kay Kasemir
 */
public interface PVProposalProvider extends ProposalProvider
{

}
