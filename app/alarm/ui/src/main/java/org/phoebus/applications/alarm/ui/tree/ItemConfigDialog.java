/*******************************************************************************
 * Copyright (c) 2018-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.tree.datetimepicker.DateTimePicker;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.util.time.SecondsParser;
import org.phoebus.util.time.TimeParser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;


/**
 * Dialog for editing {@link AlarmTreeItem}
 *
 * <p>When pressing "OK", dialog sends updated
 * configuration.
 */
@SuppressWarnings("nls")
class ItemConfigDialog extends Dialog<Boolean> {
    private TextField description;
    private CheckBox enabled, latching, annunciating;
    private DateTimePicker enabled_date_picker;
    private Spinner<Integer> delay, count;
    private TextField filter;
    private ComboBox<String> relative_date;
    private final TitleDetailTable guidance, displays, commands;
    private final TitleDetailDelayTable actions;

    private final SimpleBooleanProperty itemEnabled = new SimpleBooleanProperty();

    public ItemConfigDialog(final AlarmClient model, final AlarmTreeItem<?> item) {
        // Allow multiple instances
        initModality(Modality.NONE);
        setTitle("Configure " + item.getName());

        final GridPane layout = new GridPane();
        layout.setHgap(5);
        layout.setVgap(5);

        // First fixed-size column for labels
        // Second column grows
        final ColumnConstraints col1 = new ColumnConstraints(190);
        final ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        layout.getColumnConstraints().setAll(col1, col2);

        int row = 0;

        // Show item path, allow copying it out.
        // Can't edit; that's done via rename or move actions.
        layout.add(new Label("Path:"), 0, row);
        final TextField path = new TextField(item.getPathName());
        path.setEditable(false);
        layout.add(path, 1, row++);

        if (item instanceof AlarmClientLeaf leaf) {
            itemEnabled.setValue(((AlarmClientLeaf) item).isEnabled());

            layout.add(new Label("Description:"), 0, row);
            description = new TextField(leaf.getDescription());
            description.setTooltip(new Tooltip("Alarm description, also used for annunciation"));
            layout.add(description, 1, row++);
            GridPane.setHgrow(description, Priority.ALWAYS);

            layout.add(new Label("Behavior:"), 0, row);
            enabled = new CheckBox("Enabled");
            enabled.setTooltip(new Tooltip("Enable alarms? See also 'Enabling Filter'"));
            enabled.setSelected(leaf.isEnabled());

            itemEnabled.addListener((obs, o, n) -> {
                ((AlarmClientLeaf) item).setEnabled(n);
            });

            enabled.setOnAction(e -> {
                itemEnabled.setValue(enabled.isSelected());
                if (enabled.isSelected()) {
                    relative_date.getSelectionModel().clearSelection();
                    relative_date.setValue(null);
                    enabled_date_picker.getEditor().clear();
                    enabled_date_picker.setValue(null);
                }
                // User has unchecked checkbox to disable alarm -> disable indefinitely.
                if (!enabled.isSelected()) {
                    ((AlarmClientLeaf) item).setEnabled(false);
                }
            });

            latching = new CheckBox("Latch");
            latching.setTooltip(new Tooltip("Latch alarm until acknowledged?"));
            latching.setSelected(leaf.isLatching());
            latching.disableProperty().bind(itemEnabled.not());

            annunciating = new CheckBox("Annunciate");
            annunciating.setTooltip(new Tooltip("Request audible alarm annunciation (using the description)?"));
            annunciating.setSelected(leaf.isAnnunciating());
            annunciating.disableProperty().bind(itemEnabled.not());

            layout.add(new HBox(10, enabled, latching, annunciating), 1, row++);

            layout.add(new Label("Disable until:"), 0, row);
            enabled_date_picker = new DateTimePicker();
            enabled_date_picker.setTooltip(new Tooltip("Select a date until which the alarm should be disabled"));
            enabled_date_picker.setDateTimeValue(leaf.getEnabledDate());
            enabled_date_picker.setPrefSize(280, 25);
            enabled_date_picker.setDisable(!leaf.isEnabled());
            enabled_date_picker.disableProperty().bind(itemEnabled.not());


            enabled_date_picker.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    try {
                        // Test that the input is well-formed (if the input
                        // isn't well-formed, valueConverter.fromString()
                        // throws a DateTimeParseException):
                        TextFormatter<?> textFormatter = enabled_date_picker.getEditor().getTextFormatter();
                        StringConverter valueConverter = textFormatter.getValueConverter();
                        LocalDate dateTime = (LocalDate) valueConverter.fromString(enabled_date_picker.getEditor().getText());

                        enabled_date_picker.getEditor().commitValue();
                    }
                    catch (DateTimeParseException dateTimeParseException) {
                        // The input was not well-formed. Prevent further
                        // processing by consuming the key-event:
                        keyEvent.consume();
                    }
                }
            });

            relative_date = new ComboBox<>();
            relative_date.setTooltip(new Tooltip("Select a predefined duration for disabling the alarm"));
            relative_date.getItems().addAll(AlarmSystem.shelving_options);
            relative_date.setPrefSize(200, 25);
            relative_date.setDisable(!leaf.isEnabled());
            relative_date.disableProperty().bind(itemEnabled.not());

            final EventHandler<ActionEvent> relative_event_handler = (ActionEvent e) ->
            {
                enabled.setSelected(false);
                enabled_date_picker.getEditor().clear();
            };

            relative_date.setOnAction(relative_event_handler);

            // setOnAction for relative date must be set to null as to not trigger event when setting value
            enabled_date_picker.setOnAction((ActionEvent e) ->
            {
                if (enabled_date_picker.getDateTimeValue() != null) {
                    relative_date.setOnAction(null);
                    enabled.setSelected(false);
                    enabled_date_picker.getEditor().commitValue();
                    relative_date.getSelectionModel().clearSelection();
                    relative_date.setValue(null);
                    relative_date.setOnAction(relative_event_handler);
                }
            });

            // Configure date picker to disable selection of all dates in the past.
            enabled_date_picker.setDayCellFactory(picker -> new DateCell() {
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    LocalDate today = LocalDate.now();
                    setDisable(empty || date.isBefore(today));
                }
            });

            final HBox until_box = new HBox(10, enabled_date_picker, relative_date);
            until_box.setAlignment(Pos.CENTER);
            HBox.setHgrow(relative_date, Priority.ALWAYS);
            layout.add(until_box, 1, row++);

            layout.add(new Label("Alarm Delay [seconds]:"), 0, row);
            delay = new Spinner<>(0, Integer.MAX_VALUE, leaf.getDelay());
            final Tooltip delay_tt = new Tooltip();
            delay_tt.setShowDuration(Duration.seconds(30));
            delay_tt.setOnShowing(event ->
            {
                final int seconds = leaf.getDelay();
                final String detail;
                if (seconds <= 0)
                    detail = "With the current delay of 0 seconds, alarms trigger immediately";
                else {
                    final String hhmmss = SecondsParser.formatSeconds(seconds);
                    detail = "With the current delay of " + seconds + " seconds, alarms trigger after " + hhmmss + " hours:minutes:seconds";
                }
                delay_tt.setText("Alarms are indicated when they persist for at least this long.\n" + detail);
            });
            delay.setTooltip(delay_tt);
            delay.setEditable(true);
            delay.setPrefWidth(80);
            delay.disableProperty().bind(itemEnabled.not());
            layout.add(delay, 1, row++);

            layout.add(new Label("Alarm Count [within delay]:"), 0, row);
            count = new Spinner<>(0, Integer.MAX_VALUE, leaf.getCount());
            count.setTooltip(new Tooltip("Alarms are indicated when they occur this often within the delay"));
            count.setEditable(true);
            count.setPrefWidth(80);
            count.disableProperty().bind(itemEnabled.not());
            layout.add(count, 1, row++);

            layout.add(new Label("Enabling Filter:"), 0, row);
            filter = new TextField(leaf.getFilter());
            filter.setTooltip(new Tooltip("Optional expression for enabling the alarm"));
            filter.disableProperty().bind(itemEnabled.not());
            layout.add(filter, 1, row++);

            // Initial focus on description
            Platform.runLater(() -> description.requestFocus());
        }

        // Layout has two column
        // The PV-specific items above use two columns.
        // If there's no PV,
        // the following items use one column or span two columns.
        // There must be _something_ in the second column with Hgrow=Always
        // to cause the layout to fill its parent area.
        // 'dummy' is used for that.

        // Guidance:
        layout.add(new Label("Guidance:"), 0, row++, 2, 1);
        guidance = new TitleDetailTable(item.getGuidance());
        guidance.setPrefHeight(100);
        layout.add(guidance, 0, row++, 2, 1);

        // Displays:
        layout.add(new Label("Displays:"), 0, row++, 2, 1);
        displays = new TitleDetailTable(item.getDisplays());
        displays.setPrefHeight(100);
        layout.add(displays, 0, row++, 2, 1);

        // Commands:
        layout.add(new Label("Commands:"), 0, row++, 2, 1);
        commands = new TitleDetailTable(item.getCommands());
        commands.setPrefHeight(100);
        layout.add(commands, 0, row++, 2, 1);

        // Automated Actions:
        layout.add(new Label("Automated Actions:"), 0, row++, 2, 1);
        actions = new TitleDetailDelayTable(item.getActions());
        actions.setPrefHeight(100);
        layout.add(actions, 0, row++, 2, 1);

        // Dialog is quite high; allow scroll
        final ScrollPane scroll = new ScrollPane(layout);

        // Scroll pane stops the content from resizing,
        // so tell content to use the widths of the scroll pane
        // minus 40 to provide space for the scroll bar, and suggest minimum width
        scroll.widthProperty().addListener((p, old, width) -> layout.setPrefWidth(Math.max(width.doubleValue() - 40, 450)));

        getDialogPane().setContent(scroll);
        setResizable(true);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        final Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, event ->
                validateAndStore(model, item, event));

        setResultConverter(button -> button == ButtonType.OK);
    }

    /**
     * Send requested configuration
     *
     * @param model {@link AlarmClient}
     * @param item  Original item
     * @param event Button click event, consumed if save action fails (e.g. Kafka not reachable)
     */
    private void validateAndStore(final AlarmClient model, final AlarmTreeItem<?> item, ActionEvent event) {
        final AlarmTreeItem<?> config;

        if (item instanceof AlarmClientLeaf) {
            final AlarmClientLeaf pv = new AlarmClientLeaf(null, item.getName());

            boolean validEnableDate;
            {
                final LocalDateTime selected_enable_date = enabled_date_picker.getDateTimeValue();
                final String relative_enable_date = relative_date.getValue();

                if ((selected_enable_date != null)) {
                    validEnableDate = pv.setEnabledDate(selected_enable_date);
                } else if (relative_enable_date != null) {
                    final TemporalAmount amount = TimeParser.parseTemporalAmount(relative_enable_date);
                    final LocalDateTime update_date = LocalDateTime.now().plus(amount);
                    validEnableDate = pv.setEnabledDate(update_date);
                } else {
                    pv.setEnabled(itemEnabled.get());
                    validEnableDate = true;
                }
            }

            if (!validEnableDate) {
                Alert prompt = new Alert(Alert.AlertType.INFORMATION);
                prompt.setTitle("'Disable until' is set to a point in time in the past");
                prompt.setHeaderText("'Disable until' is set to a point in time in the past");
                prompt.setContentText("The option 'disable until' must be set to a point in time in the future.");
                DialogHelper.positionDialog(prompt, enabled_date_picker, 0, 0);
                prompt.showAndWait();

                event.consume();
                return;
            }

            pv.setDescription(description.getText().trim());
            pv.setLatching(latching.isSelected());
            pv.setAnnunciating(annunciating.isSelected());
            pv.setDelay(delay.getValue());
            pv.setCount(count.getValue());
            // TODO Check filter expression
            pv.setFilter(filter.getText().trim());

            config = pv;
        } else
            config = new AlarmClientNode(null, item.getName());
        config.setGuidance(guidance.getItems());
        config.setDisplays(displays.getItems());
        config.setCommands(commands.getItems());
        config.setActions(actions.getItems());

        try {
            model.sendItemConfigurationUpdate(item.getPathName(), config);
        } catch (Exception ex) {
            ExceptionDetailsErrorDialog.openError("Error", "Cannot update item", ex);
            event.consume();
        }
    }
}