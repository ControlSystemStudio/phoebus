/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.contextmenu;

import javafx.collections.ObservableList;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.ui.javafx.ImageCache;

import java.util.function.Consumer;

public class CopyUniqueIdToClipboardMenuItem extends SaveAndRestoreMenuItem {

    public CopyUniqueIdToClipboardMenuItem(SaveAndRestoreController saveAndRestoreController, ObservableList<Node> selectedItemsProperty, Consumer onAction) {
        super(saveAndRestoreController, selectedItemsProperty, onAction);
        setText(Messages.copyUniqueIdToClipboard);
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/copy.png"));
    }

    @Override
    public void configure() {
        disableProperty().set(selectedItemsProperty.size() != 1);
    }
}
