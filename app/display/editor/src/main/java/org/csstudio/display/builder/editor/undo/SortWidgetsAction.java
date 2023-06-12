/*******************************************************************************
 * Copyright (c) 2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;
import org.phoebus.ui.undo.UndoableAction;

/** Action to sort child widgets of display or group
 *  @author Kay Kasemir
 */
public class SortWidgetsAction extends UndoableAction
{
    /** @param editor {@link DisplayEditor}
     *  @return {@link Widget} for model or selected group, or <code>null</code>
     */
    public static Widget getWidgetToSort(final DisplayEditor editor)
    {
        final List<Widget> selection = editor.getWidgetSelectionHandler().getSelection();
        if (selection.isEmpty())
            return editor.getModel();
        if (selection.size() == 1  &&  selection.get(0) instanceof GroupWidget)
            return selection.get(0);
        return null;
    }

    /** Children of model or a selected group */
    private final ChildrenProperty children;

    /** Original widget order for 'children' and sub-widgets */
    private final List<List<Widget>> originals = new LinkedList<>();

    /** @param editor {@link DisplayEditor} */
    public SortWidgetsAction(final DisplayEditor editor)
    {
        super(Messages.SortWidgets);
        final Widget widget = getWidgetToSort(editor);
        children = ChildrenProperty.getChildren(widget);
    }

    @Override
    public void run()
    {
        originals.clear();
        sort(children);
    }

    private void sort(final ChildrenProperty children)
    {
        // Get original order
        final List<Widget> original = new ArrayList<>(children.getValue());
        originals.add(original);

        // Recurse..
        for (Widget child : original)
            if (child instanceof TabsWidget)
                for (TabItemProperty tab : ((TabsWidget)child).propTabs().getValue())
                    sort(tab.children());
            else
            {
                final ChildrenProperty sub = ChildrenProperty.getChildren(child);
                if (sub != null)
                    sort(sub);
            }

        // .. then sort on this level
        final List<Widget> sorted = new ArrayList<>(original);
        sorted.sort((a, b) ->
        {
            // Sort top..bottom, fall back to left..right and finally sort by name
            int order = a.propY().getValue() - b.propY().getValue();
            if (order == 0)
                order = a.propX().getValue() - b.propX().getValue();
            if (order == 0)
                order = a.getName().compareTo(b.getName());
            return order;
        });

        // Delete from end of list, if only to avoid 'moving' remaining items
        for (int i=original.size()-1; i>=0; --i)
            children.removeChild(original.get(i));
        for (Widget w : sorted)
            children.addChild(w);
    }

    @Override
    public void undo()
    {
        restore(children);
    }

    private void restore(final ChildrenProperty children)
    {
        // Restore order for this item ...
        final List<Widget> original = originals.remove(0);
        final List<Widget> sorted = new ArrayList<>(children.getValue());
        for (int i=sorted.size()-1; i>=0; --i)
            children.removeChild(sorted.get(i));
        for (Widget w : original)
            children.addChild(w);

        // .. then recurse
        for (Widget child : original)
            if (child instanceof TabsWidget)
                for (TabItemProperty tab : ((TabsWidget)child).propTabs().getValue())
                    restore(tab.children());
            else
            {
                final ChildrenProperty sub = ChildrenProperty.getChildren(child);
                if (sub != null)
                    restore(sub);
            }
    }
}
