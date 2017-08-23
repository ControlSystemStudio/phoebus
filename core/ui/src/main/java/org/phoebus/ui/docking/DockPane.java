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

import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Pane that contains {@link DockItem}s
 *
 *  <p>Implemented as {@link TabPane},
 *  but this might change so only methods
 *  declared in here should be invoked
 *
 *  @author Kay Kasemir
 */
public class DockPane extends TabPane
{
    /** Logger for all docking related messages */
    public static final Logger logger = Logger.getLogger(DockPane.class.getName());

    private DockPane(final DockItem... tabs)
    {
        super(tabs);

        // Allow dropping a DockItem
        setOnDragOver(this::handleDragOver);
        setOnDragEntered(this::handleDragEntered);
        setOnDragExited(this::handleDragExited);
        setOnDragDropped(this::handleDrop);
    }

    /** Helper to configure a Stage for docking
     *
     *  <p>Adds a Scene with one DockPane
     *
     *  @param stage Stage, should be empty
     *  @param tabs Zero or more initial {@link DockItem}s
     *  @throws Exception on error
     *
     *  @return {@link DockPane} that was added to the {@link Stage}
     */
    public static DockPane configureStage(final Stage stage, final DockItem... tabs)
    {
        final DockPane tab_pane = new DockPane(tabs);

        final StackPane stack_pane = new StackPane();
        stack_pane.getChildren().add(tab_pane);

        final Scene scene = new Scene(stack_pane);

        stage.setScene(scene);
        stage.setTitle("Phoebus");
        try
        {
            stage.getIcons().add(new Image(DockPane.class.getResourceAsStream("/icons/logo.png")));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot set application icon", ex);
        }
        stage.show();

        return tab_pane;
    }

    /** @param tabs One or more tabs to add */
    public void addTab(final DockItem... tabs)
    {
		getTabs().addAll(tabs);
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
            System.err.println("Empty drop?");
        else
        {
            final TabPane old_parent = item.getTabPane();

            // Unexpected, but would still "work" at this time
            if (! (old_parent instanceof DockPane))
                System.err.println("DockItem is not in DockPane");

            old_parent.getTabs().remove(item);
            getTabs().add(item);
            // Select the new item
            getSelectionModel().select(item);
        }
        event.setDropCompleted(true);
        event.consume();
    }
}
