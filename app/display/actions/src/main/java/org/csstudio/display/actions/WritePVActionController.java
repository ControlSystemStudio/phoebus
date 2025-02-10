/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.actions;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.csstudio.display.builder.model.ActionControllerBase;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;

/**
 * FXML Controller for the write PV action editor.
 */
public class WritePVActionController extends ActionControllerBase {

    @SuppressWarnings("unused")
    @FXML
    private TextField pvName;

    @SuppressWarnings("unused")
    @FXML
    private TextField pvValue;

    private final StringProperty pvNameProperty = new SimpleStringProperty();
    private final StringProperty pvValueProperty = new SimpleStringProperty();

    public WritePVActionController(WritePVAction writePVAction){
        descriptionProperty.set(writePVAction.getDescription());
        pvNameProperty.setValue(writePVAction.getPV());
        pvValueProperty.setValue(writePVAction.getValue());
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        super.initialize();
        pvName.textProperty().bindBidirectional(pvNameProperty);
        pvValue.textProperty().bindBidirectional(pvValueProperty);
        PVAutocompleteMenu.INSTANCE.attachField(pvName);
    }

    public String getValue(){
        return pvValueProperty.get();
    }

    public void setValue(String value){
        pvValueProperty.set(value);
    }

    public ActionInfo getActionInfo(){
        return new WritePVAction(descriptionProperty.get(), pvNameProperty.get(), pvValueProperty.get());
    }
}
