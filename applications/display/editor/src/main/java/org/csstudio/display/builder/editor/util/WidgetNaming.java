/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetFactory;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;

/** Helper for naming widgets
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetNaming
{
    /** Widget name "SomeName_123" */
    private static final Pattern pattern = Pattern.compile("(.*)_(\\d+)");

    /** Maximum known instance number for widget base names */
    // Cache of instance names allows starting with the highest used
    // "TextUpdate_131" instead of testing "TextUpdate_1", TextUpdate_2", ..
    // It also 'remembers' to some extend:
    // After adding a "TextUpdate_2", then _deleting_ that widget, the next
    // one added will be "TextUpdate_3", even though ".._2" is now unused
    // -> Consider that a feature
    private final Map<String, Integer> max_used_instance = new HashMap<>();

    /** Clear information about widget names */
    public void clear()
    {
        synchronized (max_used_instance)
        {
            max_used_instance.clear();
        }
    }

    /** Set widget's name
     *
     *  <p>Widget that already has unique name remains unchanged.
     *  Otherwise a "_123" instance index is added, using the next unused index.
     *  Name defaults to the widget type.
     *
     *  @param model  Model that will contain the widget (but doesn't, yet)
     *  @param widget Widget to name
     */
    public void setDefaultName(final DisplayModel model, final Widget widget)
    {
        // Check that widget is not already in the model, because otherwise
        // a name lookup would find the widget itself and consider the name
        // as already "in use".
        DisplayModel widget_model;
        try
        {
            widget_model = widget.getDisplayModel();
        }
        catch (Exception ex)
        {   // OK to not be in any model
            widget_model = null;
        }
        if (widget_model == model)
            throw new IllegalStateException(widget + " already in model " + model);

        String name = widget.getName();

        // Default to human-readable widget type
        if (name.isEmpty())
            name = WidgetFactory.getInstance().getWidgetDescriptor(widget.getType()).getName();

        // Does the name match "SomeName_14"?
        final Matcher matcher = pattern.matcher(name);

        final String base;
        int number;
        if (matcher.matches())
        {
            base = matcher.group(1);
            number = Integer.parseInt(matcher.group(2));
        }
        else
        {
            base = name;
            number = 0;
        }

        // Check for the next unused instance of this widget name
        synchronized (max_used_instance)
        {
            final Integer used = max_used_instance.get(base);
            if (used != null)
            {
                number = Math.max(number,  used + 1);
                name = base + "_" + number;
            }
            // Locate next available "SomeName_14"
            while (model.runtimeChildren().getChildByName(name) != null)
            {
                ++number;
                name = base + "_" + number;
            }
            max_used_instance.put(base, number);
        }
        widget.setPropertyValue(CommonWidgetProperties.propName, name);

        final ChildrenProperty children = ChildrenProperty.getChildren(widget);
        if (children != null)
        {
            for (Widget child : children.getValue())
                setDefaultName(model, child);
        }
    }
}
