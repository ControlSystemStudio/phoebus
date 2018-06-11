/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;

import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.talk.Annunciation;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.util.time.TimestampFormats;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Table View for the Annunciator
 * @author Evan Smith
 */
public class AnnunciatorTable extends VBox implements TalkClientListener
{
    final ToggleButton mute_button = new ToggleButton("Mute Annunciator");
    final TableView<Annunciation> table = new TableView<>();
    
    final CopyOnWriteArrayList<Annunciation> messages = new CopyOnWriteArrayList<>();
    final TalkClient client;

    private class SeverityCell extends TableCell<Annunciation, SeverityLevel>
    {
        @Override
        protected void updateItem(final SeverityLevel item, final boolean empty)
        {
            super.updateItem(item, empty);

            if (empty  ||  item == null)
            {
                setGraphic(null);
                setText("");
                setTextFill(Color.BLACK);
            }
            else
            {
                setGraphic(new ImageView(AlarmUI.getIcon(item)));
                setText(item.toString());
                setTextFill(AlarmUI.getColor(item));
            }
        }
    }
    
    /** Table cell that shows a time stamp */
    private class TimeCell extends TableCell<Annunciation, Instant>
    {
        @Override
        protected void updateItem(final Instant item, final boolean empty)
        {
            super.updateItem(item, empty);

            if (empty  ||  item == null)
                setText("");
            else
                setText(TimestampFormats.MILLI_FORMAT.format(item));
        }
    }
    
    public AnnunciatorTable (TalkClient client)
    {
        this.client = client;
        client.addListener(this);
        TableColumn<Annunciation, Instant> time = new TableColumn<>("Time Received");
        
        time.setCellValueFactory(cell -> cell.getValue().time_received);
        time.setCellFactory(c -> new TimeCell());
        
        time.prefWidthProperty().bind(table.widthProperty().multiply(0.2));
        time.setResizable(false);
        table.getColumns().add(time);
        
        TableColumn<Annunciation, SeverityLevel> severity = new TableColumn<>("Severity");
        severity.setCellValueFactory(cell -> cell.getValue().severity);
        severity.setCellFactory(c -> new SeverityCell());
        severity.prefWidthProperty().bind(table.widthProperty().multiply(0.1));
        severity.setResizable(false);
        table.getColumns().add(severity);

        TableColumn<Annunciation, String> description = new TableColumn<>("Description");
        description.setCellValueFactory(cell -> cell.getValue().message);
        description.prefWidthProperty().bind(table.widthProperty().multiply(0.7));
        description.setResizable(false);
        table.getColumns().add(description);

        table.setItems(FXCollections.observableArrayList(messages));
        
        // Table should always grow to fill VBox.
        setVgrow(table, Priority.ALWAYS);
        
        // Top button row
        HBox hbox = new HBox();
        hbox.getChildren().add(mute_button);
        hbox.setAlignment(Pos.BASELINE_RIGHT);
        
        this.getChildren().add(hbox);
        this.getChildren().add(table);
    }
    
   
    
    @Override
    public void messageReceived(SeverityLevel severity, String message)
    {
        messages.add(new Annunciation(Instant.now(), severity, message));
        // Update the table on the UI thread.
        Platform.runLater( () ->
        {
            table.setItems(FXCollections.observableArrayList(messages));
        });
    }
    

}
