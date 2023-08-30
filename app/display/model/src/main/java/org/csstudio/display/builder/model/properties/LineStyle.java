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
    /** Line style */
    SOLID(Messages.LineStyle_Solid),
    /** Line style */
    DASH(Messages.LineStyle_Dash),
    /** Line style */
    DOT(Messages.LineStyle_Dot),
    /** Line style */
    DASHDOT(Messages.LineStyle_DashDot),
    /** Line style */
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
