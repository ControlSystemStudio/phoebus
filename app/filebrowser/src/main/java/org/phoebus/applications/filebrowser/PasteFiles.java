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
import java.util.List;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.input.Clipboard;

/** Menu item to paste files from clipboard
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PasteFiles extends MenuItem
{
    /** @param target_item Item (directory) into which files from clipboard should be copied */
    public PasteFiles(TreeItem<File> target_item)
    {
        super(Messages.Paste, ImageCache.getImageView(ImageCache.class, "/icons/paste.png"));

        setOnAction(event ->
        {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final List<File> files = clipboard.getFiles();
            final File directory = target_item.getValue();

            JobManager.schedule(getText(), monitor ->
            {
                for (File original : files)
                {
                    if (original.isDirectory())
                        throw new Exception(Messages.PasteAlert1 + original + " " + Messages.PasteAlert2 + directory);
                    final File new_file = new File(directory, original.getName());
                    if (new_file.exists())
                        throw new Exception(Messages.PasteAlert3  + original + " " + Messages.PasteAlert2 + new_file + " " + Messages.PasteAlert4);

                    Files.copy(original.toPath(), new FileOutputStream(new_file));
                    Platform.runLater(() ->
                    {
                        final ObservableList<TreeItem<File>> siblings = target_item.getChildren();
                        siblings.add(new FileTreeItem(((FileTreeItem)target_item).getMonitor(), new_file));
                        FileTreeItem.sortSiblings(siblings);
                    });
                }
            });
        });
    }
}
