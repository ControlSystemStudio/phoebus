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

public class AddPropertyController {
    public static final ObservableList<String> avialable = FXCollections.observableArrayList();

    StringProperty selectedProperty = new SimpleStringProperty();
    StringProperty selectedPropertyValue = new SimpleStringProperty();

    @FXML
    ListView<String> availableProperties;

    @FXML
    TextField selectedPropertyText;

    @FXML
    TextField selectedPropertyValueText;

    @FXML
    public void initialize() {
        availableProperties.setItems(avialable);
        availableProperties.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        selectedPropertyText.textProperty().bindBidirectional(selectedProperty);
        selectedPropertyValueText.textProperty().bindBidirectional(selectedPropertyValue);
    }

    @FXML
    public void setSelection() {
        ObservableList<String> selectedItems = availableProperties.getSelectionModel().getSelectedItems();
        selectedProperty.set(selectedItems.get(0));
    }

    public void setAvaibleOptions(Collection<String> properties) {
        avialable.clear();
        avialable.addAll(properties.stream().sorted().collect(Collectors.toList()));
    }

    public String getProperty() {
        return selectedProperty.get();
    }
    
    public String getValue() {
        return selectedPropertyValue.get();
    }
}
