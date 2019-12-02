/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.filebrowser;

import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;

/** Menu item to refresh a directory
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RefreshAction extends MenuItem
{
    /** @param node Node used to position confirmation dialog
     *  @param item Item to rename
     */
    public RefreshAction(final Node node, final TreeItem<FileInfo> item)
    {
        super(Messages.Refresh, ImageCache.getImageView(ImageCache.class, "/icons/refresh.png"));

        setOnAction(event ->
        {
            Platform.runLater(() ->    ((FileTreeItem)item).forceRefresh() );
        });
    }
}
