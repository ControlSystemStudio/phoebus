/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.macros;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.framework.macros.Macros;

/** Provides values from macros, falling back to widget properties
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacroOrPropertyProvider implements MacroValueProvider
{
    private final Widget widget;

    /** Initialize
     *  @param widget      Widget for which to provide macros or properties
     */
    public MacroOrPropertyProvider(final Widget widget)
    {
        this.widget = widget;
    }

    @Override
    public String getValue(final String name)
    {
        // Automatic macro for Display ID,
        // uniquely identifies the display
        if ("DID".equals(name))
        {
            // Have properties of a widget, need display model.
            // Use the 'top' display.
            try
            {
                return widget.getTopDisplayModel().getID();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot obtain display ID for $(DID)", ex);
                return "DP00";
            }
        }

        // Automatic macro for Display NAME
        if ("DNAME".equals(name))
        {
            try
            {
                return widget.getTopDisplayModel().getName();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot obtain display name", ex);
                return "Unknown";
            }
        }

        // Check actual macros
        final Macros macros = widget.getEffectiveMacros();
        if (macros != null)
        {
            final String value = macros.getValue(name);
            if (value != null)
                return value;
        }

        // Fall back to widget properties
        try
        {
            final WidgetProperty<?> property = widget.getProperty(name);
            if (property != null)
            {   // If value is a single-element collection, get string for that one element.
                // This is primarily for buttons that use $(actions) as their text,
                // and there's a single action which should show as "That Action"
                // and not "[That Action]".
                final Object prop_val = property.getValue();
                if (prop_val instanceof Collection<?>)
                {
                    final Collection<?> coll = (Collection<?>) prop_val;
                    if (coll.size() == 1)
                        return coll.iterator().next().toString();
                }
                // Value of property may be null, example: Initial pv_value
                return Objects.toString(prop_val);
            }
        }
        catch (IllegalArgumentException ex)
        {
            // Ignore unknown macro
        }

        // Fall back to Java system properties
        final String value = System.getProperty(name);
        if (value != null)
            return value;

        // Finally, fall back to environment variables
        return System.getenv(name);
    }

    @Override
    public String toString()
    {
        return widget + " macros or properties: " + widget.getEffectiveMacros();
    }
}
