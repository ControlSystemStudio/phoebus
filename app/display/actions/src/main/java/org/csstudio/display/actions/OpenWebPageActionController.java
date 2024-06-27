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

    private final OpenWebPageAction openWebpageActionInfo;

    @FXML
    private TextField url;

    private final StringProperty urlProperty = new SimpleStringProperty();

    /**
     * @param actionInfo {@link ActionInfo}
     */
    public OpenWebPageActionController(ActionInfo actionInfo) {
        this.openWebpageActionInfo = (OpenWebPageAction) actionInfo;
        descriptionProperty.set(openWebpageActionInfo.getDescription());
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        super.initialize();

        urlProperty.setValue(openWebpageActionInfo.getURL());
        url.textProperty().bindBidirectional(urlProperty);
    }

    public String getUrl(){
        return urlProperty.get();
    }

    public void setUrl(String url){
        urlProperty.set(url);
    }
}
