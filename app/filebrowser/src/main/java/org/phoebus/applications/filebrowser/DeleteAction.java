/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.FileHelper;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;

/** Menu item to delete one file or directory
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DeleteAction extends MenuItem
{
    /** @param node Node used to position confirmation dialog
     *  @param item Item to delete
     */
    public DeleteAction(final Node node, final List<TreeItem<FileInfo>> items)
    {
        super(Messages.Delete, ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));

        setOnAction(event ->
        {
            final Alert prompt = new Alert(AlertType.CONFIRMATION);
            prompt.setTitle(Messages.DeletePromptTitle);

            // Files to delete
            final List<File> files = new ArrayList<>();
            // Listing of those files
            final StringBuilder buf = new StringBuilder();
            // Parent tree items to refresh
            final Set<FileTreeItem> parents = new HashSet<>();
            for (TreeItem<FileInfo> item : items)
            {
                files.add(item.getValue().file);
                if (buf.length() > 0)
                    buf.append(", ");
                buf.append(item.getValue().file.getName());
                parents.add((FileTreeItem)item.getParent());
            }

            prompt.setHeaderText(Messages.DeletePromptHeader + buf.toString() + "?");
            prompt.getDialogPane().setPrefWidth(500);
            prompt.setResizable(true);
            DialogHelper.positionDialog(prompt, node, 0, 0);
            if (prompt.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return;

            JobManager.schedule(Messages.DeleteJobName + buf.toString(), monitor ->
            {
                // Delete files. DirectoryMonitor might update tree items
                // as it detects file removal...
                for (File file : files)
                {
                    if (file.isFile())
                        file.delete();
                    else if (file.isDirectory())
                        FileHelper.delete(file);
                }
                // .. but to get faster response, force refresh of parent items
                for (FileTreeItem parent : parents)
                    Platform.runLater(() ->  parent.forceRefresh());
            });
        });
    }
}
