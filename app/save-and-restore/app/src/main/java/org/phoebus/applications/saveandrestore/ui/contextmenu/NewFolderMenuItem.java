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


public class NewFolderMenuItem extends SaveAndRestoreMenuItem {

    public NewFolderMenuItem(SaveAndRestoreBaseController saveAndRestoreBaseController, ObservableList<Node> selectedItems, Runnable onAction) {
        super(saveAndRestoreBaseController, selectedItems, onAction);
        setText(Messages.contextMenuNewFolder);
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/save-and-restore/folder.png"));
    }

    @Override
    public void configure() {
        visibleProperty().set(selectedItemsProperty.size() == 1 && selectedItemsProperty.get(0).getNodeType().equals(NodeType.FOLDER));
        disableProperty().set(saveAndRestoreBaseController.getUserIdentity().isNull().get() || selectedItemsProperty.size() > 1);
    }
}
