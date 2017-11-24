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

/** Action to remove element from ArrayWidgetProperty
 *  @author Kay Kasemir
 */
public class RemoveArrayElementAction<WPE extends WidgetProperty<?>> extends UndoableAction
{
    private final ArrayWidgetProperty<WPE> property;
    private WPE element;

    public RemoveArrayElementAction(final ArrayWidgetProperty<WPE> property)
    {
        super(Messages.RemoveElement);
        this.property = property;
    }

    @Override
    public void run()
    {
        element = property.removeElement();
    }

    @Override
    public void undo()
    {
        property.addElement(element);
    }
}
