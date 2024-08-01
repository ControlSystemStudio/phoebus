/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.ui.configuration.ConfigurationFromSelectionController;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.javafx.ImageCache;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom {@link Dialog<Node>} used to prompt user for a {@link Node} selection in the
 * save-and-restore tree using a {@link javafx.scene.control.TreeView}.
 */
public class NodeSelectionDialog extends Dialog<Node> {

    /**
     * Constructor.
     * @param hiddenNodeTypes Optional list of {@link NodeType}s to be hidden in the
     *                        {@link javafx.scene.control.TreeView}. Of course
     *                        {@link NodeType#FOLDER} does not make sense here.
     */
    public NodeSelectionDialog(NodeType... hiddenNodeTypes){
        setTitle(Messages.nodeSelectionForConfiguration);
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(SaveAndRestoreApplication.class.getResource("ui/NodeSelector.fxml"));
            loader.load();
            getDialogPane().setContent(loader.getRoot());
            NodeSelectionController nodeSelectionController = loader.getController();
            if(hiddenNodeTypes != null) {
                nodeSelectionController.setHiddenNodeTypes(Arrays.asList(hiddenNodeTypes));
            }
            nodeSelectionController.addOkButtonActionHandler(e -> {
                setResult(nodeSelectionController.getSelectedNode());
                close();
            });
        } catch (Exception e) {
            Logger.getLogger(NodeSelectionDialog.class.getName()).log(Level.WARNING, "Unable to launch node selection UI", e);
        }
    }
}
