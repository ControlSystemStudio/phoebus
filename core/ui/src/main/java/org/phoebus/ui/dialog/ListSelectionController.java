package org.phoebus.ui.dialog;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.phoebus.ui.Messages;
import org.phoebus.ui.javafx.ImageCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

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
    private Button cancelButton;
    @FXML
    private Button applyButton;

    // List of available items
    private ObservableList<String> available = FXCollections.observableArrayList();
    private FilteredList<String> filteredAvailable = new FilteredList<>(available);

    // List of selected items
    private ObservableList<String> selected = FXCollections.observableArrayList();

    private List<Function<List<String>, Boolean>> onApply = new ArrayList<>();
    private List<Function<List<String>, Boolean>> onCancel = new ArrayList<>();

    public synchronized void setAvailable(List<String> available) {
        // Remove already selected items.
        available.removeAll(selected);
        this.available.setAll(available);// = FXCollections.observableArrayList(available);
        refresh();
    }

    public synchronized void setSelected(List<String> selected) {
        this.selected = FXCollections.observableArrayList(selected);
        refresh();
    }

    public synchronized void setOnApply(Function<List<String>, Boolean> onApplyAction) {
        onApply.add(onApplyAction);
    }

    public synchronized void setOnCancel(Function<List<String>, Boolean> onCancelAction) {
        onCancel.add(onCancelAction);
    }

    public List<String> getSelectedItems() {
        return FXCollections.unmodifiableObservableList(selectedItems.getItems());
    }

    @FXML
    public void initialize() {

        searchField.setTooltip(new Tooltip(Messages.SearchAvailableItems));
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = searchField.getText();
            filteredAvailable.setPredicate(buildSearchFilterPredicate(filter));
        });
        add.setTooltip(new Tooltip(Messages.Add_Tooltip));
        add.setGraphic(ImageCache.getImageView(ImageCache.class, ADD_ICON));
        remove.setTooltip(new Tooltip(Messages.Remove_Tooltip));
        remove.setGraphic(ImageCache.getImageView(ImageCache.class, REMOVE_ICON));
        clear.setTooltip(new Tooltip(Messages.Clear_Tooltip));
        clear.setGraphic(ImageCache.getImageView(ImageCache.class, CLEAR_ICON));

        Platform.runLater(() -> {
            add.disableProperty().bind(Bindings.isEmpty(availableItems.getSelectionModel().getSelectedItems()));
            remove.disableProperty().bind(Bindings.isEmpty(selectedItems.getSelectionModel().getSelectedItems()));
            clear.disableProperty().bind(Bindings.isEmpty(selectedItems.getItems()));
        });

        availableItems.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // Double click to add..
        availableItems.setOnMouseClicked(event -> {
            if (event.getClickCount() < 2)
                return;
            addSelected();
            event.consume();
        });
        selectedItems.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
        availableItems.setItems(filteredAvailable);
        selectedItems.setItems(selected);
    }

    @FXML
    public void addSelected() {
        List<String> selectedItemsForAddition = Arrays.asList((availableItems.getSelectionModel().getSelectedItems()
                .toArray(new String[availableItems.getSelectionModel().getSelectedItems().size()])));
        selected.addAll(selectedItemsForAddition);
        available.removeAll(selectedItemsForAddition);
        clearSelections();
    }

    @FXML
    public void removeSelected() {
        List<String> selectedItemsForRemoval = Arrays.asList((selectedItems.getSelectionModel().getSelectedItems()
                .toArray(new String[selectedItems.getSelectionModel().getSelectedItems().size()])));
        selected.removeAll(selectedItemsForRemoval);
        available.addAll(selectedItemsForRemoval);
        clearSelections();
    }

    @FXML
    public void clearSelected() {
        List<String> selectedItemsForRemoval = Arrays.asList((selectedItems.getItems()
                .toArray(new String[selectedItems.getItems().size()])));
        selected.removeAll(selectedItemsForRemoval);
        available.addAll(selectedItemsForRemoval);
        clearSelections();
    }

    @FXML
    private void closeButtonAction() {
        onCancel.forEach((function) -> {
            function.apply(getSelectedItems());
        });
    }

    @FXML
    private void applyButtonAction() {
        onApply.forEach((function) -> {
            function.apply(getSelectedItems());
        });
    }

    private Predicate<String> buildSearchFilterPredicate(String filter) {
        if(filter == null || filter.isBlank()) {
            return null;
        }
        Pattern pattern = Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return item -> pattern.matcher(item).find();
    }

    private void clearSelections() {
        selectedItems.getSelectionModel().clearSelection();
        availableItems.getSelectionModel().clearSelection();
    }

}
