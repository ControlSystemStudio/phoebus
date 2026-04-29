package org.csstudio.display.actions;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.csstudio.display.builder.model.ActionControllerBase;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionInfo;

/**
 * FXML Controller for the open data browser action editor
 */
public class OpenDataBrowserActionController extends ActionControllerBase {

    @FXML
    private TextField pvNames;

    @FXML
    private TextField timeframe;

    private final StringProperty pvNameProperty = new SimpleStringProperty();
    private final StringProperty timeframeProperty = new SimpleStringProperty();

    public OpenDataBrowserActionController(Widget widget, OpenDataBrowserAction openDataBrowserActionInfo){
        descriptionProperty.set(openDataBrowserActionInfo.getDescription());
        pvNameProperty.setValue(openDataBrowserActionInfo.getPVs());
        timeframeProperty.setValue(openDataBrowserActionInfo.getTimeframe());
    }

    /**
     * Init
     */
    @FXML
    public void initialize() {
        super.initialize();
        pvNames.textProperty().bindBidirectional(pvNameProperty);
        timeframe.textProperty().bindBidirectional(timeframeProperty);
    }

    public ActionInfo getActionInfo(){
        return new OpenDataBrowserAction(descriptionProperty.get(), pvNameProperty.get(), timeframeProperty.get());
    }
}
