/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import java.util.List;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.phoebus.ui.undo.UndoableAction;

/** Action to group widgets
 *  @author Kay Kasemir
 */
public class GroupWidgetsAction extends UndoableAction
{
    // Performs the move of child widgets into a group
    // as one operation.
    //
    // Needs to be aware of how the Widget Tree (outline)
    // listens to the model's changes and performs them _later_,
    // on the UI thread.
    //
    // Adding the group first, then adding the widgets to the group
    // means that the outline view has _not_ added the group and
    // subscribed to its children, yet, so outline view will miss
    // the added widgets.
    // By first deleting the widgets, then adding the populated group,
    // the tree view will receive all the required information.
    private final ChildrenProperty parent_children;
    private final GroupWidget group;
    private final List<Widget> widgets;
    private final int x_offset, y_offset;

    public GroupWidgetsAction(final ChildrenProperty parent_children, final GroupWidget group,
                              final List<Widget> widgets,
                              final int x_offset, final int y_offset)
    {
        this(Messages.CreateGroup, parent_children, group, widgets,
             x_offset, y_offset);
    }

    public GroupWidgetsAction(final String name, final ChildrenProperty parent_children, final GroupWidget group,
            final List<Widget> widgets,
            final int x_offset, final int y_offset)
    {
        super(name);
        this.parent_children = parent_children;
        this.group = group;
        this.widgets = widgets;
        this.x_offset = x_offset;
        this.y_offset = y_offset;
    }

    @Override
    public void run()
    {
        for (Widget widget : widgets)
        {
            parent_children.removeChild(widget);
            final int orig_x = widget.propX().getValue();
            final int orig_y = widget.propY().getValue();
            widget.propX().setValue(orig_x - x_offset);
            widget.propY().setValue(orig_y - y_offset);
            group.runtimeChildren().addChild(widget);
        }
        parent_children.addChild(group);
    }

    @Override
    public void undo()
    {
        parent_children.removeChild(group);
        for (Widget widget : widgets)
        {
            group.runtimeChildren().removeChild(widget);
            final int orig_x = widget.propX().getValue();
            final int orig_y = widget.propY().getValue();
            widget.propX().setValue(orig_x + x_offset);
            widget.propY().setValue(orig_y + y_offset);
            parent_children.addChild(widget);
        }
    }
}
