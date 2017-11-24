/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import java.text.MessageFormat;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.phoebus.ui.undo.UndoableAction;

/** Action to update widget property
 *  @author Kay Kasemir
 */
public class SetMacroizedWidgetPropertyAction extends UndoableAction
{
    private final MacroizedWidgetProperty<?> widget_property;
    private final String orig_text;
    private final String text;

    public SetMacroizedWidgetPropertyAction(final MacroizedWidgetProperty<?> widget_property,
                                            final String text)
    {
        super(MessageFormat.format(Messages.SetPropertyFmt, widget_property.getDescription()));
        this.widget_property = widget_property;
        this.orig_text = widget_property.getSpecification();
        this.text = text;
    }

    @Override
    public void run()
    {
        widget_property.setSpecification(text);
    }

    @Override
    public void undo()
    {
        widget_property.setSpecification(orig_text);
    }
}
