/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import java.util.List;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandWithBody;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/** {@link TreeView} for {@link ScanCommand}s
 *  @author Kay Kasemir
 */
public class ScanCommandTree extends TreeView<ScanCommand>
{
    private final Model model;
    final TreeItem<ScanCommand> root = new TreeItem<>(null);

    private final ModelListener listener = new ModelListener()
    {
        @Override
        public void commandsChanged()
        {
            // Convert scan into tree items
            addCommands(root, model.getCommands());

            // Expand complete tree
            expand(root);
        }

        @Override
        public void commandAdded(final ScanCommand command)
        {
            // TODO Optimize
            root.getChildren().clear();
            commandsChanged();
        }

        @Override
        public void commandRemoved(ScanCommand command)
        {
            // TODO Optimize
            root.getChildren().clear();
            commandsChanged();
        }
    };

    // TODO TextFieldTreeCell?

    ScanCommandTree(final Model model, final UndoableActionManager undo)
    {
        this.model = model;
        setShowRoot(false);
        setCellFactory(tree_view ->  new ScanCommandTreeCell(undo, model));
        setRoot(root);

        model.addListener(listener);
    }

    private static void addCommands(final TreeItem<ScanCommand> item,
                                    final List<ScanCommand> commands)
    {
        final List<TreeItem<ScanCommand>> children = item.getChildren();
        for (ScanCommand cmd : commands)
        {
            final TreeItem<ScanCommand> cmd_item = new TreeItem<>(cmd);
            children.add(cmd_item);
            if (cmd instanceof ScanCommandWithBody)
                addCommands(cmd_item, ((ScanCommandWithBody)cmd).getBody());
        }
    }

    private static void expand(final TreeItem<ScanCommand> item)
    {
        item.setExpanded(true);
        for (TreeItem<ScanCommand> sub : item.getChildren())
            expand(sub);
    }
}
