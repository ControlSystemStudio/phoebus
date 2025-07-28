/*******************************************************************************
 * Copyright (c) 2015-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import java.io.File;

import javafx.scene.control.Menu;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Screenshot;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

/** Action for saving snapshot of display
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SaveSnapshotAction extends Menu
{
    private static final Image icon = ImageCache.getImage(SaveSnapshotAction.class, "/icons/save_edit.png");
    private static final Image copyIcon = ImageCache.getImage(SaveSnapshotAction.class, "/icons/copy.png");
    private static final ExtensionFilter all_file_extensions = new ExtensionFilter(Messages.AllFiles, "*.*");
    private static final ExtensionFilter image_file_extension = new ExtensionFilter(Messages.ImagePng, "*.png");


    /** @param node Node in scene of which to take snapshot */
    public SaveSnapshotAction(final Node node)
    {
        setText(Messages.SaveSnapshot);
        setGraphic(new ImageView(icon));

        MenuItem saveToFileMenuItem = new MenuItem(Messages.SaveSnapshotToFile, new ImageView(icon));
        saveToFileMenuItem.setOnAction(e -> save(node));

        MenuItem copyToClipboard = new MenuItem(Messages.SaveSnapshotToClipboard, new ImageView(copyIcon));
        copyToClipboard.setOnAction(e -> Screenshot.copyToClipboard(node));

        getItems().addAll(saveToFileMenuItem, copyToClipboard);

    }

    /** @param node Node of which to save a snapshot */
    public static void save(final Node node)
    {
        final Window window = node.getScene().getWindow();
        final ExtensionFilter[] file_extensions = new ExtensionFilter[]
        {
            all_file_extensions,
            image_file_extension
        };

        final File file = new SaveAsDialog().promptForFile(window, Messages.SaveSnapshotSelectFilename,
                                                           null, file_extensions);
        if (file == null)
            return;

        final Screenshot screenshot = new Screenshot(node);
        JobManager.schedule(Messages.SaveSnapshot, monitor ->
        {
            monitor.beginTask(file.toString());
            try
            {
                screenshot.writeToFile(file);
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError(Messages.ScreenshotErrHdr, Messages.ScreenshotErrMsg, ex);
            }
        });
    }
}
