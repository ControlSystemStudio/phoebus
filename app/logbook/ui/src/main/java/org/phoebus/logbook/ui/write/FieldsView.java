/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import java.time.Instant;

import org.phoebus.logbook.ui.LogbookUiPreferences;
import org.phoebus.util.time.TimestampFormats;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class FieldsView extends VBox
{   
    private final LogEntryModel    model;
    
    private final Label            userFieldLabel, passwordFieldLabel;
    private final TextField        userField;
    private final PasswordField    passwordField;
    
    private final Label                  dateLabel, levelLabel;
    private final TextField              dateField;
    private final ComboBox<String>       levelSelector;
    private final ObservableList<String> levels = FXCollections.observableArrayList(
                                                        "Urgent",
                                                        "High",
                                                        "Normal");
    
    private final Label            titleLabel, textLabel;
    private final TextField        titleField;
    private final LogbooksTagsView logbooksAndTags;
    private final TextArea         textArea;
    
    public FieldsView(LogEntryModel model)
    {
        this.model = model;
        
        
        Instant now = Instant.now();
        dateLabel = new Label("Date:");
        dateField = new TextField(TimestampFormats.DATE_FORMAT.format(now));
        dateField.setPrefWidth(100);

        this.model.setDate(now);
        levelLabel = new Label("Level:");
        levelSelector = new ComboBox<String>(levels);
        
        setSelectorAction();
        
        titleLabel = new Label("Title:");
        titleField = new TextField(model.getTitle());
        titleField.textProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (newVal.trim().isEmpty())
                titleLabel.setTextFill(Color.RED);
            else
                titleLabel.setTextFill(Color.BLACK);
        });

        // log books and tags text field, selector, and addition view button
        logbooksAndTags =  new LogbooksTagsView(model);
        
        textLabel  = new Label("Text:");
        textArea   = new TextArea(model.getText());
        
        String storedUser = "";
        String storedPass = "";
        
        userFieldLabel     = new Label("User Name:");       
        passwordFieldLabel = new Label("Password:");

        userField = new TextField();
        if (null != storedUser)
        {
            userField.setText(storedUser);
            model.setUser(storedUser);
        }
        // Update the username entered property when appropriate.
        userField.textProperty().addListener((changeListener, oldVal, newVal) -> 
        {
            if (newVal.trim().isEmpty())
                userFieldLabel.setTextFill(Color.RED);
            else
                userFieldLabel.setTextFill(Color.BLACK);
        });

        passwordField = new PasswordField();
        if(null != storedPass)
        {
            passwordField.setText(storedPass);
            model.setPassword(storedPass);
        }
        // Update the password entered property when appropriate.
        passwordField.textProperty().addListener((changeListener, oldVal, newVal) -> 
        {
            if (newVal.trim().isEmpty())
                passwordFieldLabel.setTextFill(Color.RED);
            else
                passwordFieldLabel.setTextFill(Color.BLACK);
        });
        
        model.getUpdateCredentialsProperty().addListener((changeListener, oldVal, newVal) ->
        {
            userField.setText(model.getUsername());
            passwordField.setText(model.getPassword());
            
            // Put focus on first required field that is empty.
            Platform.runLater(() ->
            {
                if (userField.getText().isEmpty())
                    userField.requestFocus();
                else if (passwordField.getText().isEmpty())
                    passwordField.requestFocus();
                else
                    titleField.requestFocus();
            });
        });
        
        userField.requestFocus();
        if (LogbookUiPreferences.save_credentials)
        {
            model.fetchStoredUserCredentials();
        }
        
        formatView();
    }
    
    private void formatView()
    {
        setSpacing(10);
        getChildren().addAll(formatCredentialBox(), formatDateLevelBox(), formatTitleTextBox());
    }
    
    private HBox formatCredentialBox()
    {
        HBox credentialBox = new HBox();

        setFieldActions();
        userFieldLabel.setPrefWidth(LogEntryDialog.labelWidth);
        
        userFieldLabel.setTextFill(Color.RED);
        passwordFieldLabel.setTextFill(Color.RED);
        
        // The preferred width is set to zero so that the labels don't minimize themselves to let the fields have their preferred widths.
        userField.setPrefWidth(0);
        passwordField.setPrefWidth(0);
        
        HBox.setMargin(passwordFieldLabel, new Insets(0, 0, 0, 5));
        HBox.setHgrow(userField, Priority.ALWAYS);
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        
        credentialBox.setSpacing(5);
        credentialBox.setAlignment(Pos.CENTER);
        credentialBox.getChildren().addAll(userFieldLabel, userField, passwordFieldLabel, passwordField);

        return credentialBox;
    }
    
    private HBox formatDateLevelBox()
    {
        HBox dateLevelBox = new HBox();
        dateField.setEditable(false);
        dateField.setTooltip(new Tooltip("Current Date"));
        dateLabel.setPrefWidth(LogEntryDialog.labelWidth);
        levelLabel.setAlignment(Pos.CENTER_RIGHT);
        levelSelector.setMinHeight(26); // When opened from phoebus, not the demo, the selector gets squeezed down to 10 pixels tall.
        levelSelector.setTooltip(new Tooltip("Select the log entry level."));
        // Put log level label and selector in HBox so that they can be right justified.
        HBox levelBox  = new HBox();
        levelBox.getChildren().addAll(levelLabel, levelSelector);
        levelBox.setSpacing(5);
        levelBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(levelBox, Priority.ALWAYS);

        dateLevelBox.setAlignment(Pos.CENTER);
        dateLevelBox.setSpacing(5);
        dateLevelBox.getChildren().addAll(dateLabel, dateField, levelBox);
        
        return dateLevelBox;
    }
    
    private VBox formatTitleTextBox()
    {
        VBox titleTextBox = new VBox();
        setTextActions();

        titleTextBox.setSpacing(10);
        // title label and title field.
        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER);
        titleLabel.setPrefWidth(LogEntryDialog.labelWidth);
        textLabel.setPrefWidth(LogEntryDialog.labelWidth);
        
        titleLabel.setTextFill(Color.RED);

        titleField.setPrefWidth(0);
        HBox.setHgrow(titleField, Priority.ALWAYS);

        titleBox.setSpacing(5);
        titleBox.getChildren().addAll(titleLabel, titleField);
        
        // text label and text area.
        HBox textBox = new HBox();
        textBox.setAlignment(Pos.TOP_CENTER);
        textArea.setPrefWidth(0);
        HBox.setHgrow(textArea, Priority.ALWAYS);
        textBox.setSpacing(5);
        VBox.setVgrow(textBox, Priority.ALWAYS);

        textBox.getChildren().addAll(textLabel, textArea);
        
        titleTextBox.getChildren().addAll(titleBox, logbooksAndTags, textBox);
        
        VBox.setVgrow(titleTextBox, Priority.ALWAYS);
        return titleTextBox;
    }
    
    private void setFieldActions()
    {
        userField.setOnKeyReleased(event ->
        {
            model.setUser(userField.getText());
        });
        
        passwordField.setOnKeyReleased(event ->
        {
            model.setPassword(passwordField.getText());
        });
    }
    
    private void setTextActions()
    {
        titleField.setOnKeyReleased(event ->
        {
            model.setTitle(titleField.getText());
        });
        
        textArea.setOnKeyReleased(event -> 
        {
            model.setText(textArea.getText());
        });   
    }

    private void setSelectorAction()
    {
        levelSelector.setOnAction(event ->
        {
            model.setLevel(levelSelector.getSelectionModel().getSelectedItem());
        });
    }
    
    /** Have the title field request the focus. */
    public void requestTitleFocus()
    {
        titleField.requestFocus();
    }
}
