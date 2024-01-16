package org.csstudio.display.builder.representation.javafx.actionsdialog;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.CallPVActionInfo;
import org.csstudio.display.builder.model.properties.WritePVActionInfo;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;

import java.util.HashMap;

public class CallPVActionDetailsController implements ActionDetailsController {
    private final CallPVActionInfo actionInfo;
    @FXML
    private TextField description;
    @FXML
    private TextField pvName;

    private StringProperty descriptionProperty = new SimpleStringProperty();
    private StringProperty pvNameProperty = new SimpleStringProperty();

    public CallPVActionDetailsController(ActionInfo actionInfo) {
        this.actionInfo = (CallPVActionInfo) actionInfo;
    }

    @FXML
    public void initialize(){
        descriptionProperty.setValue(actionInfo.getDescription());
        pvNameProperty.setValue(actionInfo.getPV());

        description.textProperty().bindBidirectional(descriptionProperty);
        pvName.textProperty().bindBidirectional(pvNameProperty);

        PVAutocompleteMenu.INSTANCE.attachField(pvName);
    }


    @Override
    public ActionInfo getActionInfo() {
        return new CallPVActionInfo(
                this.descriptionProperty.get(),
                this.pvNameProperty.get(),
                new HashMap<>()
        );
    }
}
