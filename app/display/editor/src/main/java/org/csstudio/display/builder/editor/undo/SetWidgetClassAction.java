/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.RuntimeWidgetProperty;
import org.csstudio.display.builder.model.WidgetClassSupport;
import org.csstudio.display.builder.model.WidgetClassSupport.PropertyValue;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.WidgetClassesService;
import org.csstudio.display.builder.model.properties.WidgetClassProperty;
import org.phoebus.ui.undo.UndoableAction;

/** Action to update widget class
 *  @author Kay Kasemir
 */
public class SetWidgetClassAction extends UndoableAction
{
    private final WidgetClassProperty widget_property;
    private final String orig_value, value;
    private final Map<String, WidgetClassSupport.PropertyValue> orig_prop_values = new HashMap<>();

    public SetWidgetClassAction(final WidgetClassProperty widget_property,
                                final String value)
    {
        super(MessageFormat.format(Messages.SetPropertyFmt, widget_property.getDescription()));
        this.widget_property = widget_property;
        this.orig_value = widget_property.getValue();
        this.value = value;
    }

    @Override
    public void run()
    {
        // Save the original value (or specification) for every property
        for (WidgetProperty<?> prop : widget_property.getWidget().getProperties())
            if (! (prop instanceof RuntimeWidgetProperty))
                orig_prop_values.put(prop.getName(), new WidgetClassSupport.PropertyValue(prop));
        setClass(value);
    }

    @Override
    public void undo()
    {
        // Restore original value (or specification) for every property
        for (WidgetProperty<?> prop : widget_property.getWidget().getProperties())
        {
            final PropertyValue orig = orig_prop_values.get(prop.getName());
            if (orig != null)
                orig.apply(prop);
        }
        setClass(orig_value);
    }

    private void setClass(final String widget_class)
    {
        widget_property.setValue(widget_class);
        WidgetClassesService.getWidgetClasses().apply(widget_property.getWidget());
    }
}
