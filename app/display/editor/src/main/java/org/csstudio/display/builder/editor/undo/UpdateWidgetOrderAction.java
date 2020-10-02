/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.phoebus.ui.undo.UndoableAction;

/** Action to update widget order within parent's list of children
 *  @author Kay Kasemir
 */
public class UpdateWidgetOrderAction extends UndoableAction
{
    private final Widget widget;
    private final int orig_index, desired_index;

    /** @param widget Widget
     *  @param index Desired index, -1 for "end of list"
     */
    public UpdateWidgetOrderAction(final Widget widget,
                                   final int desired_index)
    {
        this(widget, getWidgetLocation(widget), desired_index);
    }

    /** @param widget Widget
     *  @param index Desired index, -1 for "end of list"
     */
    public UpdateWidgetOrderAction(final Widget widget,
                                   final int original_index,
                                   final int desired_index)
    {
        super(Messages.UpdateWidgetOrder);
        this.widget = widget;
        this.orig_index = original_index;
        this.desired_index = desired_index;
    }

    /** @param widget Widget
     *  @return Index of widget in it's parent list of children
     */
    public static int getWidgetLocation(final Widget widget)
    {
        final ChildrenProperty children = ChildrenProperty.getParentsChildren(widget);
        return children.getValue().indexOf(widget);
    }

    @Override
    public void run()
    {
        moveTo(desired_index);
    }

    @Override
    public void undo()
    {
        moveTo(orig_index);
    }

    private void moveTo(final int index)
    {
        final ChildrenProperty children = ChildrenProperty.getParentsChildren(widget);

        children.moveChildTo(index, widget);
    }
}
