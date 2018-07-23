/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import org.csstudio.display.builder.model.Messages;

/** Options for confirming a press/release of button, checkbox etc.
 *  @author Kay Kasemir
 */
public enum ConfirmDialog
{
    NONE(Messages.Confirm_NONE),
    BOTH(Messages.Confirm_BOTH),
    PUSH(Messages.Confirm_PUSH),
    RELEASE(Messages.Confirm_RELEASE);

    private final String label;

    private ConfirmDialog(final String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
