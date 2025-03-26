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
 * Editor for {@link WritePVAction}.
 */
public class WritePVActionEditor implements ActionEditor {

    private WritePVActionController writePVActionController;
    private Node editorUi;

    @Override
    public void configure(Widget widget, ActionInfo actionInfo) {
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        fxmlLoader.setLocation(this.getClass().getResource("WritePVAction.fxml"));

        fxmlLoader.setControllerFactory(clazz -> {
            try {
                return clazz.getConstructor(WritePVAction.class).newInstance(actionInfo);
            } catch (Exception e) {
                Logger.getLogger(WritePVActionEditor.class.getName()).log(Level.SEVERE, "Failed to construct WritePVActionController", e);
            }
            return null;
        });

        try {
            editorUi = fxmlLoader.load();
            writePVActionController = fxmlLoader.getController();
        } catch (IOException e) {
            Logger.getLogger(WritePVActionEditor.class.getName()).log(Level.SEVERE, "Failed to load the WritePVAction UI", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean matchesAction(String type) {
        return WritePVAction.WRITE_PV.equals(type);
    }

    @Override
    public ActionInfo getActionInfo() {
        return writePVActionController.getActionInfo();
    }

    @Override
    public Node getEditorUi() {
        return editorUi;
    }
}
