/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.framework.nls.NLS;

import java.util.Arrays;
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
     *
     * @param supportsCreateFolder If <code>false</code>, the UI will not include button for the purpose
     *                             of creating folders in the tree structure (yes, this is a hack).
     * @param hiddenNodeTypes      Optional list of {@link NodeType}s to be hidden in the
     *                             {@link javafx.scene.control.TreeView}. Of course
     *                             {@link NodeType#FOLDER} does not make sense here.
     */
    public NodeSelectionDialog(boolean supportsCreateFolder, NodeType... hiddenNodeTypes) {
        setTitle(Messages.nodeSelectionForConfiguration);
        try {
            ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
            FXMLLoader loader = new FXMLLoader();
            loader.setResources(resourceBundle);
            loader.setLocation(SaveAndRestoreApplication.class.getResource("ui/NodeSelector.fxml"));
            loader.load();
            getDialogPane().setContent(loader.getRoot());
            NodeSelectionController nodeSelectionController = loader.getController();
            nodeSelectionController.setShowCreateFolderButton(supportsCreateFolder);
            if (hiddenNodeTypes != null) {
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

    /**
     * Constructor.
     *
     * @param hiddenNodeTypes Optional list of {@link NodeType}s to be hidden in the
     *                        {@link javafx.scene.control.TreeView}. Of course
     *                        {@link NodeType#FOLDER} does not make sense here.
     */
    public NodeSelectionDialog(NodeType... hiddenNodeTypes) {
        this(false, hiddenNodeTypes);
    }
}
