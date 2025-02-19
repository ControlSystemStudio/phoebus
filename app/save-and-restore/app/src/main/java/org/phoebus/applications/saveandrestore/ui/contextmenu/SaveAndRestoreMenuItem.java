/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.contextmenu;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;

import java.util.function.Consumer;

/**
 * Base class for {@link MenuItem}s added to the {@link javafx.scene.control.TreeView} context menu. The idea is
 * to let subclasses assume all control of look and behavior (enable/disable {@link MenuItem}).
 */
public abstract class SaveAndRestoreMenuItem extends MenuItem {

    protected SaveAndRestoreBaseController saveAndRestoreBaseController;
    protected ObservableList<Node> selectedItemsProperty;
    protected Consumer<Void> onAction;

    /**
     * Constructor
     * @param saveAndRestoreBaseController Reference to the {@link SaveAndRestoreController} as it provides functionality
     *                                 needed to determine if and how to render this {@link MenuItem}.
     * @param selectedItemsProperty An {@link ObservableList} property objects of this class will listen on. When
     *                              a change is detected, the {@link #configure()} is called.
     * @param onAction Client provided {@link Runnable} defining the action of the {@link MenuItem}.
     */
    public SaveAndRestoreMenuItem(SaveAndRestoreBaseController saveAndRestoreBaseController,
                                  ObservableList<Node> selectedItemsProperty,
                                  Runnable onAction) {
        this.saveAndRestoreBaseController = saveAndRestoreBaseController;
        this.selectedItemsProperty = selectedItemsProperty;
        ListChangeListener<Node> l = change -> configure();
        this.selectedItemsProperty.addListener(l);
        if(onAction != null){
            setOnAction(ae -> onAction.run());
        }
    }

    /**
     * Subclasses need to override this to define the dynamic behavior, i.e. whether to enable or disable, or
     * even hide this {@link MenuItem}.
     */
    protected abstract void configure();
}
