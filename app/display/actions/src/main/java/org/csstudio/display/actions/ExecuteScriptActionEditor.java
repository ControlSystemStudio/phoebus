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
 * Editor for {@link ExecuteScriptAction}.
 */
public class ExecuteScriptActionEditor implements ActionEditor {

    private ExecuteScriptActionController executeScriptActionController;
    private Node editorUi;

    @Override
    public boolean matchesAction(String type) {
        return ExecuteScriptAction.EXECUTE_SCRIPT.equalsIgnoreCase(type);
    }

    @Override
    public ActionInfo getActionInfo() {
        return executeScriptActionController.getActionInfo();
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
        fxmlLoader.setLocation(this.getClass().getResource("ExecuteScriptActionDetails.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(Widget.class, ExecuteScriptAction.class).newInstance(widget, actionInfo);
            } catch (Exception e) {
                Logger.getLogger(ExecuteCommandActionEditor.class.getName()).log(Level.SEVERE, "Failed to construct ExecuteScriptActionController", e);
            }
            return null;
        });

        try {
            this.editorUi = fxmlLoader.load();
            executeScriptActionController = fxmlLoader.getController();
        } catch (IOException e) {
            Logger.getLogger(ExecuteCommandActionEditor.class.getName()).log(Level.SEVERE, "Failed to load ExecuteScriptAction UI", e);
            throw new RuntimeException(e);
        }
    }
}
