/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.actions;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionEditor;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.framework.nls.NLS;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Editor for {@link OpenFilterAction}.
 */
public class OpenFilterActionEditor implements ActionEditor {

    private OpenFilterActionController openFilterActionController;
    private Node editorUi;

    @Override
    public boolean matchesAction(String type) {
        return OpenFilterAction.OPEN_SAR_FILTER.equalsIgnoreCase(type);
    }

    @Override
    public ActionInfo getActionInfo() {
        return openFilterActionController.getActionInfo();
    }

    @Override
    public Node getEditorUi() {
        return editorUi;
    }

    @Override
    public void configure(Widget widget, ActionInfo actionInfo) {
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("OpenFilterAction.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(OpenFilterAction.class).newInstance(actionInfo);
            } catch (Exception e) {
                Logger.getLogger(OpenFilterActionEditor.class.getName()).log(Level.SEVERE, "Failed to construct OpenFilterActionController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            openFilterActionController = fxmlLoader.getController();
        } catch (IOException e) {
            Logger.getLogger(OpenFilterActionEditor.class.getName()).log(Level.SEVERE, "Failed to load OpenFilterAction UI", e);
            throw new RuntimeException(e);
        }
    }
}
