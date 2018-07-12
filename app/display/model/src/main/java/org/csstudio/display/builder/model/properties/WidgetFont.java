/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import java.util.Objects;

/** Description of a font
 *  @author Kay Kasemir
 */
// Implementation avoids AWT, SWT, JavaFX font
@SuppressWarnings("nls")
public class WidgetFont
{
    protected final String family;
    protected final WidgetFontStyle style;
    protected final double size;

    /** Construct font
     *
     *  @param family Font family: "Liberation Sans"
     *  @param style  Font style: Bold, italic?
     *  @param size   Size (height)
     */
    public WidgetFont(final String family, final WidgetFontStyle style, final double size)
    {
        this.family = Objects.requireNonNull(family);
        this.style = Objects.requireNonNull(style);
        this.size = size;
    }

    /** @return Font family: "Liberation Sans" */
    public String getFamily()
    {
        return family;
    }

    /** @return {@link WidgetFontStyle} */
    public WidgetFontStyle getStyle()
    {
        return style;
    }

    /** @return Size (height) */
    public double getSize()
    {
        return size;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = family.hashCode();
        long temp = Double.doubleToLongBits(size);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + style.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof WidgetFont))
            return false;
        final WidgetFont other = (WidgetFont) obj;
        return family.equals(other.family) &&
               size == other.size &&
               style == other.style;
    }

    @Override
    public String toString()
    {
        return "'" + family + "', " + style + ", " + size + ")";
    }
}
