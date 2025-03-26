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

    @SuppressWarnings("unused")
    @FXML
    private TextField nodeId;

    private final SimpleStringProperty nodeIdProperty = new SimpleStringProperty();

    public OpenNodeActionController(OpenNodeAction openNodeAction) {
        descriptionProperty.set(openNodeAction.getDescription());
        nodeIdProperty.set(openNodeAction.getNodeId());
    }

    @FXML
    public void initialize() {
        super.initialize();
        nodeId.textProperty().bindBidirectional(nodeIdProperty);

        nodeId.setOnDragDropped(event -> {
            List<Node> sourceNodes = (List<Node>) event.getDragboard().getContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT);
            if (sourceNodes.size() == 1) {
                nodeIdProperty.set(sourceNodes.get(0).getUniqueId());
            }
        });
    }

    @SuppressWarnings("unused")
    @FXML
    public void selectNode() {
        NodeSelectionDialog nodeSelectionDialog = new NodeSelectionDialog(false);
        Optional<Node> nodeOptional = nodeSelectionDialog.showAndWait();
        if (nodeOptional.isPresent()) {
            nodeIdProperty.set(nodeOptional.get().getUniqueId());
        }
    }

    public ActionInfo getActionInfo(){
        return new OpenNodeAction(descriptionProperty.get(), nodeIdProperty.get());
    }
}
