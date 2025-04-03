/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.configuration;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.phoebus.applications.saveandrestore.model.CompareMode;

import java.util.regex.Pattern;

public class ComparisonDataEditorController {

    @FXML
    private ComboBox<CompareMode> comparisonModeComboBox;

    @FXML
    private TextField toleranceTextField;

    private ObjectProperty<CompareMode> comparisonModeProperty = new SimpleObjectProperty<>();
    private StringProperty toleranceProperty = new SimpleStringProperty();
    private Pattern pattern = Pattern.compile("\\d*(\\.?\\d*)?");

    //private ConfigurationController.ComparisonData comparisonData;
    public ComparisonDataEditorController(){

    }

    @FXML
    public void initialize(){
        comparisonModeComboBox.itemsProperty().set(FXCollections.observableArrayList(CompareMode.values()));
        comparisonModeComboBox.valueProperty().bind(comparisonModeProperty);
        toleranceTextField.textProperty().bindBidirectional(toleranceProperty);

        toleranceTextField.textProperty().addListener((obs, o, n) -> {
            if(n != null && !n.isEmpty()){
                if(!pattern.matcher(n).matches()){
                    toleranceTextField.textProperty().set(o);
                }
            }
        });
    }

    public void setComparisonData(ConfigurationController.ComparisonData comparisonData){

    }

   
}
