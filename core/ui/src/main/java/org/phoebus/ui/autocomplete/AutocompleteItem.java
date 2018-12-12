/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.autocomplete;

import javafx.scene.Node;

/** Item shown in an {@link AutocompletePopup}
 *  @author Kay Kasemir
 */
class AutocompleteItem
{
    final Node representation;
    final Runnable action;

    /** @param representation Node to show in list
     *  @param action Action to run when item is selected. <code>null</code> for non-invokable items
     */
    AutocompleteItem(final Node representation, final Runnable action)
    {
        this.representation = representation;
        this.action = action;
    }
}