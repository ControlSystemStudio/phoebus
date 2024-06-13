/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.representation.javafx.actions;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;

/**
 * FXML Controller
 */
public class OpenWebPageDetailsController {

    private final OpenWebPageAction openWebpageActionInfo;

    @FXML
    private TextField description;
    @FXML
    private TextField url;

    private final StringProperty descriptionProperty = new SimpleStringProperty();
    private final StringProperty urlProperty = new SimpleStringProperty();

    /**
     * @param actionInfo ActionInfo
     */
    public OpenWebPageDetailsController(PluggableActionInfo actionInfo) {
        this.openWebpageActionInfo = (OpenWebPageAction) actionInfo;
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        descriptionProperty.setValue(openWebpageActionInfo.getDescription());
        urlProperty.setValue(openWebpageActionInfo.getURL());

        description.textProperty().bindBidirectional(descriptionProperty);
        url.textProperty().bindBidirectional(urlProperty);

        descriptionProperty.addListener((obs, o, n) -> openWebpageActionInfo.setDescription(n));
        urlProperty.addListener((obs, o, n) -> openWebpageActionInfo.setURL(n));
    }
}
