/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

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
 * Editor for {@link OpenWebPageAction}.
 */
public class OpenWebPageActionEditor implements ActionEditor {

    private OpenWebPageActionController openWebPageActionController;
    private Node editorUi;

    @Override
    public boolean matchesAction(String type) {
        return OpenWebPageAction.OPEN_WEBPAGE.equalsIgnoreCase(type);
    }

    @Override
    public ActionInfo getActionInfo() {
        return openWebPageActionController.getActionInfo();
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
        fxmlLoader.setLocation(this.getClass().getResource("OpenWebPageAction.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(OpenWebPageAction.class).newInstance(actionInfo);
            } catch (Exception e) {
                Logger.getLogger(OpenWebPageActionEditor.class.getName())
                        .log(Level.SEVERE, "Failed to construct OpenWebPageActionController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            openWebPageActionController = fxmlLoader.getController();
        } catch (IOException e) {
            Logger.getLogger(OpenWebPageActionEditor.class.getName())
                    .log(Level.SEVERE, "Failed to load OpenWebPageAction UI", e);
            throw new RuntimeException(e);
        }
    }
}
