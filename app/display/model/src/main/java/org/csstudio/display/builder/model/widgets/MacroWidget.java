/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMacros;

import java.util.List;

import org.csstudio.display.builder.model.WidgetProperty;
import org.phoebus.framework.macros.Macros;

/** Base for widget with 'macros'
 *  @author Kay Kasemir
 */
public class MacroWidget extends VisibleWidget
{
    private volatile WidgetProperty<Macros> macros;

    /** Widget constructor.
     *  @param type Widget type
     */
    public MacroWidget(final String type)
    {
        super(type);
    }

    /** Widget constructor.
     *  @param type Widget type
     *  @param default_width Default width
     *  @param default_height .. and height
     */
    public MacroWidget(final String type, final int default_width, final int default_height)
    {
        super(type, default_width, default_height);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(macros = propMacros.createProperty(this, new Macros()));
    }

    /** @return 'macros' property */
    public WidgetProperty<Macros> propMacros()
    {
        return macros;
    }

    /** Widget extends parent macros
     *  @return {@link Macros}
     */
    @Override
    public Macros getEffectiveMacros()
    {
        final Macros base = super.getEffectiveMacros();
        final Macros my_macros = propMacros().getValue();
        return Macros.merge(base, my_macros);
    }
}
