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

/**
 * {@link javafx.scene.control.MenuItem} for finding references of a snapshot or composite snapshot.
 */
public class FindReferencesMenuItem extends SaveAndRestoreMenuItem {

    public FindReferencesMenuItem(SaveAndRestoreBaseController saveAndRestoreController,
                                  ObservableList<Node> selectedItemsProperty,
                                  Runnable onAction) {
        super(saveAndRestoreController, selectedItemsProperty, onAction);
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/sar-search_18x18.png"));
        setText(Messages.findSnapshotReferences);
    }

    @Override
    public void configure() {
        visibleProperty().set(selectedItemsProperty.size() == 1 &&
                (selectedItemsProperty.get(0).getNodeType().equals(NodeType.SNAPSHOT) ||
                        selectedItemsProperty.get(0).getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)));
    }
}
