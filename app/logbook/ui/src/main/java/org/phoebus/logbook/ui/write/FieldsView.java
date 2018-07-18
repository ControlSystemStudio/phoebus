/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class FieldsView extends VBox
{
    private final LogEntryModel    model;
    private final Label            titleLabel, textLabel;
    private final TextField        titleField;
    private final LogbooksTagsView logbooksAndTags;
    private final TextArea         textArea;
    
    public FieldsView(LogEntryModel model)
    {
        this.model = model;
        titleLabel = new Label("Title:");
        titleField = new TextField(model.getTitle());
        
        // log books and tags text field, selector, and addition view button
        logbooksAndTags =  new LogbooksTagsView(model);
        
        textLabel  = new Label("Text:");
        textArea   = new TextArea(model.getText());
        
        formatView();
    }

    private void formatView()
    {
        setTextActions();
        
        setSpacing(10);
        // title label and title field.
        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER);
        titleLabel.setPrefWidth(LogEntryDialog.labelWidth);
        textLabel.setPrefWidth(LogEntryDialog.labelWidth);
        
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

        getChildren().addAll(titleBox, logbooksAndTags, textBox);
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
}
