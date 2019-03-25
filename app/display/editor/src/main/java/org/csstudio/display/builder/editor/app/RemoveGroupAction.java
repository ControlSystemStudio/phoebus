/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.app;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.undo.UnGroupWidgetsAction;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PlatformInfo;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.MenuItem;

/** Action to remove a group
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RemoveGroupAction extends MenuItem
{
    private final DisplayEditor editor;
    private final GroupWidget group;

    public RemoveGroupAction(final DisplayEditor editor, final GroupWidget group)
    {
        super(Messages.RemoveGroup + " [" + PlatformInfo.SHORTCUT + "-U]",
              ImageCache.getImageView(DisplayModel.class, "/icons/group.png"));
        this.editor = editor;
        this.group = group;
        setOnAction(event -> run());
    }

    public void run()
    {
        editor.getWidgetSelectionHandler().clear();
        // Group's children list will be empty, create copy
        final List<Widget> widgets = new ArrayList<>(group.runtimeChildren().getValue());

        final UndoableActionManager undo = editor.getUndoableActionManager();
        undo.execute(new UnGroupWidgetsAction(group));

        editor.getWidgetSelectionHandler().setSelection(widgets);
    }
}
