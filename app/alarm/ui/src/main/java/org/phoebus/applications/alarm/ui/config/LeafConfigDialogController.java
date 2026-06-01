/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */
package org.phoebus.applications.alarm.ui.config;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.util.time.SecondsParser;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.LocalDateTime;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML controller for LeafConfigDialog.fxml. Intended for configuration
 * of alarm tree <i>leaf</i> items.
 */
@SuppressWarnings("nls")
public class LeafConfigDialogController extends ConfigDialogController {

    @SuppressWarnings("unused")
    @FXML
    private TextField description;
    @SuppressWarnings("unused")
    @FXML
    private HBox behaviorBox;
    @SuppressWarnings("unused")
    @FXML
    private CheckBox latching;
    @SuppressWarnings("unused")
    @FXML
    private CheckBox annunciating;
    @SuppressWarnings("unused")
    @FXML
    private Spinner<Integer> delay;
    @SuppressWarnings("unused")
    @FXML
    private Spinner<Integer> count;
    @SuppressWarnings("unused")
    @FXML
    private TextField filter;

    private final SimpleStringProperty descriptionProperty = new SimpleStringProperty("");
    private final SimpleBooleanProperty latchingProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty annunciatingProperty = new SimpleBooleanProperty();
    private final SimpleStringProperty enablingFilterProperty = new SimpleStringProperty("");

    private SpinnerValueFactory<Integer> countValueFactory;
    private SpinnerValueFactory<Integer> delayValueFactory;

    public LeafConfigDialogController(AlarmClient alarmClient, AlarmTreeItem<?> alarmTreeItem) {
        super(alarmClient, alarmTreeItem);
    }

    @SuppressWarnings("unused")
    @FXML
    public void initialize() {

        super.initialize();

        description.textProperty().bindBidirectional(descriptionProperty);
        latching.selectedProperty().bindBidirectional(latchingProperty);
        annunciating.selectedProperty().bindBidirectional(annunciatingProperty);
        filter.textProperty().bindBidirectional(enablingFilterProperty);

        relativeDate.disableProperty().bind(itemEnabledProperty.not());
        enabledDatePicker.disableProperty().bind(itemEnabledProperty.not());

        AlarmClientLeaf leaf = (AlarmClientLeaf) alarmTreeItem;

        // Filter to disallow anything but numbers in the spinner
        UnaryOperator<TextFormatter.Change> integerFilter = c -> {
            if (c.isContentChange()) {
                ParsePosition parsePosition = new ParsePosition(0);
                // NumberFormat evaluates the beginning of the text
                NumberFormat.getIntegerInstance().parse(c.getControlNewText(), parsePosition);
                if (parsePosition.getIndex() == 0 || parsePosition.getIndex() < c.getControlNewText().length()) {
                    // reject parsing the complete text failed
                    return null;
                }
            }
            return c;
        };

        countValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, leaf.getCount(), 1);
        TextFormatter<Integer> countSpinnerFormatter = new TextFormatter<>(countValueFactory.getConverter(), countValueFactory.getValue(), integerFilter);
        countValueFactory.valueProperty().bindBidirectional(countSpinnerFormatter.valueProperty());
        count.getEditor().setTextFormatter(countSpinnerFormatter);
        count.setValueFactory(countValueFactory);

        delayValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, leaf.getDelay(), 1);
        TextFormatter<Integer> delaySpinnerFormatter = new TextFormatter<>(delayValueFactory.getConverter(), delayValueFactory.getValue(), integerFilter);
        delayValueFactory.valueProperty().bindBidirectional(delaySpinnerFormatter.valueProperty());
        delay.getEditor().setTextFormatter(delaySpinnerFormatter);
        delay.setValueFactory(delayValueFactory);

        descriptionProperty.set(leaf.getDescription());
        enablingFilterProperty.set(leaf.getFilter());
        enableDateProperty.set(leaf.getEnabledDate());

        // Behavior checkboxes
        itemEnabledProperty.setValue(leaf.isEnabled());
        enabled.selectedProperty().set(leaf.isEnabled());
        latchingProperty.setValue(leaf.isLatching());
        annunciatingProperty.setValue(leaf.isAnnunciating());

        BooleanBinding binding = Bindings.createBooleanBinding(() ->
                        itemEnabledProperty.not().get() || relativeDateProperty.isNotNull().get() || enableDateProperty.isNotNull().get(),
                itemEnabledProperty, relativeDateProperty, enableDateProperty);

        latching.disableProperty().bind(binding);
        annunciating.disableProperty().bind(binding);
        count.disableProperty().bind(binding);
        delay.disableProperty().bind(binding);
        filter.disableProperty().bind(binding);

        // Delay spinner
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

        // Initial focus
        Platform.runLater(() -> description.requestFocus());
    }

    /**
     * Validates input and sends the configuration off to the message broker.
     */
    @Override
    public void validateAndStore() {

        final AlarmClientLeaf pv = new AlarmClientLeaf(null, alarmTreeItem.getName());

        LocalDateTime enableDate;
        try {
            enableDate = determineEnableDate();
        } catch (Exception e) {
            Logger.getLogger(LeafConfigDialogController.class.getName())
                    .log(Level.WARNING, "Invalid enable date specified", e);
            return;
        }
        if (enableDate != null) {
            pv.setEnabledDate(enableDate);
        } else {
            pv.setEnabled(itemEnabledProperty.get());
        }

        pv.setDescription(descriptionProperty.get());
        pv.setLatching(latchingProperty.get());
        pv.setAnnunciating(annunciatingProperty.get());
        pv.setDelay(delayValueFactory.getValue());
        pv.setCount(countValueFactory.getValue());
        // TODO Check filter expression
        pv.setFilter(enablingFilterProperty.getValue());

        pv.setGuidance(optionsTablesViewController.getGuidance());
        pv.setDisplays(optionsTablesViewController.getDisplays());
        pv.setCommands(optionsTablesViewController.getCommands());
        pv.setActions(optionsTablesViewController.getActions());

        try {
            alarmClient.sendItemConfigurationUpdate(alarmTreeItem.getPathName(), pv);
        } catch (Exception ex) {
            ExceptionDetailsErrorDialog.openError("Error", "Cannot update item", ex);
        }
    }
}
