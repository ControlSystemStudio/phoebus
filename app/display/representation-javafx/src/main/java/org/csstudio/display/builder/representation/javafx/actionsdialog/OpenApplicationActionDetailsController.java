
package org.csstudio.display.builder.representation.javafx.actionsdialog;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.OpenApplicationActionInfo;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.workbench.ApplicationService;

import java.util.Collection;
import java.util.Comparator;

/**
 * FXML Controller for open resource action.
 */
public class OpenApplicationActionDetailsController implements ActionDetailsController{

    private final OpenApplicationActionInfo actionInfo;

    @FXML
    private TextField description;
    @FXML
    private TextField inputUri ;
    @FXML
    private ComboBox<AppDescriptor> applicationSelector;

    private final StringProperty descriptionProperty = new SimpleStringProperty();
    private final StringProperty inputUriProperty = new SimpleStringProperty();
    private final SimpleObjectProperty<AppDescriptor> appDescriptorProperty = new SimpleObjectProperty();

    //private Collection<AppDescriptor> appDescriptors;

    /** @param actionInfo ActionInfo */
    public OpenApplicationActionDetailsController(ActionInfo actionInfo){
        this.actionInfo = (OpenApplicationActionInfo) actionInfo;
    }

    /** Init */
    @FXML
    public void initialize(){
        descriptionProperty.setValue(actionInfo.getDescription());
        inputUriProperty.setValue(actionInfo.getInputUri());

        description.textProperty().bindBidirectional(descriptionProperty);
        inputUri.textProperty().bindBidirectional(inputUriProperty);

        // Find all apps in order to create the combo box list
        final Collection<AppDescriptor> appDescriptors =
                ApplicationService.getApplications().stream().sorted(Comparator.comparing(AppDescriptor::getDisplayName)).toList();
        applicationSelector.getItems().addAll(appDescriptors);
        applicationSelector.valueProperty().bindBidirectional(appDescriptorProperty);

        applicationSelector.setCellFactory(new Callback<>() {
            @Override
            public ListCell<AppDescriptor> call(ListView<AppDescriptor> param) {
                return new ListCell<>() {
                    @Override
                    public void updateItem(AppDescriptor item,
                                           boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty && item != null) {
                            setText(item.getDisplayName());
                        }
                    }
                };
            }
        });

        applicationSelector.setConverter(
            new StringConverter<>() {
                @Override
                public String toString(AppDescriptor appDescriptor) {
                    if (appDescriptor == null) {
                        return "";
                    } else {
                        return appDescriptor.getDisplayName();
                    }
                }
                @Override
                public AppDescriptor fromString(String s) {
                    return appDescriptors.stream().filter(a -> a.getDisplayName().equalsIgnoreCase(s)).findFirst().orElse(null);
                }
            });

        appDescriptorProperty.setValue(actionInfo.getAppDescriptor());
    }

    /** @return ActionInfo */
    @Override
    public ActionInfo getActionInfo(){
        return new OpenApplicationActionInfo(descriptionProperty.get(),
                appDescriptorProperty.get(),
                inputUriProperty.get());
    }
}
