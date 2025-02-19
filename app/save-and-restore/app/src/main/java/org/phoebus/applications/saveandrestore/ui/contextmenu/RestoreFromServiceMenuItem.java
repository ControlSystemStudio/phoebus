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

public class RestoreFromServiceMenuItem extends SaveAndRestoreMenuItem {

    public RestoreFromServiceMenuItem(SaveAndRestoreBaseController saveAndRestoreBaseController, ObservableList<Node> selectedItemsProperty, Runnable onAction) {
        super(saveAndRestoreBaseController, selectedItemsProperty, onAction);
        setText(Messages.restoreFromService);
        setGraphic(ImageCache.getImageView(RestoreFromServiceMenuItem.class, "/icons/save-and-restore/restore.png"));
    }

    @Override
    public void configure() {
        disableProperty().set(saveAndRestoreBaseController.getUserIdentity().isNull().get() ||
                selectedItemsProperty.size() != 1 ||
                (!selectedItemsProperty.get(0).getNodeType().equals(NodeType.SNAPSHOT) &&
                        !selectedItemsProperty.get(0).getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)));
    }
}
