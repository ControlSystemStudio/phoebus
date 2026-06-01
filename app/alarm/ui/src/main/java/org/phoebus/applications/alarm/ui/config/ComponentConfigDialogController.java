/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */
package org.phoebus.applications.alarm.ui.config;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.applications.alarm.ui.tree.ComponentActionHelper;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML controller for LeafConfigDialog.fxml.
 */
@SuppressWarnings("nls")
public class ComponentConfigDialogController extends ConfigDialogController {


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
    private Label disableUntilLabel;
    @SuppressWarnings("unused")
    @FXML
    private ComboBox<String> relativeDate;

    @SuppressWarnings("unused")
    @FXML
    private DateTimePicker enabledDatePicker;

    @SuppressWarnings("unused")
    @FXML
    private Label partlyDisabledLabel;

    private List<AlarmClientLeaf> alarmClientLeaves;

    public ComponentConfigDialogController(AlarmClient alarmClient, AlarmTreeItem<?> alarmTreeItem) {
        super(alarmClient, alarmTreeItem);
    }

    @SuppressWarnings("unused")
    @FXML
    public void initialize() {

        super.initialize();

        alarmClientLeaves = new ArrayList<>();
        List<AlarmClientLeaf> disabled = new ArrayList<>();
        List<AlarmClientLeaf> withEnableDate = new ArrayList<>();
        // Check subtree for disabled PVs and PVs with non-null enable date
        findAffectedPVs(alarmTreeItem, alarmClientLeaves, disabled, withEnableDate);

        if(disabled.isEmpty()) {
            enabled.setSelected(true);
            itemEnabledProperty.setValue(true);
        }
        else if (alarmClientLeaves.size() != disabled.size()) {
            partlyDisabledLabel.setVisible(true);
            enabled.setSelected(false);
            itemEnabledProperty.setValue(false);
        }

        if (!withEnableDate.isEmpty()) {
            relativeDate.setDisable(true);
            enabledDatePicker.setDisable(true);
        }
    }

    /**
     * Validates input and sends the configuration off to the message broker.
     *
     */
    public void validateAndStore() {

        // First check if user has specified a valid enable date
        LocalDateTime enableDate;
        try {
            enableDate = determineEnableDate();
        } catch (Exception e) {
            Logger.getLogger(LeafConfigDialogController.class.getName())
                    .log(Level.WARNING, "Invalid enable date specified", e);
            return;
        }

        alarmTreeItem.setGuidance(optionsTablesViewController.getGuidance());
        alarmTreeItem.setDisplays(optionsTablesViewController.getDisplays());
        alarmTreeItem.setCommands(optionsTablesViewController.getCommands());
        alarmTreeItem.setActions(optionsTablesViewController.getActions());

        try {
            alarmClient.sendItemConfigurationUpdate(alarmTreeItem.getPathName(), alarmTreeItem);
        } catch (Exception ex) {
            ExceptionDetailsErrorDialog.openError("Error", "Cannot update item", ex);
            return;
        }

        // Lastly update enable or - if non-null - set enable date.
        if (enableDate != null) {
            updateEnablement(enableDate);
        } else {
            ComponentActionHelper.updateEnablement(scroll, alarmClient, List.of(alarmTreeItem), itemEnabledProperty.get());
        }
    }

    /**
     * Updates a component to disable a hierarchy of PVs with an enable date.
     *
     * @param enableDate The {@link LocalDateTime} to set on all leaf nodes specified in <code>items</code>   .
     */
    private void updateEnablement(LocalDateTime enableDate) {
        if (alarmClientLeaves.isEmpty()) {
            return;
        }
        if (alarmClientLeaves.size() > 1) {
            final Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
            dialog.setTitle(Messages.disableAlarms);
            dialog.setHeaderText(MessageFormat.format(Messages.headerConfirmDisableWithEnableDate, enableDate, alarmClientLeaves.size()));

            DialogHelper.positionDialog(dialog, scroll, -50, -25);
            if (dialog.showAndWait().get() != ButtonType.OK) {
                return;
            }
        }

        JobManager.schedule(Messages.disableAlarms, monitor ->
        {
            for (AlarmClientLeaf pv : alarmClientLeaves) {
                final AlarmClientLeaf copy = pv.createDetachedCopy();
                if (copy.setEnabledDate(enableDate)) {
                    try {
                        alarmClient.sendItemConfigurationUpdate(pv.getPathName(), copy);
                    } catch (Exception e) {
                        ExceptionDetailsErrorDialog.openError(Messages.error,
                                Messages.disableAlarmFailed,
                                e);
                        throw e;
                    }
                }
            }
        });
    }

    /**
     * Recursively counts alarm tree items in a subtree to find total number, number of disabled, and
     * number of disabled with enable date.
     *
     * @param item           Root item
     * @param total          {@link AtomicInteger} that will hold the total number of leaf nodes
     * @param disabled       {@link AtomicInteger} that will hold the number of disabled leaf nodes (with or without enable date)
     * @param withEnableDate {@link AtomicInteger} that will hold the number of leaf nodes with non-null enable date
     *
     */
    public static void findAffectedPVs(final AlarmTreeItem<?> item, final List<AlarmClientLeaf> total, final List<AlarmClientLeaf> disabled, final List<AlarmClientLeaf> withEnableDate) {
        if (item instanceof AlarmClientLeaf) {
            final AlarmClientLeaf pv = (AlarmClientLeaf) item;
            total.add(pv);
            if (!pv.isEnabled()) {
                disabled.add(pv);
                if (pv.getEnabledDate() != null) {
                    withEnableDate.add(pv);
                }
            }
        } else {
            for (AlarmTreeItem<?> sub : item.getChildren()) {
                findAffectedPVs(sub, total, disabled, withEnableDate);
            }
        }
    }
}
