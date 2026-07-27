package org.phoebus.applications.saveandrestore.ui;

import javafx.scene.control.TreeTableCell;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.util.time.TimestampFormats;

public class NodeUserTreeTableCell extends TreeTableCell<Node, Node> {

    private final SaveAndRestoreController saveAndRestoreController;

    public NodeUserTreeTableCell(SaveAndRestoreController saveAndRestoreController){
        this.saveAndRestoreController = saveAndRestoreController;
    }

    @Override
    public void updateItem(Node node, boolean empty){
        super.updateItem(node, empty);
        if (empty) {
            setText(null);
            return;
        }

        if(node.getNodeType().equals(NodeType.FOLDER)){
            setText(null);
        }
        else{
            setText(node.getUserName());
        }
        setOpacity(saveAndRestoreController.matchesFilter(node) ? 1.0 : 0.4);
    }
}
