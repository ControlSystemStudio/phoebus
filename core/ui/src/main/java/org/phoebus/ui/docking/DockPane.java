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

import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;

/** Pane that contains {@link DockItem}s
 *
 *  <p>Implemented as {@link TabPane},
 *  but this might change so only methods
 *  declared in here should be invoked
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DockPane extends TabPane
{
    /** Logger for all docking related messages */
    public static final Logger logger = Logger.getLogger(DockPane.class.getName());

    // Only accessible within this package (DockStage)
    DockPane(final DockItem... tabs)
    {
        super(tabs);

        // Allow dropping a DockItem
        setOnDragOver(this::handleDragOver);
        setOnDragEntered(this::handleDragEntered);
        setOnDragExited(this::handleDragExited);
        setOnDragDropped(this::handleDrop);
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
            // Select the new item
            getSelectionModel().select(item);
        }
        event.setDropCompleted(true);
        event.consume();
    }
}
