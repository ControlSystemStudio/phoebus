/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.logging.Level;

import org.csstudio.display.builder.runtime.Messages;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.ui.write.LogEntryDialog;
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
            Platform.runLater(() ->  submitLogEntry(model_parent, image_file));
        });
    }

    private void submitLogEntry(final Parent model_parent, final File image_file)
    {
        Attachment attachment = null;
        try
        {
            // TODO Somehow set the attachment content type indicating this is an image.
            attachment = AttachmentImpl.of(image_file);
        } catch (FileNotFoundException ex)
        {
            logger.log(Level.WARNING, "Default log entry attachment creation failed.", ex);
        }
        
        LogEntryBuilder logEntryBuilder = new LogEntryBuilder();
        LogEntry template = logEntryBuilder.appendDescription("Log entry from " + getText())
                       .attach(attachment)
                       .createdDate(Instant.now())
                       .build();
        
        LogEntryDialog logEntryDialog = new LogEntryDialog(model_parent, template);
        // Set the on submit action to clean up the temporary file after log entry submission.
        logEntryDialog.setOnSubmitAction(() -> image_file.delete());
        logEntryDialog.showAndWait();
    }
}
