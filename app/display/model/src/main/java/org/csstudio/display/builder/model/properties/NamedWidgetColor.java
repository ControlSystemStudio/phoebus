/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;

/** Description of a named color
 *
 *  @see NamedWidgetColors
 *  @see WidgetColorService
 *  @author Kay Kasemir
 */
// Implementation avoids AWT, SWT, JavaFX color
public class NamedWidgetColor extends WidgetColor
{
    private final String name;

    /** Construct named color
     *  @param name Name of the color
     *  @param red Red component, range {@code 0-255}
     *  @param green Green component, range {@code 0-255}
     *  @param blue Blue component, range {@code 0-255}
     *  @param alpha Alpha component, range {@code 0} (transparent) to {@code 255} (opaque)
     */
    public NamedWidgetColor(final String name, final int red, final int green, final int blue, final int alpha)
    {
        super(red, green, blue, alpha);
        this.name = name;
    }

    /** Construct named color
     *  @param name Name of the color
     *  @param red Red component, range {@code 0-255}
     *  @param green Green component, range {@code 0-255}
     *  @param blue Blue component, range {@code 0-255}
     */
    public NamedWidgetColor(final String name, final int red, final int green, final int blue)
    {
        this(name, red, green, blue, 255);
    }

    /** @return Name */
    public String getName()
    {
        return name;
    }

    // In comparisons, the names must match.
    // Current RGB may actually differ!
    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        return (obj instanceof NamedWidgetColor)  &&
               name.equals(((NamedWidgetColor)obj).name);
    }

    @Override
    public String toString()
    {
        return name;
    }
}
