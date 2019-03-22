package org.phoebus.channel.views.ui;

import java.util.Collection;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;

public class SelectEntityController {
    public static final ObservableList<String> avialable = FXCollections.observableArrayList();
    StringProperty selectedEntity = new SimpleStringProperty();

    @FXML
    ListView<String> availableEntities;

    @FXML
    TextField selectedOptionText;

    @FXML
    public void initialize() {
        availableEntities.setItems(avialable);
        availableEntities.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        selectedOptionText.textProperty().bindBidirectional(selectedEntity);
    }

    @FXML
    public void setSelection() {
        ObservableList<String> selectedItems = availableEntities.getSelectionModel().getSelectedItems();
        selectedEntity.set(selectedItems.get(0));
    }

    public void setAvaibleOptions(Collection<String> options) {
        avialable.clear();
        avialable.addAll(options.stream().sorted().collect(Collectors.toList()));
    }

    public String getSelectedOption() {
        return selectedEntity.get();
    }

}
