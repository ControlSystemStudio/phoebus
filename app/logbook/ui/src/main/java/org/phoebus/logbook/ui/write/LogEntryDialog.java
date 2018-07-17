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
import java.util.Collection;
import java.util.logging.Level;

import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
/**
 * Dialog for making an entry into a log book.
 * @author Evan Smith
 */
public class LogEntryDialog extends Dialog<LogEntry>
{
    /** Width of labels on views leftmost column. */
    public static final int labelWidth = 80;
        
    /** Purveyor of log entry application state. */
    private final LogEntryModel           model;
    
    /** Dialog Content */
    private final VBox                    content;
    
    /** View handles user credential entry for access to log. */
    private final CredentialEntryView  credentialEntry;
    
    /** View handles displaying of date and log entry level selection. */
    private final DateLevelView        dateAndLevel;
    
    /** View handles the input for creation of the entry. */
    private final FieldsView      logEntryFields;
        
    /** View handles addition of log entry attachments. */
    private final AttachmentsView attachmentsView;
    
    /** Button type for submitting log entry. */
    private final ButtonType submit;

    public LogEntryDialog(final Node parent, LogEntry template)
    {         
        model = new LogEntryModel(parent);
        
        if (null != template)
            setModelTemplate(template);
        
        content = new VBox();
        
        // user name and password label and fields.
        credentialEntry = new CredentialEntryView(model);
        
        // date and level labels, fields, and selectors.
        dateAndLevel = new DateLevelView(model);
        
        // title and text labels and fields.
        logEntryFields = new FieldsView(model);
                
        // Images, Files, Properties
        attachmentsView = new AttachmentsView(model);        
        
        // Let the Text Area grow to the bottom.
        VBox.setVgrow(logEntryFields,  Priority.ALWAYS);

        VBox.setMargin(credentialEntry,       new Insets(10, 0,  0, 0));
        VBox.setMargin(logEntryFields,        new Insets( 0, 0, 10, 0));
        
        content.setSpacing(10);
        content.getChildren().addAll(credentialEntry, dateAndLevel, logEntryFields, attachmentsView);
        
        setTitle("Create Log Book Entry");
        
        getDialogPane().setContent(content);
        
        submit = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        
        setResizable(true);
        
        DialogHelper.positionAndSize(this, parent,
                PhoebusPreferenceService.userNodeForClass(LogEntryDialog.class),
                800, 1000);

        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, submit);
        
        setResultConverter(button ->
        {
            try
            {
                return button == submit ? model.submitEntry() : null;
            } 
            
            catch (IOException ex)
            {
                logger.log(Level.WARNING, "Log Entry Submission Failed!", ex);
                return null;
            }
        });
    }

    /**
     * The model will be initialized to contain the same data as the template.
     * @param template
     */
    private void setModelTemplate(LogEntry template)
    {
        // model.setTitle(template.getTitle());
        model.setText(template.getDescription());
        Collection<Logbook> logbooks = template.getLogbooks();
        logbooks.forEach(logbook-> 
        {
            try
            {
                model.addSelectedLogbook(logbook.getName());
            } 
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Selected logbook initialization failed.", ex);
            }
        });  
        
        Collection<Tag> tags = template.getTags();
        tags.forEach(tag-> 
        {
            try
            {
                model.addSelectedTag(tag.getName());
            } 
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Selected tag initialization failed.", ex);
            }
        });
        
        // TODO Currently assuming all attachments are images. Update to handle standard files as well.
        for (Attachment attachment : template.getAttachments())
        {
            File file = attachment.getFile();
            // TODO Once the API is updated to allow the Attachment.contentType to be set, check the content type to differentiate from files and images.
            Image image = null;
            try
            {
                image = new Image(new FileInputStream(file));
            } 
            catch (FileNotFoundException ex)
            {
                logger.log(Level.WARNING, "Log entry template attachment file not found: '" + file.getName() + '"', ex);
            }
            
            model.addImage(image);
        }
    }

    /** Set a runnable to be executed <b>after</b> the log entry submission occurs. */
    public void setOnSubmitAction(Runnable runnable)
    {
        model.setOnSubmitAction(runnable);
    }
}