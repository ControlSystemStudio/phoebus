/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import java.util.List;
import java.util.stream.Collectors;

import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/** Menu item to copy path of file or directory to clipboard
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CopyPath extends MenuItem
{
    /** @param items Items which paths to copy to clipboard */
    public CopyPath(final List<TreeItem<FileInfo>> items)
    {
        super(Messages.CopyPathClp, ImageCache.getImageView(ImageCache.class, "/icons/copy.png"));

        setOnAction(event ->
        {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(items.stream().map(item -> item.getValue().file.getAbsolutePath()).collect(Collectors.joining(", ")));
            content.putFiles(items.stream().map(item -> item.getValue().file).collect(Collectors.toList()));
            clipboard.setContent(content);
        });
    }
}

