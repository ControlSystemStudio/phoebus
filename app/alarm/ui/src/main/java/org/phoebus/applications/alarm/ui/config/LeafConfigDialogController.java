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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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
import java.util.function.UnaryOperator;


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

    @FXML
    protected CheckBox enabled;

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


    private final SimpleStringProperty descriptionProperty = new SimpleStringProperty("");
    private final SimpleBooleanProperty latchingProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty annunciatingProperty = new SimpleBooleanProperty();
    private final SimpleStringProperty enablingFilterProperty = new SimpleStringProperty("");

    private SpinnerValueFactory<Integer> countValueFactory;
    private SpinnerValueFactory<Integer> delayValueFactory;

    protected final SimpleBooleanProperty itemEnabledProperty = new SimpleBooleanProperty();


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



                enabled.setOnAction(e -> {
            itemEnabledProperty.setValue(enabled.isSelected());
        });


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

        // Behavior checkboxes
        itemEnabledProperty.setValue(leaf.isEnabled());
        enabled.selectedProperty().set(leaf.isEnabled());
        latchingProperty.setValue(leaf.isLatching());
        annunciatingProperty.setValue(leaf.isAnnunciating());


        BooleanBinding binding = Bindings.createBooleanBinding(() ->
                        itemEnabledProperty.not().get(), itemEnabledProperty);

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


        pv.setDescription(descriptionProperty.get());
        pv.setEnabled(itemEnabledProperty.get());
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
