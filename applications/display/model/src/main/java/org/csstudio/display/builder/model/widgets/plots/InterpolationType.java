/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.model.widgets.plots;

import org.csstudio.display.builder.model.Messages;

/** Interpolation from image to screen pixels
 *  @author Kay Kasemir
 */
public enum InterpolationType
{
    /** No interpolation */
    NONE(Messages.Interpolation_None),

    /** Interpolate */
    INTERPOLATE(Messages.Interpolation_Interpolate),

    /** Automatically enable/disable interpolation */
    AUTOMATIC(Messages.Interpolation_Automatic);

    final private String name;

    private InterpolationType(final String name)
    {
        this.name = name;
    }
    @Override
    public String toString()
    {
        return name;
    }
}
