/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * Utility base class taking care of common aspects of an action editor controller.
 */
public abstract class ActionControllerBase {

    @SuppressWarnings("unused")
    @FXML
    private TextField description;

    protected final StringProperty descriptionProperty = new SimpleStringProperty();

    public ActionControllerBase(){}

    public void initialize(){
        description.textProperty().bindBidirectional(descriptionProperty);
    }

    public String getDescription(){
        return descriptionProperty.get();
    }

    public void setDescription(String description){
        descriptionProperty.set(description);
    }
}
