/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import java.io.File;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.DirectoryDeleter;
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
    public DeleteAction(final Node node, final TreeItem<File> item)
    {
        super("Delete", ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));

        setOnAction(event ->
        {
            final File file = item.getValue();
            final Alert prompt = new Alert(AlertType.CONFIRMATION);
            prompt.setTitle("Delete");
            prompt.setHeaderText("Delete " + file.getName() + "?");
            DialogHelper.positionDialog(prompt, node, 0, 0);
            if (prompt.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return;

            JobManager.schedule("Delete " + item.getValue(), monitor ->
            {
                if (file.isFile())
                    file.delete();
                else if (file.isDirectory())
                    DirectoryDeleter.delete(file);

                final FileTreeItem parent = (FileTreeItem)item.getParent();
                Platform.runLater(() ->  parent.forceRefresh());
            });
        });
    }
}
