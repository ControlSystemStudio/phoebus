/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTooltip;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propVisible;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropConnected;

import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;

/** Base class for all visible widgets.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VisibleWidget extends Widget
{
    private WidgetProperty<Boolean> visible;
    private WidgetProperty<String> tooltip;
    private WidgetProperty<Boolean> connected;

    /** Widget constructor.
     *  @param type Widget type
     */
    public VisibleWidget(final String type)
    {
        super(type);
    }

    /** Widget constructor.
     *  @param type Widget type
     *  @param default_width Default width
     *  @param default_height .. and height
     */
    public VisibleWidget(final String type, final int default_width, final int default_height)
    {
        super(type, default_width, default_height);
    }

    /** Called on construction to define widget's properties.
     *
     *  <p>Mandatory properties have already been defined.
     *  Derived class overrides to add its own properties.
     *
     *  @param properties List to which properties must be added
     */
    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(visible = propVisible.createProperty(this, true));
        properties.add(tooltip = propTooltip.createProperty(this, getInitialTooltip()));
        // Start 'connected', assuming there are no PVs
        properties.add(connected = runtimePropConnected.createProperty(this, true));
    }

    /** @return Default, initial tool tip text */
    protected String getInitialTooltip()
    {   // Plain Widgets tend to have no tool tip
        // (see PVWidget)
        return "";
    }

    /** @return Property 'visible' */
    public WidgetProperty<Boolean> propVisible()
    {
        return visible;
    }

    /** @return Property 'tooltip' */
    public WidgetProperty<String> propTooltip()
    {
        return tooltip;
    }

    /** @return Runtime 'connected' property */
    public WidgetProperty<Boolean> runtimePropConnected()
    {
        return connected;
    }
}
