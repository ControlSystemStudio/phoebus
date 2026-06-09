/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.alarm.ui.config;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller for the buttons controlling the guidance, displays... tables. Action handlers will simply
 * forward to methods in a {@link TitleDetailTableController}.
 */
@SuppressWarnings("unused")
public class TitleDetailToolbarController {

    private TitleDetailTableController titleDetailTableController;

    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button upButton;
    @FXML
    private Button downButton;
    @FXML
    private Button deleteButton;

    public void setTitleDetailTableController(TitleDetailTableController titleDetailTableController) {
        this.titleDetailTableController = titleDetailTableController;
    }

    @FXML
    private void handleAdd() {
        titleDetailTableController.handleAdd();
    }

    @FXML
    private void handleEdit() {
        titleDetailTableController.handleEdit();
    }

    @FXML
    private void handleUp() {
        titleDetailTableController.handleUp();
    }

    @FXML
    private void handleDown() {
        titleDetailTableController.handleDown();
    }

    @FXML
    private void handleDelete() {
        titleDetailTableController.handleDelete();
    }

    /**
     * Configures the button states based on the number of selected items in the {@link javafx.scene.control.TableView}.
     * @param numberOfSelectedItems
     */
    public void setButtonStates(int numberOfSelectedItems){
        final boolean nothing = numberOfSelectedItems <= 0;
        upButton.setDisable(nothing);
        editButton.setDisable(numberOfSelectedItems != 1);
        downButton.setDisable(nothing);
        deleteButton.setDisable(nothing);
    }
}
