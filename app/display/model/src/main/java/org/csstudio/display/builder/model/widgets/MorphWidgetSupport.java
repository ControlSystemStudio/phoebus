/*******************************************************************************
 * Copyright (c) 2020 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.RuntimeWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;

/** Class to morph properties between widgets
 *
 *  Intended as a support class to MorphWidgetsMenu
 *
 *  @author Krisztián Löki
 */
public class MorphWidgetSupport
{
    final private Method morpher;
    final private Widget old_widget;
    final private Widget new_widget;

    public MorphWidgetSupport(final Widget old_widget, final Widget new_widget)
    {
        this.old_widget = old_widget;
        this.new_widget = new_widget;

        Method method = null;

        try
        {
            method = getClass().getDeclaredMethod("doMorphProperty", WidgetProperty.class, old_widget.getClass(), new_widget.getClass());
        }
        catch (NoSuchMethodException ex)
        {
            // no specialization for widget pair
        }

        morpher = method;
    }

    private Optional<WidgetProperty<Object>> checkProperty(final WidgetProperty<?> prop)
    {
        final String propName = prop.getName();
        final Optional<WidgetProperty<Object>> check = new_widget.checkProperty(propName);
        if (check.isPresent())
        {
            if (propName.contentEquals("tooltip") && old_widget instanceof VisibleWidget && prop instanceof StringWidgetProperty &&
                ((VisibleWidget)old_widget).getInitialTooltip().contentEquals(((StringWidgetProperty)prop).getSpecification()))
                /*
                 * Do not copy the tooltip if it was not modified
                 * Prevents overwriting the default PVWidget tooltip with a default (empty) non-PVWidget tooltip and vice versa
                 */
                return Optional.ofNullable(null);
        }

        return check;
    }

    public Optional<WidgetProperty<Object>> morphProperty(final WidgetProperty<?> prop)
    {
        Optional<WidgetProperty<Object>> morphedProperty = checkProperty(prop);
        if (morphedProperty.isPresent())
            return morphedProperty;

        if (prop instanceof RuntimeWidgetProperty)
            return morphedProperty;

        try
        {
            if (morpher != null)
                return Optional.ofNullable((WidgetProperty<Object>)morpher.invoke(this, prop, old_widget, new_widget));
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot invoke morph method to morph '" + prop.getName() + "' property from " + old_widget + " to " + new_widget, ex);
        }

        return morphedProperty;
    }

    private WidgetProperty<?> doMorphProperty(final WidgetProperty<?> prop, final SymbolWidget old_widget, final PictureWidget new_widget)
    {
        if (prop.getName().contentEquals("symbols"))
        {
            return new_widget.getProperty("file");
        }

        return null;
    }

    private WidgetProperty<?> doMorphProperty(final WidgetProperty<?> prop, final PictureWidget old_widget, final SymbolWidget new_widget)
    {
        if (prop.getName().contentEquals("file"))
            return new_widget.getPropertyByPath("symbols[0]", true);

        return null;
    }

    private WidgetProperty<?> doMorphProperty(final WidgetProperty<?> prop, final LabelWidget old_widget, final TextSymbolWidget new_widget)
    {
        if (prop.getName().contentEquals("text"))
            return new_widget.getPropertyByPath("symbols[0]", true);

        return null;
    }

    private WidgetProperty<?> doMorphProperty(final WidgetProperty<?> prop, final TextSymbolWidget old_widget, final LabelWidget new_widget)
    {
        if (prop.getName().contentEquals("symbols"))
            return new_widget.getProperty("text");

        return null;
    }
}
