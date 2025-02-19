/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.contextmenu;

import javafx.collections.ObservableList;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.ui.javafx.ImageCache;

public class LoginMenuItem extends SaveAndRestoreMenuItem {

    public LoginMenuItem(SaveAndRestoreBaseController saveAndRestoreBaseController, ObservableList<Node> selectedItemsProperty, Runnable onAction) {
        super(saveAndRestoreBaseController, selectedItemsProperty, onAction);
        setText(Messages.login);
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/credentials.png"));
    }

    @Override
    public void configure() {
        visibleProperty().set(saveAndRestoreBaseController.getUserIdentity().isNull().get());
    }
}
