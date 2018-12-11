/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.autocomplete;

import org.phoebus.framework.autocomplete.PVProposalService;

/** Auto-completion menu for PV names
 *  @author Kay Kasemir
 */
public class PVAutocompleteMenu extends AutocompleteMenu
{
    public static final PVAutocompleteMenu INSTANCE = new PVAutocompleteMenu();

    private PVAutocompleteMenu()
    {
        super(PVProposalService.INSTANCE);
    }
}
