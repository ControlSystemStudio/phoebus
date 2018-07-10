/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.io.File;

import org.csstudio.display.builder.runtime.Messages;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Screenshot;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Action for saving snapshot of display
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SendLogbookAction extends MenuItem
{
    // TODO Use some icon from logbook UI
    private static final Image icon = ImageCache.getImage(SendLogbookAction.class, "/icons/save_edit.png");

    public SendLogbookAction(final Parent model_parent)
    {
        super(Messages.SendToLogbook, new ImageView(icon));
        setOnAction(event -> save(model_parent));
    }

    private void save(final Parent model_parent)
    {
        // On UI thread, create screenshot
        final Screenshot screenshot = new Screenshot(model_parent);

        // Save to file in background thread
        JobManager.schedule(Messages.SendToLogbook, monitor ->
        {
            final File image_file = screenshot.writeToTempfile("display");

            // Create log entry via dialog on UI thread
            Platform.runLater(() ->  submitLogentry(model_parent, image_file));
        });
    }

    private void submitLogentry(final Parent model_parent, final File image_file)
    {
        // TODO Open Logbook entry dialog with image_file
        ExceptionDetailsErrorDialog.openError(model_parent, Messages.SendToLogbook, "Not implemented",
                new Exception("TODO: Create log entry for display screenshot"));

        JobManager.schedule(Messages.SendToLogbook, monitor ->
        {
            // TODO Create log entry,
            // then delete the file
            image_file.delete();
        });
    }
}
