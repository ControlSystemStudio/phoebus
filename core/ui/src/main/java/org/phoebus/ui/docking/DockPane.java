/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.ui.jobs.JobManager;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.event.EventType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Pane that contains {@link DockItem}s
 *
 *  <p>Implemented as {@link TabPane},
 *  but this might change so only methods
 *  declared in here should be invoked.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DockPane extends TabPane
{
    /** Logger for all docking related messages */
    public static final Logger logger = Logger.getLogger(DockPane.class.getName());
    
    private static DockPane active = null;

    /** @return The last known active dock pane */
    public static DockPane getActiveDockPane()
    {
        return active;
    }

    // Called by DockStage within pachage
    static void setActiveDockPane(final DockPane pane)
    {
        active = pane;
    }

    // Only accessible within this package (DockStage)
    DockPane(final DockItem... tabs)
    {
        super(tabs);

        // Allow dropping a DockItem
        setOnDragOver(this::handleDragOver);
        setOnDragEntered(this::handleDragEntered);
        setOnDragExited(this::handleDragExited);
        setOnDragDropped(this::handleDrop);
        
        // This pane, just opened, is the active one for now
        active = this;

        // Track changes of active tab, remember its pane as active
        getSelectionModel().selectedItemProperty().addListener((p, old, tab) ->
        {
            final DockItem item = (DockItem) tab;
            active = item == null  ?  null  :  (DockPane)item.getTabPane();
        });

        // Show/hide tabs as tab count changes
        getTabs().addListener((InvalidationListener) change -> autoHideTabs());
    }
    
    // lookup() in autoHideTabs() only works when the scene has been rendered.
    // Before, it returns null
    // There is no event for 'node has been rendered',
    // but overriding layoutChildren() allows to detect that point in time.
    @Override
    protected void layoutChildren()
    {
        // Perform initial autoHideTabs
        autoHideTabs();
        super.layoutChildren();
    }

    public void autoHideTabs()
    {
        // Hack from https://www.snip2code.com/Snippet/300911/A-trick-to-hide-the-tab-area-in-a-JavaFX:
        final StackPane header = (StackPane) lookup(".tab-header-area");
        final boolean single = getTabs().size() == 1;
        if (header != null)
            header.setPrefHeight(single  ?  0  :  -1);

        // If header for single tab is not shown,
        // put its label into the window tile
        final Stage stage = ((Stage) getScene().getWindow());
        if (single)
        {   // Bind to get actual header, which for DockItemWithInput may contain 'dirty' marker,
            // and keep updating as it changes
            stage.titleProperty().bind(((DockItem)getTabs().get(0)).getNameNode().textProperty());
        }
        else
        {   // Fixed title
            stage.titleProperty().unbind();
            stage.setTitle("Phoebus");
        }
    }

    /** @param tabs One or more tabs to add */
    public void addTab(final DockItem... tabs)
    {
        getTabs().addAll(tabs);
        // Select the newly added tab
        getSelectionModel().select(getTabs().size()-1);
    }

    /** Accept dock items */
    private void handleDragOver(final DragEvent event)
    {
        if (DockItem.dragged_item.get() != null)
            event.acceptTransferModes(TransferMode.MOVE);
        event.consume();
    }

    /** Highlight while 'drop' is possible */
    private void handleDragEntered(final DragEvent event)
    {
        if (DockItem.dragged_item.get() != null)
            setBorder(DockItem.DROP_ZONE_BORDER);
        event.consume();
    }

    /** Remove Highlight */
    private void handleDragExited(final DragEvent event)
    {
        setBorder(Border.EMPTY);
        event.consume();
    }

    /** Accept a dropped tab */
    private void handleDrop(final DragEvent event)
    {
        final DockItem item = DockItem.dragged_item.get();
        if (item == null)
            logger.log(Level.SEVERE, "Empty drop, " + event);
        else
        {
            final TabPane old_parent = item.getTabPane();

            // Unexpected, but would still "work" at this time
            if (! (old_parent instanceof DockPane))
                logger.log(Level.SEVERE, "DockItem is not in DockPane but " + old_parent);

            old_parent.getTabs().remove(item);
            getTabs().add(item);
            autoHideTabs();

            // Select the new item
            getSelectionModel().select(item);
        }
        event.setDropCompleted(true);
        event.consume();
    }

    @Override
    public String toString()
    {
        return "DockPane " + getTabs();
    }
}
