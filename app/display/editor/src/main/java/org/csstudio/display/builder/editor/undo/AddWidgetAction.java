/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import java.util.Arrays;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.WidgetSelectionHandler;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.phoebus.ui.undo.UndoableAction;

/** Action to add widget
 *  @author Kay Kasemir
 */
public class AddWidgetAction extends UndoableAction
{
    private final WidgetSelectionHandler selection;
    private final ChildrenProperty children;
    private final Widget widget;
    private final int index;

    public AddWidgetAction(final WidgetSelectionHandler selection, final ChildrenProperty children, final Widget widget)
    {
        this(selection, children, widget, -1);
    }

    public AddWidgetAction(final WidgetSelectionHandler selection, final ChildrenProperty children, final Widget widget, final int index)
    {
        super(Messages.AddWidget);
        this.selection = selection;
        this.children = children;
        this.widget = widget;
        this.index = index;
    }

    @Override
    public void run()
    {
        children.addChild(index, widget);
        selection.setSelection(Arrays.asList(widget));
    }

    @Override
    public void undo()
    {
        selection.clear();
        children.removeChild(widget);
    }
}
