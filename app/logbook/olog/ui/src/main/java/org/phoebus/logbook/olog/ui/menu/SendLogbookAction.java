/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.olog.ui.menu;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.olog.ui.LogbookAvailabilityChecker;
import org.phoebus.logbook.olog.ui.write.LogEntryEditorStage;
import org.phoebus.olog.es.api.model.OlogLog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Screenshot;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

import static org.phoebus.logbook.LogService.logger;

/**
 * Action for submitting log entry
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SendLogbookAction extends MenuItem {
    // TODO Create a messages class for the log book ui.
    private static final String MESSAGE = "Send To Log Book...";

    /**
     * Constructor.
     *
     * @param parent    JavaFX parent that context menu is called from.
     * @param title     Initial title or <code>null</code>
     * @param body      Initial body text or <code>null</code>
     * @param get_image Supplier for image to attach, or <code>null</code>
     */
    public SendLogbookAction(final Node parent, final String title, final String body, final Supplier<Image> get_image) {
        this(parent, title, body == null ? null : () -> body, get_image);
    }

    /**
     * Constructor.
     *
     * @param parent    JavaFX parent that context menu is called from.
     * @param title     Initial title or <code>null</code>
     * @param get_body  Supplier for initial body text or <code>null</code>
     * @param get_image Supplier for image to attach, or <code>null</code>
     */
    public SendLogbookAction(final Node parent, final String title, final Supplier<String> get_body, final Supplier<Image> get_image) {
        super(MESSAGE, ImageCache.getImageView(SendLogbookAction.class, "/icons/logentry-add-16.png"));

        if (LogbookPreferences.is_supported)
            setOnAction(event ->
            {
                // On UI thread, create screenshot etc.
                final String body = get_body == null ? "" : get_body.get();
                final Image image = get_image == null ? null : get_image.get();

                // Save to file in background thread
                JobManager.schedule(MESSAGE, monitor ->
                {
                    if (!LogbookAvailabilityChecker.isLogbookAvailable()) {
                        return;
                    }
                    final File image_file = image == null ? null : new Screenshot(image).writeToTempfile("image");

                    // Create log entry via dialog on UI thread
                    Platform.runLater(() -> submitLogEntry(parent, title, body, image_file));
                });
            });
        else
            setDisable(true);
    }

    private void submitLogEntry(final Node parent, final String title, final String body, final File image_file) {
        OlogLog ologLog = new OlogLog();
        ologLog.setTitle(title != null ? title : "");
        ologLog.setDescription(body != null ? body : "");

        if (image_file != null) {
            try {
                final Attachment attachment = AttachmentImpl.of(image_file, "image", false);
                List<Attachment> attachments = new ArrayList<>();
                attachments.add(attachment);
                ologLog.setAttachments(attachments);
            } catch (FileNotFoundException ex) {
                logger.log(Level.WARNING, "Cannot attach " + image_file, ex);
            }
        }
        new LogEntryEditorStage(parent, ologLog).show();
    }
}
