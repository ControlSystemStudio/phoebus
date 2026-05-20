/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */
package org.phoebus.applications.alarm.ui.config;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.applications.alarm.ui.tree.TitleDetailDelayTable;
import org.phoebus.applications.alarm.ui.tree.TitleDetailTable;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.util.time.SecondsParser;
import org.phoebus.util.time.TimeParser;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;

/**
 * FXML controller for ItemConfigDialog.fxml.
 */
@SuppressWarnings("nls")
public class ItemConfigDialogController {

    // ── FXML-injected fields ──────────────────────────────────────────────────

    @SuppressWarnings("unused")
    @FXML
    private ScrollPane scroll;
    @SuppressWarnings("unused")
    @FXML
    private javafx.scene.layout.GridPane layout;

    // Path row (always visible)
    @SuppressWarnings("unused")
    @FXML
    private TextField path;

    // Leaf-only rows
    @SuppressWarnings("unused")
    @FXML
    private Label descriptionLabel;
    @SuppressWarnings("unused")
    @FXML
    private TextField description;

    @SuppressWarnings("unused")
    @FXML
    private Label behaviorLabel;
    @SuppressWarnings("unused")
    @FXML
    private HBox behaviorBox;
    @SuppressWarnings("unused")
    @FXML
    private CheckBox enabled;
    @SuppressWarnings("unused")
    @FXML
    private CheckBox latching;
    @SuppressWarnings("unused")
    @FXML
    private CheckBox annunciating;

    @SuppressWarnings("unused")
    @FXML
    private Label disableUntilLabel;
    @SuppressWarnings("unused")
    @FXML
    private ComboBox<String> relativeDate;

    @SuppressWarnings("unused")
    @FXML
    private Label delayLabel;
    @SuppressWarnings("unused")
    @FXML
    private Spinner<Integer> delay;
    @SuppressWarnings("unused")
    @FXML
    private Label countLabel;
    @SuppressWarnings("unused")
    @FXML
    private Spinner<Integer> count;

    @SuppressWarnings("unused")
    @FXML
    private Label filterLabel;
    @SuppressWarnings("unused")
    @FXML
    private TextField filter;

    // Shared table placeholders
    @SuppressWarnings("unused")
    @FXML
    private StackPane guidancePlaceholder;
    @SuppressWarnings("unused")
    @FXML
    private StackPane displaysPlaceholder;
    @SuppressWarnings("unused")
    @FXML
    private StackPane commandsPlaceholder;
    @SuppressWarnings("unused")
    @FXML
    private StackPane actionsPlaceholder;
    @SuppressWarnings("unused")
    @FXML
    private DateTimePicker enabledDatePicker;
    @SuppressWarnings("unused")
    @FXML
    private HBox untilBox;

    private TitleDetailTable guidance;
    private TitleDetailTable displays;
    private TitleDetailTable commands;
    private TitleDetailDelayTable actions;

    private final SimpleBooleanProperty itemEnabled = new SimpleBooleanProperty();

    private final AlarmClient alarmClient;
    private final AlarmTreeItem alarmTreeItem;

    public ItemConfigDialogController(AlarmClient alarmClient, AlarmTreeItem alarmTreeItem) {
        this.alarmClient = alarmClient;
        this.alarmTreeItem = alarmTreeItem;
    }

