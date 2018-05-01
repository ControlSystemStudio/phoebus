/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeView;

/** Action to rename a PV
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class RenameTreeItemAction extends MenuItem
{
    /** @param node UI node used to position dialog
     *  @param model {@link AlarmClient}
     *  @param pv PV to rename
     */
    public RenameTreeItemAction(final TreeView<AlarmTreeItem<?>> node,
                          final AlarmClient model,
                          final AlarmTreeItem<?> item)
    {
        super("Rename Item", ImageCache.getImageView(AlarmSystem.class, "/icons/rename.png"));

        // TODO Should be able to rename any item, not just a leaf,
        //      by renaming that item and all its child entries
        if (! (item instanceof AlarmClientLeaf))
            throw new IllegalArgumentException("Can for now only rename PVs, not arbitrary alarm tree items");

        setOnAction(event ->
        {
            // Prompt for new name
            final TextInputDialog prompt = new TextInputDialog(item.getName());
            DialogHelper.positionDialog(prompt, node, -200, -100);
            prompt.setTitle(getText());
            prompt.setHeaderText("Enter new name for PV");
            final String new_name = prompt.showAndWait().orElse(null);
            if (new_name == null)
                return;

            // Tree view keeps the selection indices, which will point to wrong content
            // after those items have been removed
            if (node instanceof TreeView<?>)
                ((TreeView<?>) node).getSelectionModel().clearSelection();

            // Delete old, add new name in background thread
            JobManager.schedule(getText(), monitor ->
            {
                final AlarmTreeItem<BasicState> parent = item.getParent();
                model.removeComponent(item);
                model.addPV(parent, new_name);
            });
        });
    }
}
