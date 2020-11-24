package org.phoebus.channel.views.ui;

import java.util.Collection;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import org.phoebus.channelfinder.Property;

import static org.phoebus.channelfinder.Property.Builder.property;

public class AddPropertyController {
    public static final ObservableList<Property> available = FXCollections.observableArrayList();

    StringProperty selectedProperty = new SimpleStringProperty();
    StringProperty selectedPropertyValue = new SimpleStringProperty();
    StringProperty selectedPropertyOwner = new SimpleStringProperty();

    @FXML
    ListView<Property> availableProperties;

    @FXML
    TextField selectedPropertyText;

    @FXML
    TextField selectedPropertyValueText;

    @FXML
    TextField selectedPropertyOwnerText;

    @FXML
    public void initialize() {
        availableProperties.setCellFactory(new Callback<ListView<Property>, ListCell<Property>>(){

            @Override
            public ListCell<Property> call(ListView<Property> p) {

                ListCell<Property> cell = new ListCell<Property>(){

                    @Override
                    protected void updateItem(Property property, boolean bln) {
                        super.updateItem(property, bln);
                        if (property != null) {
                            setText(property.getName() + " ("+property.getOwner()+")");
                        }
                    }

                };

                return cell;
            }
        });
        availableProperties.setItems(available);
        availableProperties.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        selectedPropertyText.textProperty().bindBidirectional(selectedProperty);
        selectedPropertyValueText.textProperty().bindBidirectional(selectedPropertyValue);
        selectedPropertyOwnerText.textProperty().bindBidirectional(selectedPropertyOwner);
    }

    @FXML
    public void setSelection() {
        ObservableList<Property> selectedItems = availableProperties.getSelectionModel().getSelectedItems();
        selectedProperty.set(selectedItems.get(0).getName());
        selectedPropertyOwner.set(selectedItems.get(0).getOwner());
    }

    public void setAvaibleOptions(Collection<Property> properties) {
        available.clear();
        available.addAll(properties.stream().sorted((o1, o2) -> {return o1.getName().compareToIgnoreCase(o2.getName());}).collect(Collectors.toList()));
    }

    public Property getProperty() {
        return property(selectedProperty.get(), selectedPropertyValue.get()).owner(selectedPropertyOwner.get()).build();
    }

    public String getPropertyName() {
        return selectedProperty.get();
    }

    public String getValue() {
        return selectedPropertyValue.get();
    }
}
