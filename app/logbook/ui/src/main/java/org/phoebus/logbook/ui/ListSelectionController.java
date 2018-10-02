package org.phoebus.logbook.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.phoebus.ui.javafx.ImageCache;
import org.python.modules.synchronize;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

public class ListSelectionController {
    /* Non NLS Strings */
    private static final String ADD_ICON = "/icons/add.png";
    private static final String CLEAR_ICON = "/icons/remove_multiple.png";
    private static final String REMOVE_ICON = "/icons/delete.png";

    @FXML
    private ListView<String> availableItems;
    @FXML
    private ListView<String> selectedItems;

    @FXML
    Button add;
    @FXML
    Button remove;
    @FXML
    Button clear;

    @FXML
    TextField searchField;

    @FXML
    private Button closeButton;
    @FXML
    private Button applyButton;

    // List of available items
    private ObservableList<String> available = FXCollections.observableArrayList();
    // List of selected items
    private ObservableList<String> selected = FXCollections.observableArrayList();

    private List<Function<List<String>, Boolean>> onClose = new ArrayList<>();

    public synchronized void setAvailable(List<String> available) {
        this.available = FXCollections.observableArrayList(available);
        refresh();
    }

    public synchronized void setSelected(List<String> selected) {
        this.selected = FXCollections.observableArrayList(selected);
        refresh();
    }

    public synchronized void setOnClose(Function<List<String>, Boolean> onCloseAction) {
        onClose.add(onCloseAction);
    }

    public List<String> getSelectedItems() {
        return FXCollections.unmodifiableObservableList(selectedItems.getItems());
    }

    @FXML
    public void initialize() {
        searchField.setTooltip(new Tooltip(Messages.SearchAvailableItems));
        searchField.textProperty().addListener((changeListener, oldVal, newVal) -> {
            searchAvailableItemsForSubstring(newVal);
        });
        add.setTooltip(new Tooltip(Messages.Add_Tooltip));
        add.disableProperty().bind(Bindings.isEmpty(availableItems.getSelectionModel().getSelectedItems()));
        add.setGraphic(ImageCache.getImageView(ImageCache.class, ADD_ICON));
        remove.setTooltip(new Tooltip(Messages.Remove_Tooltip));
        remove.disableProperty().bind(Bindings.isEmpty(selectedItems.getSelectionModel().getSelectedItems()));
        remove.setGraphic(ImageCache.getImageView(ImageCache.class, REMOVE_ICON));
        clear.setTooltip(new Tooltip(Messages.Clear_Tooltip));
        clear.disableProperty().bind(Bindings.isEmpty(selectedItems.getItems()));
        clear.setGraphic(ImageCache.getImageView(ImageCache.class, CLEAR_ICON));

        // Double click to add..
        availableItems.setOnMouseClicked(event -> {
            if (event.getClickCount() < 2)
                return;
            addSelected();
            event.consume();
        });
        // .. or remove items
        selectedItems.setOnMouseClicked(event -> {
            if (event.getClickCount() < 2)
                return;
            removeSelected();
            event.consume();
        });

        refresh();
    }

    void refresh() {
        availableItems.setItems(available);
        selectedItems.setItems(selected);
    }

    private void searchAvailableItemsForSubstring(final String substring) {
        if (substring.trim().isEmpty())
            availableItems.getSelectionModel().clearSelection();
        else {
            // Case insensitive, support unicode...
            Pattern pattern = Pattern.compile(Pattern.quote(substring),
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            int size = availableItems.getItems().size();
            for (int i = 0; i < size; i++) {
                final String item = availableItems.getItems().get(i);
                if (pattern.matcher(item).find())
                    availableItems.getSelectionModel().select(i);
                else
                    availableItems.getSelectionModel().clearSelection(i);
            }
        }
    }

    @FXML
    public void addSelected() {
        // defensive copy
        ObservableList<String> selectedItemsForAddition = availableItems.getSelectionModel().getSelectedItems();
        Platform.runLater(() -> {
            selectedItems.getItems().addAll(selectedItemsForAddition);
            availableItems.getItems().removeAll(selectedItemsForAddition);
            clearSelections();
        });
    }

    @FXML
    public void removeSelected() {
        ObservableList<String> selectedItemsForRemoval = selectedItems.getSelectionModel().getSelectedItems();
        Platform.runLater(() -> {
            selectedItems.getItems().removeAll(selectedItemsForRemoval);
            availableItems.getItems().addAll(selectedItemsForRemoval);
            clearSelections();
        });
    }

    @FXML
    public void clearSelected() {
        refresh();
        clearSelections();
    }

    @FXML
    private void closeButtonAction(){
        // get a handle to the stage
        Stage stage = (Stage) closeButton.getScene().getWindow();
        // do what you have to do
        stage.close();
    }
    

    @FXML
    private void applyButtonAction(){
        onClose.forEach((function)->{
            function.apply(getSelectedItems());
        });
        closeButtonAction();
    }
    
    private void clearSelections() {
        selectedItems.getSelectionModel().clearSelection();
        availableItems.getSelectionModel().clearSelection();
    }

}
