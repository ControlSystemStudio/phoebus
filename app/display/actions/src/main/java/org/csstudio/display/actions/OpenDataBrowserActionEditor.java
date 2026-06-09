package org.csstudio.display.actions;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionEditor;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.framework.nls.NLS;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Editor for {@link OpenDataBrowserAction}.
 */
public class OpenDataBrowserActionEditor implements ActionEditor {

    private OpenDataBrowserActionController openDataBrowserActionController;
    private Node openDataBowserEditorUi;

    @Override
    public boolean matchesAction(String type) {
        return OpenDataBrowserAction.OPEN_DATA_BROWSER.equalsIgnoreCase(type);
    }

    @Override
    public ActionInfo getActionInfo() {
        return openDataBrowserActionController.getActionInfo();
    }

    @Override
    public Node getEditorUi() {
        return openDataBowserEditorUi;
    }

    @Override
    public void configure(Widget widget, ActionInfo actionInfo) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(NLS.getMessages(Messages.class));
        fxmlLoader.setLocation(this.getClass().getResource("OpenDataBrowserAction.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(OpenDataBrowserAction.class).newInstance(actionInfo);
            } catch (Exception e) {
                Logger.getLogger(OpenDataBrowserActionEditor.class.getName()).log(Level.SEVERE, "Failed to construct OpenDataBrowserActionController", e);
            }
            return null;
        });

        try {
            openDataBowserEditorUi = fxmlLoader.load();
            openDataBrowserActionController = fxmlLoader.getController();
        } catch (IOException e) {
            Logger.getLogger(OpenDataBrowserActionEditor.class.getName()).log(Level.SEVERE, "Failed to load the OpenDataBrowserAction UI", e);
            throw new RuntimeException(e);
        }
    }
}
