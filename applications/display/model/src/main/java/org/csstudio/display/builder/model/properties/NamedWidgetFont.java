/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import java.util.Objects;

import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetFontService;

/** Description of a named font
 *
 *  @see NamedWidgetFonts
 *  @see WidgetFontService
 *  @author Kay Kasemir
 */
// Implementation avoids AWT, SWT, JavaFX color
public class NamedWidgetFont extends WidgetFont
{
    private final String name;

    /** Construct named font
     *  @param name Name of the color
     *  @param family Font family: "Liberation Sans"
     *  @param style  Font style: Bold, italic?
     *  @param size   Size (height)
     */
    public NamedWidgetFont(final String name, final String family, final WidgetFontStyle style, final double size)
    {
        super(family, style, size);
        this.name = Objects.requireNonNull(name);
    }

    /** @return Name */
    public String getName()
    {
        return name;
    }

    // In comparisons, the names must match.
    // Current settings (family, size, ..) may actually differ!
    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        return (obj instanceof NamedWidgetFont)  &&
               name.equals(((NamedWidgetFont)obj).name);
    }

    @Override
    public String toString()
    {
        return name;
    }
}
