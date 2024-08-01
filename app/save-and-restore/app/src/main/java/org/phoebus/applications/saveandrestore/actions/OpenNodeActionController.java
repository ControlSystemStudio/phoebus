/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.actions;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.csstudio.display.builder.model.ActionControllerBase;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.NodeSelectionDialog;

import java.util.List;
import java.util.Optional;

public class OpenNodeActionController extends ActionControllerBase {

    private final OpenNodeAction openNodeAction;

    @FXML
    private TextField nodeId;

    private final SimpleStringProperty nodeIdProperty = new SimpleStringProperty();

    public OpenNodeActionController(ActionInfo actionInfo) {
        this.openNodeAction = (OpenNodeAction) actionInfo;
        descriptionProperty.set(openNodeAction.getDescription());
    }

    @FXML
    public void initialize() {
        super.initialize();
        setInitialNodeId(openNodeAction.getNodeId());
        nodeId.textProperty().bindBidirectional(nodeIdProperty);

        nodeId.setOnDragDropped(event -> {
            List<Node> sourceNodes = (List<Node>) event.getDragboard().getContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT);
            if (sourceNodes.size() == 1) {
                nodeIdProperty.set(sourceNodes.get(0).getUniqueId());
            }
        });
    }

    @FXML
    public void selectNode() {
        NodeSelectionDialog nodeSelectionDialog = new NodeSelectionDialog(false);
        Optional<Node> nodeOptional = nodeSelectionDialog.showAndWait();
        if (nodeOptional.isPresent()) {
            nodeIdProperty.set(nodeOptional.get().getUniqueId());
        }
    }

    public String getNodeId() {
        return nodeIdProperty.get();
    }

    public void setInitialNodeId(String nodeId) {
        nodeIdProperty.set(nodeId);
    }
}
