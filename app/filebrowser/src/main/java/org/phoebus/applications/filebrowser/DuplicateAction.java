/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;

/** Menu item to duplicate a file
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DuplicateAction extends MenuItem
{
    /** @param node Node used to position confirmation dialog
     *  @param item Item to duplicate
     */
    public DuplicateAction(final Node node, final TreeItem<FileInfo> item)
    {
        super(Messages.Duplicate, ImageCache.getImageView(ImageCache.class, "/icons/copy.png"));

        setOnAction(event ->
        {
            final File file = item.getValue().file;
            final TextInputDialog prompt = new TextInputDialog(Messages.DuplicatePrefix + file.getName());
            prompt.setTitle(getText());
            prompt.setHeaderText(Messages.DuplicatePromptHeader);
            DialogHelper.positionDialog(prompt, node, 0, 0);
            final String new_name = prompt.showAndWait().orElse(null);
            if (new_name == null)
                return;

            JobManager.schedule(Messages.DuplicateJobName + item.getValue(), monitor ->
            {
                final File new_file = new File(file.getParentFile(), new_name);

                if (new_file.exists())
                {
                    Platform.runLater(() ->
                    {
                        final Alert dialog = new Alert(AlertType.ERROR);
                        dialog.setTitle(getText());
                        dialog.setHeaderText(Messages.DuplicateAlert1 + new_file + " " + Messages.DuplicateAlert2);
                        DialogHelper.positionDialog(dialog, node, 0, 0);
                        dialog.showAndWait();
                    });
                    return;
                }

                final FileOutputStream out = new FileOutputStream(new_file);
                Files.copy(file.toPath(), out);
                out.flush();
                out.close();

                final FileTreeItem fte = (FileTreeItem)item;
                final FileTreeItem parent = (FileTreeItem)fte.getParent();
                Platform.runLater(() ->
                {
                    parent.getChildren().add(new FileTreeItem(fte.getMonitor(), new_file));
                    FileTreeItem.sortSiblings(parent.getChildren());
                });
            });
        });
    }
}
