/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import org.csstudio.display.builder.model.Messages;

/** Line Style
 *  @author Kay Kasemir
 */
public enum LineStyle
{
    // Ordinals match the line style used by legacy opibuilder
    SOLID(Messages.LineStyle_Solid),
    DASH(Messages.LineStyle_Dash),
    DOT(Messages.LineStyle_Dot),
    DASHDOT(Messages.LineStyle_DashDot),
    DASHDOTDOT(Messages.LineStyle_DashDotDot);
    private final String label;

    private LineStyle(final String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
