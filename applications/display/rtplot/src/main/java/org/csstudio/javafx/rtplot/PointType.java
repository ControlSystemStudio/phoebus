/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

/** How points of a {@link Trace} are drawn
 *  @author Kay Kasemir
 */
public enum PointType
{
    /** No line/area between points */
    NONE(Messages.Type_None),

    /** Square Markers */
    SQUARES(Messages.PointType_Squares),

    /** Circle Markers */
    CIRCLES(Messages.PointType_Circles),

    /** Diamond Markers */
    DIAMONDS(Messages.PointType_Diamonds),

    /** Cross Markers */
    XMARKS(Messages.PointType_XMarks),

    /** Triangle Markers */
    TRIANGLES(Messages.PointType_Triangles);

    final private String name;

    private PointType(final String name)
    {
        this.name = name;
    }

    /** Obtain trace type from ordinal
     *  @param ordinal
     *  @return TraceType
     *  @throws RuntimeException on invalid ordinal
     */
    public static PointType fromOrdinal(final int ordinal)
    {
        for (PointType type : PointType.values())
            if (type.ordinal() == ordinal)
                return type;
        throw new RuntimeException("Invalid Point Type " + ordinal); //$NON-NLS-1$
    }

    /** @return Array of display names for all trace types */
    public static String[] getDisplayNames()
    {
        final PointType types[] = PointType.values();
        final String names[] = new String[types.length];
        for (int i=0; i<names.length; ++i)
            names[i] = types[i].name;
        return names;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
