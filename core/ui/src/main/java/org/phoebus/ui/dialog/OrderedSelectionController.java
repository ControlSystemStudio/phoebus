package org.phoebus.ui.dialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.stream.Collectors;

public class OrderedSelectionController {

    public static final ObservableList<String> available = FXCollections.observableArrayList();
    public static final ObservableList<String> selected = FXCollections.observableArrayList();

    @FXML
    ListView<String> availableOptions;

    @FXML
    ListView<String> selectedOptions;

    @FXML
    Button moveLeft;

    @FXML
    Button moveUp;

    @FXML
    Button moveDown;

    @FXML
    Button moveRight;

    @FXML
    public void initialize() {
        availableOptions.setItems(available);
        selectedOptions.setItems(selected);
    }

    /**
     * Moves the selected item/s from the selected list to the available list
     */
    @FXML
    public void doubleClickDeselect(MouseEvent e) {
        if (e.getClickCount() == 2) {
            moveLeft();
        }
    }

    /**
     * Moves the selected item/s from the selected list to the available list
     */
    @FXML
    public void moveLeft() {
        ObservableList<String> selectedItems = selectedOptions.getSelectionModel().getSelectedItems();
        available.addAll(selectedItems);
        selected.removeAll(selectedItems);
    }

    /**
     * Moves the selected item/s from the available list to the selected list
     */
    @FXML
    public void doubleClickSelect(MouseEvent e) {
        if (e.getClickCount() == 2) {
            moveRigth();
        }
    }

    /**
     * Moves the selected item/s from the available list to the selected list
     */
    @FXML
    public void moveRigth() {
        ObservableList<String> selectedItems = availableOptions.getSelectionModel().getSelectedItems();
        selected.addAll(selectedItems);
        available.removeAll(selectedItems);

    }

    /**
     * Moves the selected item up the order in the selected list
     */
    @FXML
    public void moveUp() {

    }

    /**
     * Moves the selected item down the order in the selected list
     */
    @FXML
    public void moveDown() {

    }

    public void setAvailableOptions(List<String> availableOptions) {
        available.clear();
        selected.clear();
        available.addAll(availableOptions);
    }

    public void setOrderedSelectedOptions(List<String> selectedOptions) {
        selected.clear();
        available.removeAll(selectedOptions);
        selected.addAll(selectedOptions);
    }

    public List<String> getOrderedSelectedOptions() {
        return selected.stream().collect(Collectors.toList());
    }
}
