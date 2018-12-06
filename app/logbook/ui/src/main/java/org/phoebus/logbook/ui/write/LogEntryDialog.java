/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.ui.Messages;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Dialog for making an entry into a log book.
 * * <p> Username, password, title, and logbooks are all required fields.
 * The button to submit entry will be disabled if they are not all set.
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class LogEntryDialog extends Dialog<LogEntry>
{
    /** Width of labels on views leftmost column. */
    public static final int labelWidth = 80;

    /** Purveyor of log entry application state. */
    private final LogEntryModel model;

    /** Dialog Content */
    private final VBox content;

    /** View handles the input for creation of the entry. */
    private final FieldsView logEntryFields;

    /** View handles addition of log entry attachments. */
    private final AttachmentsView attachmentsView;

    /** Button type for submitting log entry. */
    private final ButtonType submitType;

    private static final String SUBMIT_ID = "submitButton",
                                CANCEL_ID = "cancelButton";

    public LogEntryDialog(final Node parent, LogEntry template)
    {

        model = new LogEntryModel(parent);

        if (null != template)
            setModelTemplate(template);

        content = new VBox();

        // title and text labels and fields.
        logEntryFields = new FieldsView(model);

        // Images, Files, Properties
        attachmentsView = new AttachmentsView(parent, model);

        // Let the Text Area grow to the bottom.
        VBox.setVgrow(logEntryFields,  Priority.ALWAYS);

        //VBox.setMargin(credentialEntry, new Insets(10, 0,  0, 0));
        VBox.setMargin(logEntryFields,  new Insets( 0, 0, 10, 0));

        content.setSpacing(10);
        content.getChildren().addAll(logEntryFields, attachmentsView);

        setTitle(Messages.CreateLogbookEntry);

        getDialogPane().setContent(content);

        submitType = new ButtonType(Messages.Submit, ButtonBar.ButtonData.OK_DONE);

        setResizable(true);

        DialogHelper.positionAndSize(this, parent,
                PhoebusPreferenceService.userNodeForClass(LogEntryDialog.class),
                800, 1000);

        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, submitType);

        Button submitButton = (Button) getDialogPane().lookupButton(submitType);
        Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);

        submitButton.setId(SUBMIT_ID);
        cancelButton.setId(CANCEL_ID);

        cancelButton.setTooltip(new Tooltip(Messages.CancelTooltip));
        submitButton.setTooltip(new Tooltip(Messages.SubmitTooltip));

        // Bind the submit button's disable property to the inverse of the model's ready to submit property.
        submitButton.disableProperty().bind(model.getReadyToSubmitProperty().not());

        // Prevent enter from causing log entry submission. We want the button to be clicked.
        // If the button doesn't have focus, it wasn't clicked.
        submitButton.addEventFilter(ActionEvent.ACTION, eventFilter ->
        {
            if (!submitButton.isFocused())
                eventFilter.consume();
        });

        setResultConverter(buttonType ->
        {
            try
            {
                model.setImages(attachmentsView.getImages());
                model.setFiles(attachmentsView.getFiles());
                return buttonType == submitType ? model.submitEntry() : null;
            }
            catch (IOException ex)
            {
                logger.log(Level.WARNING, "Log Entry Submission Failed!", ex);
                return null;
            }
        });
    }

    /** @return User name under which entry was submitted */
    public String getUsername()
    {
        return model.getUsername();
    }

    /**
     * The model will be initialized to contain the same data as the template.
     * @param template
     */
    private void setModelTemplate(LogEntry template)
    {
        model.setTitle(template.getTitle());
        model.setText(template.getDescription());
        Collection<Logbook> logbooks = template.getLogbooks();
        logbooks.forEach(logbook->
        {
            model.addSelectedLogbook(logbook.getName());
        });

        Collection<Tag> tags = template.getTags();
        tags.forEach(tag->
        {
            model.addSelectedTag(tag.getName());
        });

        final List<Image> images = new ArrayList<>();
        final List<File> files = new ArrayList<>();
        for (Attachment attachment : template.getAttachments())
        {
            final File file = attachment.getFile();

            // Add image to model if attachment is image.
            if (attachment.getContentType().equals(Attachment.CONTENT_IMAGE))
            {
                try
                {
                    images.add(new Image(new FileInputStream(file)));
                }
                catch (FileNotFoundException ex)
                {
                    logger.log(Level.WARNING, "Log entry template attachment file not found: '" + file.getName() + "'", ex);
                }
            }
            // Add file to model if attachment is file.
            else if (attachment.getContentType().equals(Attachment.CONTENT_FILE))
                files.add(file);
        }
        model.setImages(images);
        model.setFiles(files);
    }

    /** Set a runnable to be executed <b>after</b> the log entry submission occurs. */
    public void setOnSubmitAction(Runnable runnable)
    {
        model.setOnSubmitAction(runnable);
    }
}