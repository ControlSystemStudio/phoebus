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

public class PasteMenuItem extends SaveAndRestoreMenuItem {

    public PasteMenuItem(SaveAndRestoreController saveAndRestoreController,
                         ObservableList<Node> selectedItemsProperty,
                         Runnable onAction) {
        super(saveAndRestoreController, selectedItemsProperty, onAction);
        setText(Messages.paste);
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/paste.png"));
    }

    @Override
    public void configure() {
        disableProperty().set(saveAndRestoreController.getUserIdentity().isNull().get() ||
                selectedItemsProperty.size() != 1 ||
                selectedItemsProperty.get(0).getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID) ||
                (!selectedItemsProperty.get(0).getNodeType().equals(NodeType.FOLDER) &&
                !selectedItemsProperty.get(0).getNodeType().equals(NodeType.CONFIGURATION)) ||
                !saveAndRestoreController.mayPaste());
    }
}
