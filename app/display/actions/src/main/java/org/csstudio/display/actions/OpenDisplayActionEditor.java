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
 * Editor for {@link OpenDisplayAction}.
 */
public class OpenDisplayActionEditor implements ActionEditor {

    private OpenDisplayActionController openDisplayActionController;
    private Node editorUi;

    @Override
    public boolean matchesAction(String type) {
        return OpenDisplayAction.OPEN_DISPLAY.equalsIgnoreCase(type);
    }

    @Override
    public ActionInfo getActionInfo() {
        return openDisplayActionController.getActionInfo();
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
        fxmlLoader.setLocation(this.getClass().getResource("OpenDisplayAction.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(Widget.class, OpenDisplayAction.class).newInstance(widget, actionInfo);
            } catch (Exception e) {
                Logger.getLogger(OpenDisplayActionEditor.class.getName()).log(Level.SEVERE, "Failed to construct OpenDisplayActionController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            openDisplayActionController = fxmlLoader.getController();
        } catch (IOException e) {
            Logger.getLogger(OpenDisplayActionEditor.class.getName()).log(Level.SEVERE, "Failed to load OpenDisplayAction UI", e);
            throw new RuntimeException(e);
        }
    }
}
