package org.phoebus.applications.saveandrestore.ui;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.util.Date;

public class NodeLastUpdatedTreeTableCell extends TreeTableCell<Node, Node> {

    private final SaveAndRestoreController saveAndRestoreController;

    public NodeLastUpdatedTreeTableCell(SaveAndRestoreController saveAndRestoreController){
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
            setText(TimestampFormats.SECONDS_FORMAT.format(node.getLastModified().toInstant()));
        }
        setOpacity(saveAndRestoreController.matchesFilter(node) ? 1.0 : 0.4);
    }
}
