/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import java.util.List;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeView;

/** Action that deletes item from the alarm tree configuration
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class RemoveComponentAction extends MenuItem
{
    /** @param node Node to position dialog
     *  @param model Model where components are to be removed
     *  @param items Items to remove from alarm tree
     */
    public RemoveComponentAction(final Node node, final AlarmClient model, final List<AlarmTreeItem<?>> items)
    {
        super("Remove Selected Items",
              ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
        setOnAction(event ->
        {
            final Alert dialog = new Alert(AlertType.CONFIRMATION);
            dialog.setTitle(getText());
            final StringBuilder buf = new StringBuilder();
            buf.append("Remove\n");
            int count = 0;
            for (AlarmTreeItem<?> item : items)
            {
                if (count > 0)
                    buf.append("\n");
                if (++count < 10)
                    buf.append("   ").append(item.getName());
                else
                {
                    buf.append("   ...").append(items.size() - count + 1).append(" more items");
                    break;
                }
            }
            buf.append("\nand all sub-entries?\nThere is no way to 'UNDO' this change.");
            dialog.setHeaderText(buf.toString());
            DialogHelper.positionDialog(dialog, node, -100, -50);
            if (dialog.showAndWait().get() != ButtonType.OK)
                return;

            // Tree view keeps the selection indices, which will point to wrong content
            // after those items have been removed
            if (node instanceof TreeView<?>)
                ((TreeView<?>) node).getSelectionModel().clearSelection();

            JobManager.schedule(getText(), monitor ->
            {
                for (AlarmTreeItem<?> item : items)
                    model.removeComponent(item);
            });
        });
    }
}
