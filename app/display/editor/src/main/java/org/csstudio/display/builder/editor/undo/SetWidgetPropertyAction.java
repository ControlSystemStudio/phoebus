/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import java.text.MessageFormat;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.WidgetProperty;
import org.phoebus.ui.undo.UndoableAction;

/** Action to update widget property
 *  @author Kay Kasemir
 *  @param <T> Type of the property's value
 */
public class SetWidgetPropertyAction<T extends Object> extends UndoableAction
{
    private final WidgetProperty<T> widget_property;
    private final T orig_value, value;

    public SetWidgetPropertyAction(final WidgetProperty<T> widget_property,
                                   final T value)
    {
        this(widget_property, widget_property.getValue(), value);
    }

    public SetWidgetPropertyAction(final WidgetProperty<T> widget_property,
                                   final T orig_value, final T value)
    {
        super(MessageFormat.format(Messages.SetPropertyFmt, widget_property.getDescription()));
        this.widget_property = widget_property;
        this.orig_value = orig_value;
        this.value = value;
    }

    @Override
    public void run()
    {
        widget_property.setValue(value);
    }

    @Override
    public void undo()
    {
        widget_property.setValue(orig_value);
    }
}
