/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.phoebus.applications.alarm.talk.Message;
import org.phoebus.applications.alarm.talk.TalkClient;
import org.phoebus.applications.alarm.talk.TalkClientListener;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Table View for the Annunciator
 * @author Evan Smith
 */
public class AnnunciatorTable extends VBox implements TalkClientListener
{
    final ToggleButton mute_button = new ToggleButton("Mute Annunciator");
    final TableView<Message> table = new TableView<Message>();
    
    final CopyOnWriteArrayList<Message> messages = new CopyOnWriteArrayList<>();
    final TalkClient client;
    final ArrayList<TableColumn<Message, String>> columns = new ArrayList<>();

    public AnnunciatorTable (TalkClient client)
    {
        this.client = client;
        client.addListener(this);
        TableColumn<Message, String> time = new TableColumn<Message, String>("Time Received");
        time.setCellValueFactory(new PropertyValueFactory<>("time"));
        time.prefWidthProperty().bind(table.widthProperty().multiply(0.2));
        time.setResizable(false);
        columns.add(time);
        
        TableColumn<Message, String> severity = new TableColumn<Message, String>("Severity");
        severity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        severity.prefWidthProperty().bind(table.widthProperty().multiply(0.1));
        severity.setResizable(false);
        columns.add(severity);

        TableColumn<Message, String> description = new TableColumn<Message, String>("Description");
        description.setCellValueFactory(new PropertyValueFactory<>("description"));
        description.prefWidthProperty().bind(table.widthProperty().multiply(0.7));
        description.setResizable(false);
        columns.add(description);

        table.setItems(FXCollections.observableArrayList(messages));
        table.getColumns().addAll(columns);
        
        HBox hbox = new HBox();
        hbox.getChildren().add(mute_button);
        hbox.setAlignment(Pos.BASELINE_RIGHT);
        this.getChildren().add(hbox);
        this.getChildren().add(table);
    }
    
   
    @Override
    public void messageRecieved(String severity, String description)
    {
        Instant now = Instant.now();
        messages.add(new Message(now.toString(), severity, description));
        // Update the table on the UI thread.
        Platform.runLater( () ->
        {
            table.setItems(FXCollections.observableArrayList(messages));
        });
    }

}
