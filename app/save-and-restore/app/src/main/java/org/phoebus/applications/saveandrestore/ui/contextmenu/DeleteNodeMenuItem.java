/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.contextmenu;

import javafx.collections.ObservableList;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.ui.javafx.ImageCache;

public class DeleteNodeMenuItem extends SaveAndRestoreMenuItem {

    public DeleteNodeMenuItem(SaveAndRestoreBaseController saveAndRestoreController, ObservableList<Node> selectedItemsProperty, Runnable onAction) {
        super(saveAndRestoreController, selectedItemsProperty, onAction);
        setText(Messages.contextMenuDelete);
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
    }

    @Override
    public void configure() {
        disableProperty().set(saveAndRestoreController.getUserIdentity().isNull().get() ||
                selectedItemsProperty.stream().anyMatch(n -> n.getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID)) ||
                !((SaveAndRestoreController) saveAndRestoreController).hasSameParent());
    }
}
