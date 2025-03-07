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

public class RenameFolderMenuItem extends SaveAndRestoreMenuItem {

    public RenameFolderMenuItem(SaveAndRestoreBaseController saveAndRestoreBaseController,
                                ObservableList<Node> selectedItemsProperty,
                                Runnable onAction) {
        super(saveAndRestoreBaseController, selectedItemsProperty, onAction);
        setText(Messages.contextMenuRename);
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/rename_col.png"));
    }

    @Override
    public void configure() {
        disableProperty().set(saveAndRestoreBaseController.getUserIdentity().isNull().get() ||
                selectedItemsProperty.size() != 1 ||
                !selectedItemsProperty.get(0).getNodeType().equals(NodeType.FOLDER) ||
                selectedItemsProperty.get(0).getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID));
    }
}
