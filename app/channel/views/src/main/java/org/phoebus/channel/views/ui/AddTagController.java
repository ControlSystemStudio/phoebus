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

public class AddTagController {
    public static final ObservableList<String> avialable = FXCollections.observableArrayList();

    StringProperty selectedTag = new SimpleStringProperty();

    @FXML
    ListView<String> availableTags;

    @FXML
    TextField selectedTagText;

    @FXML
    public void initialize() {
        availableTags.setItems(avialable);
        availableTags.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        selectedTagText.textProperty().bindBidirectional(selectedTag);
    }

    @FXML
    public void setSelection() {
        ObservableList<String> selectedItems = availableTags.getSelectionModel().getSelectedItems();
        selectedTag.set(selectedItems.get(0));
    }

    public void setAvaibleOptions(Collection<String> tags) {
        avialable.clear();
        avialable.addAll(tags.stream().sorted().collect(Collectors.toList()));
    }

    public String getSelectedTag() {
        return selectedTag.get();
    }

}
