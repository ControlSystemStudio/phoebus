/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.app;

import java.util.List;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.undo.GroupWidgetsAction;
import org.csstudio.display.builder.editor.util.GeometryTools;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget.Style;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PlatformInfo;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.MenuItem;

/** Action to create group
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CreateGroupAction extends MenuItem
{
    private final DisplayEditor editor;
    private final List<Widget> widgets;

    public CreateGroupAction(final DisplayEditor editor, final List<Widget> widgets)
    {
        super(Messages.CreateGroup + " [" + PlatformInfo.SHORTCUT + "-G]",
              ImageCache.getImageView(DisplayModel.class, "/icons/group.png"));
        this.editor = editor;
        this.widgets = widgets;
        setOnAction(event -> run());
    }

    public void run()
    {
        editor.getWidgetSelectionHandler().clear();

        // Create group that surrounds the original widget boundaries
        final GroupWidget group = new GroupWidget();

        // Get bounds of widgets relative to their container,
        // which might be a group within the display
        // or the display itself
        final Rectangle2D rect = GeometryTools.getBounds(widgets);

        // Inset depends on representation and changes with group style and font.
        // Can be obtained via group.runtimePropInsets() _after_ the group has
        // been represented. For this reason Style.NONE is used, where the inset
        // is always 0. An alternative could be Style.LINE, with an inset of 1.
        final int inset = 0;
        group.propStyle().setValue(Style.NONE);
        group.propTransparent().setValue(true);
        group.propX().setValue((int) rect.getMinX() - inset);
        group.propY().setValue((int) rect.getMinY() - inset);
        group.propWidth().setValue((int) rect.getWidth() + 2*inset);
        group.propHeight().setValue((int) rect.getHeight() + 2*inset);
        group.propName().setValue(org.csstudio.display.builder.model.Messages.GroupWidget_Name);

        final ChildrenProperty parent_children = ChildrenProperty.getParentsChildren(widgets.get(0));
        final UndoableActionManager undo = editor.getUndoableActionManager();
        undo.execute(new GroupWidgetsAction(parent_children, group, widgets, (int)rect.getMinX(), (int)rect.getMinY()));

        editor.getWidgetSelectionHandler().toggleSelection(group);
    }
}
