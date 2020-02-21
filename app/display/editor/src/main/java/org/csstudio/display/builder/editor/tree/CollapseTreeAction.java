/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.tree;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Menu entry for collapsing tree items
 *  @author Pavel Charvat
 */
@SuppressWarnings("nls")
public class CollapseTreeAction extends MenuItem
{
    /** @param tree WidgetTree to expand
     */
    public CollapseTreeAction(WidgetTree tree)
    {
        super(Messages.CollapseTree, ImageCache.getImageView(DisplayEditor.class, "/icons/tree_collapse.png"));
        setOnAction(event ->
        {
            tree.collapseAllTreeItems();
        });
    }
}
