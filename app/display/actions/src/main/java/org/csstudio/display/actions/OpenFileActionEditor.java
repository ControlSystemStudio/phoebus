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
 * Editor for {@link OpenFileAction}.
 */
public class OpenFileActionEditor implements ActionEditor {

    private OpenFileActionController openFileActionController;
    private Node editorUi;

    @Override
    public boolean matchesAction(String type) {
        return OpenFileAction.OPEN_FILE.equals(type);
    }

    @Override
    public ActionInfo getActionInfo() {
        return openFileActionController.getActionInfo();
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
        fxmlLoader.setLocation(this.getClass().getResource("OpenFileAction.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(Widget.class, OpenFileAction.class).newInstance(widget, actionInfo);
            } catch (Exception e) {
                Logger.getLogger(OpenFileActionEditor.class.getName())
                        .log(Level.SEVERE, "Failed to construct OpenFileActionController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            openFileActionController = fxmlLoader.getController();
        } catch (IOException e) {
            Logger.getLogger(OpenFileActionEditor.class.getName())
                    .log(Level.SEVERE, "Failed to load  OpenFileAction UI", e);
            throw new RuntimeException(e);
        }
    }
}
