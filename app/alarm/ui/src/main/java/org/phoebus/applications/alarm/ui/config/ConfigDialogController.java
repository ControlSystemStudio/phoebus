/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.alarm.ui.config;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

public class ConfigDialogController {

    @SuppressWarnings("unused")
    @FXML
    private ScrollPane scroll;
    @SuppressWarnings("unused")
    @FXML
    private GridPane layout;


    @SuppressWarnings("unused")
    @FXML
    private TextField path;


    @FXML
    protected OptionsTablesController optionsTablesViewController;

    protected final AlarmClient alarmClient;
    protected final AlarmTreeItem<?> alarmTreeItem;


    public ConfigDialogController(AlarmClient alarmClient, AlarmTreeItem<?> alarmTreeItem) {
        this.alarmClient = alarmClient;
        this.alarmTreeItem = alarmTreeItem;
    }

    @FXML
    public void initialize() {

        path.setText(alarmTreeItem.getPathName());

//        // ── Scroll-pane width listener ────────────────────────────────────────
        scroll.widthProperty().addListener((p, old, width) ->
                layout.setPrefWidth(Math.max(width.doubleValue() - 40, 450)));
    }


    public void validateAndStore(){
        alarmTreeItem.setGuidance(optionsTablesViewController.getGuidance());
        alarmTreeItem.setDisplays(optionsTablesViewController.getDisplays());
        alarmTreeItem.setCommands(optionsTablesViewController.getCommands());
        alarmTreeItem.setActions(optionsTablesViewController.getActions());

        try {
            alarmClient.sendItemConfigurationUpdate(alarmTreeItem.getPathName(), alarmTreeItem);
        } catch (Exception ex) {
            ExceptionDetailsErrorDialog.openError("Error", "Cannot update item", ex);
        }
    }
}
