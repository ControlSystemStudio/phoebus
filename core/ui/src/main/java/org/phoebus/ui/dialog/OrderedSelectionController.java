package org.phoebus.ui.dialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.JFXUtil;

import java.util.List;
import java.util.stream.Collectors;

public class OrderedSelectionController {

    public static final ObservableList<String> available = FXCollections.observableArrayList();
    public static final ObservableList<String> selected = FXCollections.observableArrayList();

    private static final ImageView up = ImageCache.getImageView(JFXUtil.class, "/icons/up.png");
    private static final ImageView down = ImageCache.getImageView(JFXUtil.class, "/icons/down.png");
    private static final ImageView right = ImageCache.getImageView(JFXUtil.class, "/icons/right.png");
    private static final ImageView left = ImageCache.getImageView(JFXUtil.class, "/icons/left.png");

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
        // Text and Graphic initialization
        moveUp.setText("  Up  ");
        moveUp.setGraphic(up);
        moveDown.setText("Down");
        moveDown.setGraphic(down);

        moveLeft.setGraphic(left);
        moveLeft.setText("");
        moveRight.setGraphic(right);
        moveRight.setText("");

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
            moveRight();
        }
    }

    /**
     * Moves the selected item/s from the available list to the selected list
     */
    @FXML
    public void moveRight() {
        ObservableList<String> selectedItems = availableOptions.getSelectionModel().getSelectedItems();
        selected.addAll(selectedItems);
        available.removeAll(selectedItems);

    }

    /**
     * Moves the selected item up the order in the selected list
     */
    @FXML
    public void moveUp() {
        String selection = selectedOptions.getSelectionModel().getSelectedItem();
        if (selection != null) {
            int selectionIndex = selected.indexOf(selection);
            if(selectionIndex >= 1) {
                String target = selected.get(selectionIndex - 1);
                selected.set(selectionIndex - 1, selection);
                selected.set(selectionIndex, target);
                selectedOptions.getSelectionModel().select(selection);
            }
        }
    }

    /**
     * Moves the selected item down the order in the selected list
     */
    @FXML
    public void moveDown() {
        String selection = selectedOptions.getSelectionModel().getSelectedItem();
        if (selection != null) {
            int selectionIndex = selected.indexOf(selection);
            if(selected.size() > 1 && selectionIndex < selected.size() - 1) {
                String target = selected.get(selectionIndex + 1);
                selected.set(selectionIndex + 1, selection);
                selected.set(selectionIndex, target);
                selectedOptions.getSelectionModel().select(selection);
            }
        }
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
