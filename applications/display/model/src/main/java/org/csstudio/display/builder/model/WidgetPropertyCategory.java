/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.Objects;

/** Category of a {@link WidgetProperty}.
 *
 *  Widget properties are grouped by category,
 *  ordered by the property's <code>ordinal()</code>.
 *
 *  @author Kay Kasemir
 */
public enum WidgetPropertyCategory
{
    /** Basic widget meta data: Name, primary PV, .. */
    WIDGET(Messages.WidgetPropertyCategory_Widget),
    /** Widget position: x, y, width, height, visible */
    POSITION(Messages.WidgetPropertyCategory_Position),
    /** What/how the widget displays */
    DISPLAY(Messages.WidgetPropertyCategory_Display),
    /** How the widget behaves */
    BEHAVIOR(Messages.WidgetPropertyCategory_Behavior),
    /** Widget properties that do not fit the other categories */
    MISC(Messages.WidgetPropertyCategory_Misc),
    /** Runtime data, not persisted */
    RUNTIME(Messages.WidgetPropertyCategory_Runtime);

    final private String description;

    private WidgetPropertyCategory(final String description)
    {
        this.description = Objects.requireNonNull(description);
    }

    /** @return Human-readable description. */
    public String getDescription()
    {
        return description;
    }

    /** @return Debug representation. */
    @Override
    public String toString()
    {
        return "Property Category " + name(); //$NON-NLS-1$
    }
}
