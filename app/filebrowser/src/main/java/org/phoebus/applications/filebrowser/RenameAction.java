/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;

/** Menu item to rename one file or directory
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RenameAction extends MenuItem
{
    /** @param node Node used to position confirmation dialog
     *  @param item Item to rename
     */
    public RenameAction(final Node node, final TreeItem<FileInfo> item)
    {
        super(Messages.Rename, ImageCache.getImageView(ImageCache.class, "/icons/name.png"));

        setOnAction(event ->
        {
            final File file = item.getValue().file;
            final TextInputDialog prompt = new TextInputDialog(file.getName());
            prompt.setTitle(getText());
            prompt.setHeaderText(Messages.RenameHdr);
            DialogHelper.positionDialog(prompt, node, 0, 0);
            final String new_name = prompt.showAndWait().orElse(null);
            if (new_name == null)
                return;

            JobManager.schedule(Messages.RenameJobName + item.getValue(), monitor ->
            {
                Files.move(file.toPath(), (new File(file.getParentFile(), new_name)).toPath(), StandardCopyOption.REPLACE_EXISTING);

                final FileTreeItem parent = (FileTreeItem)item.getParent();
                Platform.runLater(() ->  parent.forceRefresh());
            });
        });
    }
}
