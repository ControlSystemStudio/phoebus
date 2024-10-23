/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.contextmenu;

import javafx.collections.ObservableList;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.ui.javafx.ImageCache;

import java.util.function.Consumer;


public class NewFolderMenuItem extends SaveAndRestoreMenuItem {

    public NewFolderMenuItem(SaveAndRestoreController saveAndRestoreController, ObservableList<Node> selectedItems, Consumer onAction) {
        super(saveAndRestoreController, selectedItems, onAction);
        setText(Messages.contextMenuNewFolder);
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/save-and-restore/folder.png"));
    }

    @Override
    public void configure() {
        visibleProperty().set(selectedItemsProperty.size() == 1 && selectedItemsProperty.get(0).getNodeType().equals(NodeType.FOLDER));
        disableProperty().set(saveAndRestoreController.getUserIdentity().isNull().get() || selectedItemsProperty.size() > 1);
    }
}
