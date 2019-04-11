/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

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

    /** @return Array of display names for all trace types */
    public static String[] getDisplayNames()
    {
        final LineStyle types[] = LineStyle.values();
        final String names[] = new String[types.length];
        for (int i=0; i<names.length; ++i)
            names[i] = types[i].label;
        return names;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
