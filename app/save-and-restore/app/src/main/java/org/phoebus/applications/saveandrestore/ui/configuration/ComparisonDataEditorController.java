/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.configuration;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.DoubleStringConverter;
import org.phoebus.applications.saveandrestore.model.PvCompareMode;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComparisonDataEditorController {

    @FXML
    private ComboBox<PvCompareMode> comparisonModeComboBox;

    @FXML
    private TextField toleranceTextField;

    private ObjectProperty<PvCompareMode> comparisonModeProperty = new SimpleObjectProperty<>();
    private StringProperty toleranceProperty = new SimpleStringProperty();
    private Pattern pattern = Pattern.compile("\\d*(\\.?\\d*)?");

    //private ConfigurationController.ComparisonData comparisonData;
    public ComparisonDataEditorController(){

    }

    @FXML
    public void initialize(){
        comparisonModeComboBox.itemsProperty().set(FXCollections.observableArrayList(PvCompareMode.values()));
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
