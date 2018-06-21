/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.talk.TalkClient;
import org.phoebus.applications.alarm.talk.TalkClientListener;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.util.time.TimestampFormats;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * Table View for the Annunciator
 * @author Evan Smith
 */
public class AnnunciatorTable extends VBox implements TalkClientListener
{
    private final Button       clearTableButton = new Button("Clear Messages"); 
    private final Alert        clearTableAlert  = new Alert(AlertType.CONFIRMATION);
    private final ToggleButton muteButton       = new ToggleButton("Mute Annunciator");
    
    private final TableView<AnnunciationRowInfo> table = new TableView<>();
    
    TableColumn<AnnunciationRowInfo, Instant>       time        = new TableColumn<>("Time Received");
    TableColumn<AnnunciationRowInfo, SeverityLevel> severity    = new TableColumn<>("Severity");
    TableColumn<AnnunciationRowInfo, String>        description = new TableColumn<>("Description");

    private final CopyOnWriteArrayList<AnnunciationRowInfo> messages = new CopyOnWriteArrayList<>();
    
    private final TalkClient client;
    
    private final int annunciator_threshold       = AlarmSystem.annunciator_threshold;
    private final int annunciator_retention_count = AlarmSystem.annunciator_retention_count;
    
    private final AnnunciatorController annunciatorController;

    /**
     * AnnunciatorCell extends table cell and implements a method that can alter the cells background 
     * to indicate if the mute button is selected or not.
     * @author 1es
     *
     * @param <K>
     * @param <V>
     */
    private class AnnunciatorCell<K, V> extends TableCell<K, V>
    {
        private final String ORANGE = "#ff8700";
        private final String WHITE  = "#ffffff";
        protected void setMutedColor(final boolean muted)
        {
            String color = (muted) ? ORANGE : WHITE;
            setBackground(new Background(new BackgroundFill(Paint.valueOf(color), new CornerRadii(0), new Insets(1))));
        }
    }
    
    /**
     * Table cell that displays alarm severity using icons and colored text.
     */
    private class SeverityCell extends AnnunciatorCell<AnnunciationRowInfo, SeverityLevel>
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
            setMutedColor(muteButton.isSelected());
        }
    }
    
    /**
     * Table cell that shows a time stamp.
     */
    private class TimeCell extends AnnunciatorCell<AnnunciationRowInfo, Instant>
    {
        @Override
        protected void updateItem(final Instant item, final boolean empty)
        {
            super.updateItem(item, empty);

            if (empty  ||  item == null)
            {
                setText("");
            }
            else
            {
                setText(TimestampFormats.MILLI_FORMAT.format(item));
            }
            setMutedColor(muteButton.isSelected());
        }
    }
    
    /**
     * Table Cell that shows a message.
     */
    private class MessageCell extends AnnunciatorCell<AnnunciationRowInfo, String>
    {
        @Override
        protected void updateItem(final String item, final boolean empty)
        {
            super.updateItem(item, empty);

            if (empty  ||  item == null)
            {
                setText("");
            }
            else
            {
                setText(item);
            }
            setMutedColor(muteButton.isSelected());
        }
    }
    
    /**
     * Create an AnnunciatorTable view.
     * @param client - TalkClient used to listen to the *Talk topic.
     */
    public AnnunciatorTable (TalkClient client)
    {
        this.client = client;
        this.client.addListener(this);
        
        if (annunciator_retention_count < 1)
            logger.log(Level.SEVERE, "Annunciation Retention Count set below 1.");
            
        time.setCellValueFactory(cell -> cell.getValue().time_received);
        time.setCellFactory(c -> new TimeCell());
        time.setPrefWidth(190);
        time.setResizable(false);
        table.getColumns().add(time);
               
        severity.setCellValueFactory(cell -> cell.getValue().severity);
        severity.setCellFactory(c -> new SeverityCell());
        severity.setPrefWidth(90);
        severity.setResizable(false);
        table.getColumns().add(severity);

        description.setCellValueFactory(cell -> cell.getValue().message);
        description.setCellFactory(c -> new MessageCell());
        // Width left in window is window width minus time width (190), minus severity width (90), minus width of window edges(1 * 2).
        description.prefWidthProperty().bind(table.widthProperty().subtract(282));
        table.getColumns().add(description);

        // Table should always grow to fill VBox.
        setVgrow(table, Priority.ALWAYS);
          
        annunciatorController = new AnnunciatorController(annunciator_threshold);
        
        // Top button row
        HBox hbox = new HBox();
        
        muteButton.setTooltip(new Tooltip("Mute the annunciator."));
        muteButton.setOnAction((event) -> {
            annunciatorController.setMuted(muteButton.isSelected());
            // Set the cell backgrounds to some color to identify table as muted.
            table.refresh();
        });
        
        clearTableAlert.setTitle("Clear Annunciator Table");
        clearTableAlert.setHeaderText("Clear the table of all annunciations?");
        
        clearTableButton.setTooltip(new Tooltip("Clear the messages in the table."));
        clearTableButton.setOnAction((event) -> 
        {
            clearTableAlert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> clearTable());
        });
        
        hbox.getChildren().addAll(muteButton, clearTableButton);
        hbox.setAlignment(Pos.BASELINE_RIGHT);
        
        this.getChildren().add(hbox);
        this.getChildren().add(table);      
    }
    
    /**
     * Clear the table and the message list.
     */
    private void clearTable()
    {
        logger.log(Level.INFO, "Annunciator table cleared.");
        messages.clear();
        // Clear the table on the UI thread.
        Platform.runLater(() -> 
        {
            table.getItems().clear();
        });
    }

    /**
     * Override of the TalkClientListener messageReceived method.
     * <p> Called whenever the listener is notified of a received message.
     */
    @Override
    public void messageReceived(SeverityLevel severity, boolean standout, String message)
    {     
        AnnunciationRowInfo annunciation = new AnnunciationRowInfo(Instant.now(), severity, message);
        
        addAnnunciationToTable(annunciation);
        logAnnunciation(annunciation);  

        annunciatorController.handleAnnunciation(standout, annunciation);
    }
    
    /**
     * Handle message addition to the table.
     */
    private void addAnnunciationToTable(AnnunciationRowInfo annunciation)
    {
        messages.add(annunciation);
        
        // Remove the oldest messages to stay under the message retention threshold. 
        if (messages.size() > annunciator_retention_count)
        {
            // Only the table items are sorted, the messages list maintains chronological order.
            final AnnunciationRowInfo to_remove = messages.remove(0);
            Platform.runLater(() -> 
            {
                table.getItems().remove(to_remove);
                table.getItems().sort(table.getComparator());
            });
        }
        
        // Update the table on the UI thread.
        Platform.runLater( () ->
        {
            // The table should maintain its selected sort order after message addition.
            table.getItems().add(annunciation);
            table.getItems().sort(table.getComparator());
        });
    }
    
    /**
     * Log an annunciation.
     * @param annunciation
     */
    private void logAnnunciation(AnnunciationRowInfo annunciation)
    {
        logger.info(TimestampFormats.MILLI_FORMAT.format(annunciation.time_received.get()) + 
                " Severity: " + annunciation.severity.get() + 
                ", Description: \"" + annunciation.message.get() + "\"");
    }

    /**
     * Shut down the table.
     */
    public void shutdown()
    {
        // Stop the annunciator controller.
        try
        {
            annunciatorController.shutdown();
        } 
        catch (InterruptedException e)
        { /* Ignore. Time to die. */ }
    }
}
