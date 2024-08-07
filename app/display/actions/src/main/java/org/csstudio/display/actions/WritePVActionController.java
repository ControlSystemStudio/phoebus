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

    private final WritePVAction writePVActionInfo;

    @FXML
    private TextField pvName;
    @FXML
    private TextField pvValue;

    private final StringProperty pvNameProperty = new SimpleStringProperty();
    private final StringProperty pvValueProperty = new SimpleStringProperty();

    /**
     * @param actionInfo {@link ActionInfo}
     */
    public WritePVActionController(ActionInfo actionInfo) {
        this.writePVActionInfo = (WritePVAction) actionInfo;
        descriptionProperty.set(actionInfo.getDescription());
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        super.initialize();

        pvNameProperty.setValue(writePVActionInfo.getPV());
        pvValueProperty.setValue(writePVActionInfo.getValue());
        pvName.textProperty().bindBidirectional(pvNameProperty);
        pvValue.textProperty().bindBidirectional(pvValueProperty);

        PVAutocompleteMenu.INSTANCE.attachField(pvName);
    }

    public String getPvName(){
        return pvNameProperty.get();
    }

    public String getValue(){
        return pvValueProperty.get();
    }

    public void setValue(String value){
        pvValueProperty.set(value);
    }

    public void setPvName(String pvName){
        pvNameProperty.set(pvName);
    }
}
