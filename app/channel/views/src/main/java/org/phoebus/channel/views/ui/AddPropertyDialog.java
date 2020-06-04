package org.phoebus.channel.views.ui;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;

import org.phoebus.channelfinder.Property;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import static org.phoebus.channel.views.ui.ChannelFinderController.logger;
/**
 * A dialog for adding a property to a list of channels
 * 
 * @author Kunal Shroff
 *
 */
public class AddPropertyDialog extends Dialog<Property> {

    public AddPropertyDialog(final Node parent, final Collection<String> properties) {
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResizable(true);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("AddProperty.fxml"));
        try {
            getDialogPane().setContent(loader.load());
            AddPropertyController controller = loader.getController();
            controller.setAvaibleOptions(properties);
            setResultConverter(button -> {
                return button == ButtonType.OK
                        ? Property.Builder.property(controller.getProperty(), controller.getValue()).build()
                        : null;
            });
        } catch (IOException e) {
            // TODO update the dialog
            logger.log(Level.WARNING, "Failed to add property", e);
        }
    }
}
