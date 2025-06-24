/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.actions;

import org.csstudio.display.builder.model.ActionControllerBase;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionInfo;

import javafx.fxml.FXML;

/**
 * FXML Controller for the close display action editor
 */
public class CloseDisplayActionController extends ActionControllerBase {

    public CloseDisplayActionController(Widget widget, CloseDisplayAction closeDisplayActionInfo){
        descriptionProperty.set(closeDisplayActionInfo.getDescription());
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        super.initialize();
    }

    public ActionInfo getActionInfo(){
        return new CloseDisplayAction(descriptionProperty.get());
    }
}
