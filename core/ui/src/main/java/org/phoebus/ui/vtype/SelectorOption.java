/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.vtype;

import org.phoebus.ui.Messages;

/** Options for formatting a value
 *  @author Kay Kasemir
 */
public enum SelectorOption
{
    /** TODO update comment */
    NONE(Messages.Selector_None),

    /** TODO update comment */
    FILE(Messages.Selector_File),

    /** TODO update comment */
    DATETIME(Messages.Selector_Datetime);


    // To remain compatible with previous versions of this enum,
    // new options must be added to the end.

    private final String label;

    private SelectorOption(final String label)
    {
        this.label = label;
    }


    @Override
    public String toString()
    {
        return label;
    }
}
