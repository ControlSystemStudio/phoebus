/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import java.time.Instant;

import org.phoebus.util.time.TimestampFormats;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * View to display the date and select the log entry level.
 * @author Evan Smith
 */
public class DateLevelView extends HBox
{
    private final LogEntryModel          model;
    private final Label                  dateLabel, levelLabel;
    private final TextField              dateField;
    private final ComboBox<String>       levelSelector;
    private final ObservableList<String> levels = FXCollections.observableArrayList(
                                                        "Urgent",
                                                        "High",
                                                        "Normal");

    public DateLevelView(final LogEntryModel model)
    {
        this.model = model;
        
        dateLabel = new Label("Date:");
        dateField = new TextField(TimestampFormats.DATE_FORMAT.format(Instant.now()));
        dateField.setPrefWidth(100);

        this.model.setDate(dateField.getText());
        levelLabel = new Label("Level:");
        levelSelector = new ComboBox<String>(levels);
        
        setSelectorAction();
        
        formatView();
    }

    private void formatView()
    {
        dateField.setEditable(false);
        dateField.setTooltip(new Tooltip("Current Date"));
        dateLabel.setPrefWidth(LogEntryDialog.labelWidth);
        levelLabel.setAlignment(Pos.CENTER_RIGHT);
        levelSelector.setTooltip(new Tooltip("Select the log entry level."));
        // Put log level label and selector in HBox so that they can be right justified.
        HBox levelBox  = new HBox();
        levelBox.getChildren().addAll(levelLabel, levelSelector);
        levelBox.setSpacing(5);
        levelBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(levelBox, Priority.ALWAYS);

        setAlignment(Pos.CENTER);
        setSpacing(5);
        getChildren().addAll(dateLabel, dateField, levelBox);
    }
   
    private void setSelectorAction()
    {
        levelSelector.setOnAction(event ->
        {
            model.setLevel(levelSelector.getSelectionModel().getSelectedItem());
        });
    }
}
