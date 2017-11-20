/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.io.File;

import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.csstudio.display.builder.runtime.Messages;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Screenshot;
import org.phoebus.ui.jobs.JobManager;

import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/** Action for saving snapshot of display
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SaveSnapshotAction extends MenuItem
{
    private static final Image icon = ImageCache.getImage(SaveSnapshotAction.class, "/icons/save_edit.png");
    private static final ExtensionFilter image_file_extension = new ExtensionFilter("Image (*.png)", "*.png");

    public SaveSnapshotAction(final Parent model_parent)
    {
        super(Messages.SaveSnapshot, new ImageView(icon));
        setOnAction(event -> save(model_parent));
    }

    private void save(final Parent model_parent)
    {
        final FileChooser dialog = new FileChooser();
        dialog.setTitle(Messages.SaveSnapshotSelectFilename);

        dialog.getExtensionFilters().setAll(FilenameSupport.file_extensions[0]);
        dialog.getExtensionFilters().add(image_file_extension);

        final File file = dialog.showSaveDialog(model_parent.getScene().getWindow());
        if (file == null)
            return;

        final Screenshot screenshot = new Screenshot(model_parent);
        JobManager.schedule(Messages.SaveSnapshot, monitor ->
        {
            monitor.beginTask(file.toString());
            try
            {
                screenshot.writeToFile(file);
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError("Screenshot error", "Cannot write screenshot", ex);
            }
        });
    }
}
