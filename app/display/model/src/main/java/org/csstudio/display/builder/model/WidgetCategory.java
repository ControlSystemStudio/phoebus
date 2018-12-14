/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.Objects;

/** Category of a {@link Widget}.
 *
 *  Allows grouping Widgets by category,
 *  ordered by the property's <code>ordinal()</code>.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public enum WidgetCategory
{
    /** Fundamental graphics: Rectangle, Circle, Label, ... */
    GRAPHIC(Messages.WidgetCategory_Graphics),
    /** Widgets that monitor a PV: Text Update, Meter, LED, ... */
    MONITOR(Messages.WidgetCategory_Monitors),
    /** Widgets that control a PV: Text Entry, Button, Slider, ... */
    CONTROL(Messages.WidgetCategory_Controls),
    /** Basically monitors, but more complex */
    PLOT(Messages.WidgetCategory_Plots),
    /** Widgets that structure the display: Group, Embedded Display */
    STRUCTURE(Messages.WidgetCategory_Structure),
    /** Widgets grouped as a site specific collection */
    EXTENSIONS(Messages.WidgetCategory_Extensions),
    // More categories?
    /** Widgets that do not fit the other categories */
    MISC(Messages.WidgetCategory_Miscellaneous);

    final private String description;
    
    private WidgetCategory(final String description)
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
        return "Widget Category " + name();
    }
}
