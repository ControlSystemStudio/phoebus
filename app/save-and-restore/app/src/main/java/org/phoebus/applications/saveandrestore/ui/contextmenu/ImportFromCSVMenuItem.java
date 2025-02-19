/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.contextmenu;

import javafx.collections.ObservableList;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.ui.javafx.ImageCache;

public class ImportFromCSVMenuItem extends SaveAndRestoreMenuItem {

    public ImportFromCSVMenuItem(SaveAndRestoreBaseController saveAndRestoreController,
                                 ObservableList<Node> selectedItemsProperty,
                                 Runnable onAction) {
        super(saveAndRestoreController, selectedItemsProperty, onAction);
        // Set text in configure()
        setGraphic(new ImageView(ImageCache.getImage(ImportFromCSVMenuItem.class, "/icons/csv_import.png")));
    }

    @Override
    public void configure() {
        disableProperty().set(saveAndRestoreBaseController.getUserIdentity().isNull().get() ||
                selectedItemsProperty.size() != 1 ||
                selectedItemsProperty.get(0).getNodeType().equals(NodeType.SNAPSHOT) ||
                selectedItemsProperty.get(0).getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT) ||
                selectedItemsProperty.get(0).getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID));
        setText(selectedItemsProperty.get(0).getNodeType().equals(NodeType.FOLDER) ?
                Messages.importConfigurationLabel :
                Messages.importSnapshotLabel);
    }
}
