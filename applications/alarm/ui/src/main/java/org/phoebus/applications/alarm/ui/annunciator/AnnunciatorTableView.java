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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AnnunciatorTableView extends TableView<Message> implements TalkClientListener
{
    
    final CopyOnWriteArrayList<Message> messages = new CopyOnWriteArrayList<>();
    final TalkClient client;
    final ArrayList<TableColumn<Message, String>> columns = new ArrayList<>();

    public AnnunciatorTableView (TalkClient client)
    {
        this.client = client;
        client.addListener(this);
        TableColumn<Message, String> time = new TableColumn<Message, String>("Time Received");
        time.setCellValueFactory(new PropertyValueFactory<>("time"));
        time.prefWidthProperty().bind(widthProperty().multiply(0.2));
        time.setResizable(false);
        columns.add(time);
        
        TableColumn<Message, String> severity = new TableColumn<Message, String>("Severity");
        severity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        severity.prefWidthProperty().bind(widthProperty().multiply(0.1));
        severity.setResizable(false);
        columns.add(severity);

        TableColumn<Message, String> description = new TableColumn<Message, String>("Description");
        description.setCellValueFactory(new PropertyValueFactory<>("description"));
        description.prefWidthProperty().bind(widthProperty().multiply(0.7));
        description.setResizable(false);
        columns.add(description);

        // This seems foolish. Is there no way to build the observable list beforehand?
        setItems(FXCollections.observableArrayList(messages));
        getColumns().addAll(columns);
    }
    
    @Override
    public void messageRecieved(String severity, String description)
    {
        Instant now = Instant.now();
        messages.add(new Message(now.toString(), severity, description));
        Platform.runLater( () -> {
            setItems(FXCollections.observableArrayList(messages));
        });
    }

}
