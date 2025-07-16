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
 * Editor for {@link CloseDisplayAction}.
 */
public class CloseDisplayActionEditor implements ActionEditor {

    private CloseDisplayActionController closeDisplayActionController;
    private Node editorUi;

    @Override
    public boolean matchesAction(String type) {
        return CloseDisplayAction.CLOSE_DISPLAY.equalsIgnoreCase(type);
    }

    @Override
    public ActionInfo getActionInfo() {
        return closeDisplayActionController.getActionInfo();
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
        fxmlLoader.setLocation(this.getClass().getResource("CloseDisplayAction.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(Widget.class, CloseDisplayAction.class).newInstance(widget, actionInfo);
            } catch (Exception e) {
                Logger.getLogger(CloseDisplayActionEditor.class.getName()).log(Level.SEVERE, "Failed to construct CloseDisplayActionController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            closeDisplayActionController = fxmlLoader.getController();
        } catch (IOException e) {
            Logger.getLogger(CloseDisplayActionEditor.class.getName()).log(Level.SEVERE, "Failed to load the CloseDisplayAction UI", e);
            throw new RuntimeException(e);
        }
    }
}
