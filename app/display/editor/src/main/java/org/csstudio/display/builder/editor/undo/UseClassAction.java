/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import org.csstudio.display.builder.model.WidgetProperty;
import org.phoebus.ui.undo.UndoableAction;

/** Action to update 'use_class' of property
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class UseClassAction extends UndoableAction
{
    private final WidgetProperty<?> property;
    private final boolean use_class;

    public UseClassAction(final WidgetProperty<?> widget_property, final boolean use_class)
    {
        super(widget_property.getName() + (use_class ? " - use widget class" : " - ignore widget class"));
        this.property = widget_property;
        this.use_class = use_class;
    }

    @Override
    public void run()
    {
        property.useWidgetClass(use_class);
    }

    @Override
    public void undo()
    {
        property.useWidgetClass(!use_class);
    }
}
