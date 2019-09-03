/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.WidgetSelectionHandler;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.phoebus.ui.undo.UndoableAction;

/** Action to remove widgets
 *  @author Kay Kasemir
 */
public class RemoveWidgetsAction extends UndoableAction
{
    private final WidgetSelectionHandler selection;

    private static class Info
    {
        final Widget widget;
        final ChildrenProperty container;
        final int index;

        Info(final Widget widget)
        {
            this.widget = widget;
            container = ChildrenProperty.getParentsChildren(widget);
            index = container.getValue().indexOf(widget);
        }
    };

    private final Info[] info;

    public RemoveWidgetsAction(final WidgetSelectionHandler selection, final List<Widget> widgets)
    {
        super(Messages.RemoveWidgets);
        this.selection = selection;
        final int N = widgets.size();
        info = new Info[N];
        for (int i=0; i<N; ++i)
            info[i] = new Info(widgets.get(i));

        // Sort by index.
        // For removal, the order doesn't matter, but for addition, it does.
        // When adding widgets, the one at the lowest index needs to be added first, then the next by index.
        // Otherwise we'd try to add at an index that's not valid, yet, until the other widgets have been added.
        Arrays.sort(info, (a, b) -> Integer.compare(a.index, b.index));
    }

    @Override
    public void run()
    {
        for (Info i : info)
            i.container.removeChild(i.widget);
        selection.clear();
    }

    @Override
    public void undo()
    {
        final List<Widget> sel = new ArrayList<>();
        for (Info i : info)
        {
            i.container.addChild(i.index, i.widget);
            sel.add(i.widget);
        }
        selection.setSelection(sel);
    }
}
