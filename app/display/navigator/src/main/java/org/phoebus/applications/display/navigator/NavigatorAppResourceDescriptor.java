package org.phoebus.applications.display.navigator;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockPane;

import java.net.URI;
import java.net.URL;
import java.util.List;

public class NavigatorAppResourceDescriptor implements AppResourceDescriptor {

    protected static NavigatorInstance instance = null;
    @Override
    public String getName() {
        return "navigator";
    }

    @Override
    public String getDisplayName() {
        return "Navigator";
    }

    @Override
    public URL getIconURL() {
        return getClass().getResource("/icons/navigator.png");
    }

    @Override
    public void start() {
        return;
    }

    @Override
    public AppInstance create() {
        instance = new NavigatorInstance(this);
        return instance;
    }

    @Override
    public void stop() {
        if (instance != null && instance.controller.unsavedChanges) {
            ButtonType saveAndExit = new ButtonType("Save & Exit");
            ButtonType discardAndExit = new ButtonType("Discard & Exit");

            Alert prompt = new Alert(Alert.AlertType.CONFIRMATION);
            prompt.getDialogPane().getButtonTypes().remove(ButtonType.OK);
            prompt.getDialogPane().getButtonTypes().remove(ButtonType.CANCEL);
            ((ButtonBar) prompt.getDialogPane().lookup(".button-bar")).setButtonOrder(ButtonBar.BUTTON_ORDER_NONE); // Set the button order manually (since they are non-standard)
            prompt.getDialogPane().getButtonTypes().add(saveAndExit);
            prompt.getDialogPane().getButtonTypes().add(discardAndExit);

            int prefWidth = 400;
            int prefHeight = 160;
            prompt.getDialogPane().setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            prompt.getDialogPane().setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            prompt.setResizable(false);
            DialogHelper.positionDialog(prompt, DockPane.getActiveDockPane(), -prefWidth, -prefHeight);
            prompt.initOwner(DockPane.getActiveDockPane().getScene().getWindow());
            prompt.setResizable(true);
            prompt.setTitle("Unsaved Changes in a Navigator");
            prompt.setHeaderText("There are unsaved changes in the navigator '" + instance.controller.navigatorName_original + "'. Do you want to save or discard the changes?");
            ButtonType result = prompt.showAndWait().orElse(discardAndExit);

            if (result == saveAndExit) {
                instance.controller.saveNavigatorAction(null);
            }
        }
    }

    @Override
    public List<String> supportedFileExtentions() {
        return List.of("navigator");
    }

    @Override
    public AppInstance create(URI resource) {
        instance = new NavigatorInstance(this);
        return instance;
    }
}
