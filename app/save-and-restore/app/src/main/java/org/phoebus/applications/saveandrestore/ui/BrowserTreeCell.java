/**
 * Copyright (C) 2026 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui;


import javafx.scene.control.TreeCell;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;

/**
 * A cell editor managing the different type of nodes in the save-and-restore data set
 * when rendered in a {@link javafx.scene.control.TreeView}.
 */
public class BrowserTreeCell extends TreeCell<Node> {

    public BrowserTreeCell(){
        super();
    }

    @Override
    public void updateItem(Node node, boolean empty) {
        super.updateItem(node, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
            return;
        }

        switch (node.getNodeType()) {
            case SNAPSHOT:
                setGraphic(node.hasTag(Tag.GOLDEN) ? new ImageView(ImageRepository.GOLDEN_SNAPSHOT)
                        : new ImageView(ImageRepository.SNAPSHOT));
                break;
            case COMPOSITE_SNAPSHOT:
                setGraphic(new ImageView(ImageRepository.COMPOSITE_SNAPSHOT));
                break;
            case CONFIGURATION:
                setGraphic(new ImageView(ImageRepository.CONFIGURATION));
                break;
            case FOLDER:
                setGraphic(new ImageView(ImageRepository.FOLDER));
                break;
        }

        setText(node.getName());
    }
}