    @SuppressWarnings("unused")
    @FXML
    public void initialize() {

        path.setText(alarmTreeItem.getPathName());

        // ── Leaf-only section ─────────────────────────────────────────────────
        if (alarmTreeItem instanceof AlarmClientLeaf leaf) {
            itemEnabled.setValue(leaf.isEnabled());

            // Description
            description.setText(leaf.getDescription());

            // Behavior checkboxes
            enabled.setSelected(leaf.isEnabled());
            latching.setSelected(leaf.isLatching());
            annunciating.setSelected(leaf.isAnnunciating());

            latching.disableProperty().bind(itemEnabled.not());
            annunciating.disableProperty().bind(itemEnabled.not());

            itemEnabled.addListener((obs, o, n) -> leaf.setEnabled(n));

            enabledDatePicker.disableProperty().bind(itemEnabled.not());

            // Day-cell factory – disable past dates
            enabledDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(LocalDate.now()));
                }
            });

            // ENTER key handler on the date picker's editor
            enabledDatePicker.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    try {
                        TextFormatter<?> tf = enabledDatePicker.getEditor().getTextFormatter();
                        @SuppressWarnings("unchecked")
                        StringConverter<LocalDate> conv =
                                (StringConverter<LocalDate>) tf.getValueConverter();
                        conv.fromString(enabledDatePicker.getEditor().getText());
                        enabledDatePicker.getEditor().commitValue();
                    } catch (DateTimeParseException ex) {
                        keyEvent.consume();
                    }
                }
            });

            // Relative-date combo
            relativeDate.getItems().addAll(AlarmSystem.shelving_options);
            relativeDate.setDisable(!leaf.isEnabled());
            relativeDate.disableProperty().bind(itemEnabled.not());

            // Relative-date action handler (must be a field so it can be removed temporarily)
            final EventHandler<ActionEvent> relativeEventHandler = e -> {
                enabled.setSelected(false);
                enabledDatePicker.getEditor().clear();
            };
            relativeDate.setOnAction(relativeEventHandler);

            // Date-picker action handler
            enabledDatePicker.setOnAction(e -> {
                if (enabledDatePicker.getDateTimeValue() != null) {
                    relativeDate.setOnAction(null);
                    enabled.setSelected(false);
                    enabledDatePicker.getEditor().commitValue();
                    relativeDate.getSelectionModel().clearSelection();
                    relativeDate.setValue(null);
                    relativeDate.setOnAction(relativeEventHandler);
                }
            });

            // Enabled checkbox action
            enabled.setOnAction(e -> {
                itemEnabled.setValue(enabled.isSelected());
                if (enabled.isSelected()) {
                    relativeDate.getSelectionModel().clearSelection();
                    relativeDate.setValue(null);
                    enabledDatePicker.getEditor().clear();
                    enabledDatePicker.setValue(null);
                }
                if (!enabled.isSelected()) {
                    leaf.setEnabled(false);
                }
            });

            // Delay spinner
            delay.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, leaf.getDelay()));
            delay.setEditable(true);
            delay.setPrefWidth(80);
            delay.disableProperty().bind(itemEnabled.not());
            final Tooltip delayTt = new Tooltip();
            delayTt.setShowDuration(Duration.seconds(30));
            delayTt.setOnShowing(event -> {
                final int seconds = leaf.getDelay();
                final String detail;
                if (seconds <= 0)
                    detail = Messages.delayTooltip1;
                else {
                    detail = MessageFormat.format(Messages.delayTooltip2, seconds, SecondsParser.formatSeconds(seconds));
                }
                delayTt.setText(MessageFormat.format(Messages.delayTooltip0, detail));
            });
            delay.setTooltip(delayTt);

            // Count spinner
            count.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, leaf.getCount()));
            count.setEditable(true);
            count.setPrefWidth(80);
            count.disableProperty().bind(itemEnabled.not());

            // Filter field
            filter.setText(leaf.getFilter());
            filter.disableProperty().bind(itemEnabled.not());

            // Initial focus
            Platform.runLater(() -> description.requestFocus());

        } else {
            // Hide all leaf-only rows when item is not a leaf
            setLeafSectionVisible(false);
        }

        // ── Shared tables (guidance, displays, commands, actions) ─────────────
        guidance = new TitleDetailTable(alarmTreeItem.getGuidance());
        guidance.setPrefHeight(100);
        guidancePlaceholder.getChildren().setAll(guidance);

        displays = new TitleDetailTable(alarmTreeItem.getDisplays());
        displays.setPrefHeight(100);
        displaysPlaceholder.getChildren().setAll(displays);

        commands = new TitleDetailTable(alarmTreeItem.getCommands());
        commands.setPrefHeight(100);
        commandsPlaceholder.getChildren().setAll(commands);

        actions = new TitleDetailDelayTable(alarmTreeItem.getActions());
        actions.setPrefHeight(100);
        actionsPlaceholder.getChildren().setAll(actions);

        // ── Scroll-pane width listener ────────────────────────────────────────
        scroll.widthProperty().addListener((p, old, width) ->
                layout.setPrefWidth(Math.max(width.doubleValue() - 40, 450)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Show or hide every control that belongs to the leaf-only section.
     */
    private void setLeafSectionVisible(boolean visible) {
        for (javafx.scene.Node n : new javafx.scene.Node[]{
                descriptionLabel, description,
                behaviorLabel, behaviorBox,
                disableUntilLabel, untilBox,
                delayLabel, delay,
                countLabel, count,
                filterLabel, filter}) {
            n.setVisible(visible);
            n.setManaged(visible);
        }
    }

    // ── Validation / store ────────────────────────────────────────────────────

    /**
     * Validates input and sends the configuration off to the message broker.
     */
    public void validateAndStore() {
        final AlarmTreeItem<?> config;

        if (alarmTreeItem instanceof AlarmClientLeaf) {
            final AlarmClientLeaf pv = new AlarmClientLeaf(null, alarmTreeItem.getName());

            boolean validEnableDate;
            {
                final LocalDateTime selectedEnableDate = enabledDatePicker.getDateTimeValue();
                final String relativeEnableDate = relativeDate.getValue();

                if (selectedEnableDate != null) {
                    validEnableDate = pv.setEnabledDate(selectedEnableDate);
                } else if (relativeEnableDate != null) {
                    final TemporalAmount amount = TimeParser.parseTemporalAmount(relativeEnableDate);
                    final LocalDateTime updateDate = LocalDateTime.now().plus(amount);
                    validEnableDate = pv.setEnabledDate(updateDate);
                } else {
                    pv.setEnabled(itemEnabled.get());
                    validEnableDate = true;
                }
            }

            if (!validEnableDate) {
                Alert prompt = new Alert(Alert.AlertType.INFORMATION);
                prompt.setTitle(Messages.promptTitle);
                prompt.setHeaderText(Messages.promptTitle);
                prompt.setContentText(Messages.promptContent);
                DialogHelper.positionDialog(prompt, enabledDatePicker, 0, 0);
                prompt.showAndWait();
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
        } else {
            config = new AlarmClientNode(null, alarmTreeItem.getName());
        }

        config.setGuidance(guidance.getItems());
        config.setDisplays(displays.getItems());
        config.setCommands(commands.getItems());
        config.setActions(actions.getItems());

        try {
            alarmClient.sendItemConfigurationUpdate(alarmTreeItem.getPathName(), config);
        } catch (Exception ex) {
            ExceptionDetailsErrorDialog.openError("Error", "Cannot update item", ex);
        }
    }
}
