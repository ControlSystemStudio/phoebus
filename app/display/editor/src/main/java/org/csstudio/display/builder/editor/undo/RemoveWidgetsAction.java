/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.undo;

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
    private final ChildrenProperty[] containers;
    private final Widget[] widgets;

    public RemoveWidgetsAction(final WidgetSelectionHandler selection, final List<Widget> widgets)
    {
        super(Messages.RemoveWidgets);
        this.selection = selection;
        final int N = widgets.size();
        this.containers = new ChildrenProperty[N];
        this.widgets = new Widget[N];
        for (int i=0; i<N; ++i)
        {
            this.widgets[i] = widgets.get(i);
            containers[i] = ChildrenProperty.getParentsChildren(this.widgets[i]);
        }
    }

    @Override
    public void run()
    {   // Remove in 'reverse', so that undo() will then
        // add them back in the matching order:
        // Add the one removed last, ..
        for (int i=widgets.length-1; i>=0; --i)
            containers[i].removeChild(widgets[i]);
        selection.clear();
    }

    @Override
    public void undo()
    {
        for (int i=0; i<widgets.length; ++i)
            containers[i].addChild(widgets[i]);
        selection.setSelection(Arrays.asList(widgets));
    }
}
