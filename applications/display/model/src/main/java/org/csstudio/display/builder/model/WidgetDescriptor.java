/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.net.URL;
import java.util.Collections;
import java.util.List;

/** Description of a widget.
 *
 *  <p>Describes a type of widget.
 *
 *  <p>In addition to the unique type ID, each widget
 *  has a human readable name (short) and description (longer).
 *  These may be localized and change over time, while the type ID
 *  remains fixed.
 *
 *  <p>The type ID identifies the current version of the widget.
 *  Alternate type IDs can be used in case this widget emulates the
 *  behavior of an older widget.
 *
 *  <p>Used by the {@link WidgetFactory} to list available widgets
 *  and to create {@link Widget} instances
 *
 *  @author Xihui Chen - Similar class in opibuilder
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public abstract class WidgetDescriptor
{
    final private String type;
    final private List<String> alternate_types;
    final private WidgetCategory category;
    final private String name;
    final private String icon;
    final private String description;

    /** @param type Type ID of the widget
     *  @param category Widget category
     *  @param name Human readable name
     *  @param icon Icon path
     *  @param description Longer description of the widget
     */
    public WidgetDescriptor(final String type,
            final WidgetCategory category,
            final String name,
            final String icon,
            final String description)
    {
        this(type, category, name, icon, description, Collections.emptyList());
    }

    /** @param type Type ID of the widget
     *  @param category Widget category
     *  @param name Human readable name
     *  @param icon Icon path
     *  @param description Longer description of the widget
     *  @param alternate_types Alternate type IDs
     */
    public WidgetDescriptor(final String type,
            final WidgetCategory category,
            final String name,
            final String icon,
            final String description,
            final List<String> alternate_types)
    {
        this.type = type;
        this.category = category;
        this.alternate_types = alternate_types;
        this.name = name;
        this.icon = icon;
        this.description = description;
    }

    /** @return Type ID, uniquely assigned to this widget */
    public String getType()
    {
        return type;
    }

    /** @return Widget category */
    public WidgetCategory getCategory()
    {
        return category;
    }

    /** Alternate IDs for this widget type.
     *
     *  <p>Widgets evolve.
     *  If this widget emulates the behavior of an older widget that has
     *  been removed, return those older IDs.
     *
     *  @return Alternate type IDs that should also match to this widget
     */
    public List<String> getAlternateTypes()
    {
        return alternate_types;
    }

    /** @return Human readable name */
    public String getName()
    {
        return name;
    }

    /** @return URL for icon
     *  @throws Exception on error
     */
    public URL getIconURL() throws Exception
    {
        return getClass().getResource(icon);
    }

    /** @return Description of the widget */
    public String getDescription()
    {
        return description;
    }

    /** Create a widget
     *  @return {@link Widget}
     */
    abstract public Widget createWidget();

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return "Widget type '" + type + "': " + name;
    }
}
