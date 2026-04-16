/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

/** Legacy compatibility wrapper for {@link org.phoebus.ui.color.WidgetColor}.
 *
 * @deprecated Use {@link org.phoebus.ui.color.WidgetColor}.
 */
@Deprecated(since = "5.0.3")
public class WidgetColor extends org.phoebus.ui.color.WidgetColor
{
    /** Construct RGB color.
     *  @param red Red component, range {@code 0-255}
     *  @param green Green component, range {@code 0-255}
     *  @param blue Blue component, range {@code 0-255}
     */
    public WidgetColor(final int red, final int green, final int blue)
    {
        super(red, green, blue);
        logger.warning("Deprecated wrapper in use: org.csstudio.display.builder.model.properties.WidgetColor. " +
                "Use org.phoebus.ui.color.WidgetColor.");
    }

    /** Construct RGBA color.
     *  @param red Red component, range {@code 0-255}
     *  @param green Green component, range {@code 0-255}
     *  @param blue Blue component, range {@code 0-255}
     *  @param alpha Alpha component, range {@code 0} (transparent) to {@code 255} (opaque)
     */
    public WidgetColor(final int red, final int green, final int blue, final int alpha)
    {
        super(red, green, blue, alpha);
        logger.warning("Deprecated wrapper in use: org.csstudio.display.builder.model.properties.WidgetColor. " +
                "Use org.phoebus.ui.color.WidgetColor.");
    }
}



