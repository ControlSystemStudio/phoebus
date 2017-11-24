/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import java.text.MessageFormat;
import java.util.logging.Level;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.phoebus.ui.undo.UndoableAction;

/** Action to update widget property
 *  @author Kay Kasemir
 *  @param <T> Type of the property's value
 */
@SuppressWarnings("nls")
public class SetWidgetEnumPropertyAction extends UndoableAction
{
    private final EnumWidgetProperty<?> widget_property;
    private final String orig_value;
    private final Object value;

    /** @param widget_property {@link EnumWidgetProperty} to set
     *  @param value Integer ordinal or String specification
     */
    public SetWidgetEnumPropertyAction(final EnumWidgetProperty<?> widget_property,
                                       final Object value)
    {
        super(MessageFormat.format(Messages.SetPropertyFmt, widget_property.getDescription()));
        this.widget_property = widget_property;
        this.orig_value = widget_property.getSpecification();
        this.value = value;
    }

    @Override
    public void run()
    {
        if (value instanceof String)
            widget_property.setSpecification((String) value);
        else
            try
            {
                widget_property.setValueFromObject(value);
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, "Cannot set " + widget_property.getName() + " to " + value, ex);
            }
    }

    @Override
    public void undo()
    {
        widget_property.setSpecification(orig_value);
    }
}
