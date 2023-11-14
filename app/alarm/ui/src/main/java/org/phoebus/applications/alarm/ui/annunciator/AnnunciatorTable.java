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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.talk.TalkClient;
import org.phoebus.applications.alarm.talk.TalkClientListener;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.ToolbarHelper;
import org.phoebus.util.time.TimestampFormats;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/** Table View for the Annunciator
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AnnunciatorTable extends VBox implements TalkClientListener
{
    private static final Image anunciate_icon = ImageCache.getImage(AlarmUI.class, "/icons/annunciator.png");
    private static final Image mute_icon = ImageCache.getImage(AlarmUI.class, "/icons/silence.png");

    private final Tooltip muteTip         = new Tooltip("Mute the annunciator");
    private final Tooltip annunciateTip   = new Tooltip("Un-mute the annunciator");
    private final ToggleButton muteButton = new ToggleButton("", new ImageView(mute_icon));
    private final ToggleButton testButton = new ToggleButton("Test");
    private final Button clearTableButton = new Button("Clear Messages");

    private final ToolBar toolbar;

    private final TableView<AnnunciationRowInfo> table = new TableView<>();

    private final TableColumn<AnnunciationRowInfo, Instant>       time        = new TableColumn<>("Time Received");
    private final TableColumn<AnnunciationRowInfo, SeverityLevel> severity    = new TableColumn<>("Severity");
    private final TableColumn<AnnunciationRowInfo, String>        description = new TableColumn<>("Description");

    private final CopyOnWriteArrayList<AnnunciationRowInfo> messages = new CopyOnWriteArrayList<>();

    private final TalkClient client;

    private final int annunciator_threshold       = AlarmSystem.annunciator_threshold;
    private final int annunciator_retention_count = AlarmSystem.annunciator_retention_count;

    private final AnnunciatorController annunciatorController;

    /**
     * AnnunciatorCell extends table cell and implements a method that can alter the cells background
     * to indicate if the mute button is selected or not.
     *
     * @param <K>
     * @param <V>
     */
    private class AnnunciatorCell<K, V> extends TableCell<K, V>
    {
        private final String ORANGE     = "#ff8700"; // RGB (255, 135,   0)
        private final String OFF_ORANGE = "#dc6400"; // RGB (220, 100,   0)
        private final String WHITE      = "#ffffff"; // RGB (255, 255, 255)
        private final String OFF_WHITE  = "#f0f0f0"; // RGB (240, 240, 240)

        /**
         * Set the color of the cell if muted is <code>true</code>.
         * @param muted - boolean
         */
        protected void setMutedColor(final boolean muted)
        {
            final int rowIndex = getTableRow().getIndex();
            // Determine the row specific color.
            final String rowColor       = rowIndex % 2 == 0 ? WHITE  : OFF_WHITE;
            final String mutedRowColor  = rowIndex % 2 == 0 ? ORANGE : OFF_ORANGE;

            // Color the cells if muted, leave them the row color otherwise.
            String color = (muted) ? mutedRowColor : rowColor;

            setBackground( new Background( new BackgroundFill(
                                            Paint.valueOf(color), // Set the color.
                                            new CornerRadii(0),   // We want square cells.
                                            new Insets(1))));     // Don't color over the cell borders.

            /*
             * Upon row selection, the table will update the text color based on the darkness of the ROW background.
             * Not the cell background. So the cell may have a white background, but the row could be blue when selected.
             * This blue color would trigger the table to set the cell's text color to white to provide a nice contrast.
             * Only the white text would be displayed on the cell's white background, not the blue background of the
             * table row. To prevent this, set the cell's text color to always be black when drawn on a background.
             */
            setStyle("-fx-text-background-color: black;");
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
    public AnnunciatorTable (final TalkClient client)
    {
        this.client = client;
        this.client.addListener(this);

        if (annunciator_retention_count < 1)
            logger.log(Level.SEVERE, "Annunciation Retention Count set below 1.");

        table.setPlaceholder(new Label("No annunciations"));

        time.setCellValueFactory(cell -> cell.getValue().time_received);
        time.setCellFactory(c -> new TimeCell());
        time.setPrefWidth(190);
        time.setResizable(false);
        table.getColumns().add(time);

        // Sort by time, most recent on top
        time.setSortType(SortType.DESCENDING);
        table.getSortOrder().setAll(List.of(time));

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

        // Give the addAnnunciationToTable method as a callback to the controller. Will be called after message handling to add message to table.
        annunciatorController = new AnnunciatorController(annunciator_threshold, this::addAnnunciationToTable);

        // Top button row
        muteButton.setTooltip(muteTip);
        muteButton.setOnAction(event ->
        {
            // Mute is true when the annunciator should be muted.
            final boolean mute = muteButton.isSelected();
            // Update image
            final ImageView image = (ImageView) muteButton.getGraphic();
            image.setImage(mute ? anunciate_icon : mute_icon);
            muteButton.setTooltip(mute ? annunciateTip : muteTip);
            annunciatorController.setMuted(mute);
            // Refresh the table cell items so that they recalculate their background color.
            table.refresh();
        });

        testButton.setTooltip(new Tooltip("Play test message"));
        testButton.setOnAction(event ->
            annunciatorController.annunciate(new AnnunciatorMessage(false, SeverityLevel.OK, Instant.now(), "Testing 1 2 3")) );

        clearTableButton.setTooltip(new Tooltip("Clear the messages in the table."));
        clearTableButton.setOnAction(event ->
        {
            final Alert alert  = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Clear Annunciator Table");
            alert.setHeaderText("Clear the table of all annunciations?");
            DialogHelper.positionDialog(alert, clearTableButton, -200, -100);
            alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> clearTable());
        });

        toolbar = new ToolBar(ToolbarHelper.createSpring(), muteButton, testButton, clearTableButton);

        getChildren().setAll(toolbar, table);

        // Annunciate message so that user can determine if annunciator and table are indeed functional.
        messageReceived(SeverityLevel.OK, true, "Annunciator started");
    }

    ToolBar getToolbar()
    {
        return toolbar;
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
    public void messageReceived(final SeverityLevel severity, final boolean standout, final String message)
    {
        final AnnunciatorMessage annunciation = new AnnunciatorMessage(standout, severity, Instant.now(), message);
        logger.log(Level.FINE,
                   () -> "Annunciator received " +
                         TimestampFormats.MILLI_FORMAT.format(annunciation.time) +
                         " Severity: " + annunciation.severity +
                         ", Description: \"" + annunciation.message + "\"");

        annunciatorController.annunciate(annunciation);
    }

    /**
     * Handle message addition to the table.
     */
    private void addAnnunciationToTable(AnnunciatorMessage annunciation)
    {
        Platform.runLater(() ->
        {
            AnnunciationRowInfo row = new AnnunciationRowInfo(annunciation.time, annunciation.severity, annunciation.message);
            messages.add(row);

            // Remove the oldest messages to stay under the message retention threshold.
            if (messages.size() > annunciator_retention_count)
            {
                // Only the table items are sorted, the messages list maintains chronological order.
                final AnnunciationRowInfo to_remove = messages.remove(0);

                table.getItems().remove(to_remove);
            }

            // The table should maintain its selected sort order after message addition.
            table.getItems().add(row);
            table.getItems().sort(table.getComparator());
        });
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
