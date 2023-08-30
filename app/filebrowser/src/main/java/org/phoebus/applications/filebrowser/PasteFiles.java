/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import java.io.File;
import java.util.List;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.FileHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.input.Clipboard;

/** Menu item to paste files from clipboard
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PasteFiles extends MenuItem
{
    /** @param Node Node for error dialog
     *  @param target_item Item (directory) into which files from clipboard should be copied
     */
    public PasteFiles(final Node node, final TreeItem<FileInfo> target_item)
    {
        super(Messages.Paste, ImageCache.getImageView(ImageCache.class, "/icons/paste.png"));

        setOnAction(event ->
        {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final List<File> files = clipboard.getFiles();
            final File directory = target_item.getValue().file;

            JobManager.schedule(getText(), monitor ->
            {
                try
                {
                    for (File original : files)
                    {
                        FileHelper.copy(original, directory);
                        final File new_file = new File(directory, original.getName());
                        Platform.runLater(() ->
                        {
                            final ObservableList<TreeItem<FileInfo>> siblings = target_item.getChildren();
                            siblings.add(new FileTreeItem(((FileTreeItem)target_item).getMonitor(), new_file));
                            FileTreeItem.sortSiblings(siblings);
                        });
                    }
                }
                catch (Exception ex)
                {
                    ExceptionDetailsErrorDialog.openError(node, Messages.Paste, "Failed to paste files", ex);
                }
            });
        });
    }
}
