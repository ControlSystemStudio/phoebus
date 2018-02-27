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

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class ScanCommandTree extends TreeView<ScanCommand>
{
    final TreeItem<ScanCommand> root = new TreeItem<>(null);

    // TODO TextFieldTreeCell?

    ScanCommandTree()
    {
        setShowRoot(false);
        setCellFactory(tree_view ->  new ScanCommandTreeCell());
        setRoot(root);
    }

    /** @param commands Commands to show in the editor */
    public void setScan(final List<ScanCommand> commands)
    {
        // Convert scan into tree items
        CommandTreeUtil.addCommands(root, commands);

        // Expand complete tree
        expand(root);
    }

    private static void expand(final TreeItem<ScanCommand> item)
    {
        item.setExpanded(true);
        for (TreeItem<ScanCommand> sub : item.getChildren())
            expand(sub);
    }
}
