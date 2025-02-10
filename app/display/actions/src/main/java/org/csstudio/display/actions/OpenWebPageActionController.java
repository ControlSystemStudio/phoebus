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

/**
 * FXML Controller for the open web page action editor.
 */
public class OpenWebPageActionController extends ActionControllerBase {

    @SuppressWarnings("unused")
    @FXML
    private TextField url;

    private final StringProperty urlProperty = new SimpleStringProperty();

    /**
     * @param openWebPageAction {@link ActionInfo}
     */
    public OpenWebPageActionController(OpenWebPageAction openWebPageAction) {
        descriptionProperty.set(openWebPageAction.getDescription());
        urlProperty.setValue(openWebPageAction.getURL());
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        super.initialize();
        url.textProperty().bindBidirectional(urlProperty);
    }

    public ActionInfo getActionInfo(){
        return new OpenWebPageAction(descriptionProperty.get(), urlProperty.get());
    }
}
