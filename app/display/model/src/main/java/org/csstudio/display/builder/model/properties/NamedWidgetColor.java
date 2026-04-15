/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * Copyright (c) 2026 Brookhaven National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

/** Legacy compatibility wrapper for {@link org.phoebus.ui.color.NamedWidgetColor}.
 *
 * @deprecated Use {@link org.phoebus.ui.color.NamedWidgetColor}.
 */
@Deprecated(since = "5.0.3")
public class NamedWidgetColor extends org.phoebus.ui.color.NamedWidgetColor
{
    /** Construct named RGB color.
     *  @param name  Name of the color
     *  @param red   Red component, range {@code 0-255}
     *  @param green Green component, range {@code 0-255}
     *  @param blue  Blue component, range {@code 0-255}
     */
    public NamedWidgetColor(final String name, final int red, final int green, final int blue)
    {
        super(name, red, green, blue);
    }

    /** Construct named RGBA color.
     *  @param name  Name of the color
     *  @param red   Red component, range {@code 0-255}
     *  @param green Green component, range {@code 0-255}
     *  @param blue  Blue component, range {@code 0-255}
     *  @param alpha Alpha component, range {@code 0} (transparent) to {@code 255} (opaque)
     */
    public NamedWidgetColor(final String name, final int red, final int green, final int blue, final int alpha)
    {
        super(name, red, green, blue, alpha);
    }
}

