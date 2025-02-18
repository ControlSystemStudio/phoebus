/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.contextmenu;

import javafx.collections.ObservableList;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.ui.javafx.ImageCache;

public class ExportToCSVMenuItem extends SaveAndRestoreMenuItem {

    public ExportToCSVMenuItem(SaveAndRestoreBaseController saveAndRestoreController,
                               ObservableList<Node> selectedItemsProperty,
                               Runnable onAction) {
        super(saveAndRestoreController, selectedItemsProperty, onAction);
        // Set text in configure()
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/csv_export.png"));
    }

    @Override
    public void configure() {
        visibleProperty().set(selectedItemsProperty.size() == 1 &&
                (selectedItemsProperty.get(0).getNodeType().equals(NodeType.CONFIGURATION) ||
                        selectedItemsProperty.get(0).getNodeType().equals(NodeType.SNAPSHOT)) &&
                !selectedItemsProperty.get(0).getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID));
        setText(selectedItemsProperty.get(0).getNodeType().equals(NodeType.CONFIGURATION) ?
                Messages.exportConfigurationLabel :
                Messages.exportSnapshotLabel);
    }
}
