/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.WidgetProperty;
import org.phoebus.ui.undo.UndoableAction;

/** Action to add element to ArrayWidgetProperty
 *  @author Kay Kasemir
 */
public class AddArrayElementAction<WPE extends WidgetProperty<?>> extends UndoableAction
{
    private final ArrayWidgetProperty<WPE> property;
    private final WPE element;

    public AddArrayElementAction(final ArrayWidgetProperty<WPE> property, final WPE element)
    {
        super(Messages.AddElement);
        this.property = property;
        this.element = element;
    }

    @Override
    public void run()
    {
        property.addElement(element);
    }

    @Override
    public void undo()
    {
        property.removeElement();
    }
}
