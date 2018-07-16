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
    private String default_text;
    private Runnable default_runnable;
    
    public SendLogbookAction(final Parent model_parent)
    {
        super(Messages.SendToLogbook, new ImageView(icon));
        setOnAction(event -> save(model_parent));
        default_text = "Log Entry from " + getText();
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
        if (null != default_runnable)
            default_runnable.run();
        
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
        LogEntry template = logEntryBuilder.appendDescription(default_text)
                       .attach(attachment)
                       .createdDate(Instant.now())
                       .build();
        
        LogEntryDialog logEntryDialog = new LogEntryDialog(model_parent, template);
        // Set the on submit action to clean up the temporary file after log entry submission.
        logEntryDialog.setOnSubmitAction(() -> image_file.delete());
        logEntryDialog.showAndWait();
    }
    
    /**  Set the default text for log book entry. */
    public void setDefaultText(String text)
    {
        default_text = text;
    }
    
    /** 
     * The idea here is to call setDefaultText from inside this runnable. 
     * The code to generate any default text based on application state can go in here.
     * onAction is used internally so setting that will prevent the dialog from being opened on an action event.
     * <p> So long as it is not null, the runnable will be called before the submission action takes place. 
     * <p> <b>NOTE:</b> &ensp; <code>setDefaultText()</code> must still be called in the runnable for the text to be set.
     * <p> For example:
     *  <code>
     *  <pre>
     *  SendLogbookAction sendLogbookAction ....
     *  sendLogBookAction.setDefaultRunnable(() -> 
     *  {
     *      StringBuilder builder = ...
     *      builder.append("Stuff")
     *             .append(.....
     *             
     *      setDefaultText(builder.toString);
     *  });
     *  </pre>
     *  </code>
     *  @author Evan Smith
     * */
    public void setDefaultRunnable(Runnable runnable)
    {
        default_runnable = runnable;
    }
}
