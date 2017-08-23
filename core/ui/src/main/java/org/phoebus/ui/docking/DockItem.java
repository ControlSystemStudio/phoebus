/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** Item for a {@link DockPane}
 *
 *  <p>Technically a {@link Tab},
 *  should be treated as a new type of node,
 *  calling only
 *  <ul>
 *  <li>the methods declared in here
 *  <li>{@link Tab#setContent(javafx.scene.Node)},
 *  <li>{@link Tab#setClosable(boolean)}
 *  </ul>
 *  to allow changes to the internals.
 *
 *  @author Kay Kasemir
 */
class DockItem extends Tab
{
    private final static ImageView detach_icon;

    static
    {
        ImageView icon = null;
        try
        {
            icon = new ImageView(new Image(DockItem.class.getResourceAsStream("/icons/detach.png")));
        }
        catch (Throwable ex)
        {
            DockPane.logger.log(Level.WARNING, "Cannot obtain icon", ex);
        }
        detach_icon = icon;
    }

    /** The item that's currently being dragged
     *
     *  <p>The actual DockItem cannot be serialized
     *  for drag-and-drop,
     *  and since docking is limited to windows within
     *  the same JVM, this reference points to the item
     *  that's being dragged.
     */
    static final AtomicReference<DockItem> dragged_item = new AtomicReference<>();

    static final Border DROP_ZONE_BORDER = new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID,
                                                                       new CornerRadii(5.0), BorderStroke.MEDIUM));

    /** Drag-and-drop data format
     *
     *  Custom format to prevent dropping a tab into e.g. a text editor
     */
    private static final DataFormat DOCK_ITEM = new DataFormat("dock_item.custom");

    /** Label used for the Tab because Tab itself cannot participate in drag-and-drop */
    private final Label tab_name;

    /** Create dock item
     *  @param label Initial label
     */
    public DockItem(final String label)
    {
        // Create tab with no 'text',
        // instead using a Label for the text
        // because the label can react to drag operations
        tab_name = new Label(label);
        setGraphic(tab_name);

        tab_name.setOnDragDetected(this::handleDragDetected);
        tab_name.setOnDragOver(this::handleDragOver);
        tab_name.setOnDragEntered(this::handleDragEntered);
        tab_name.setOnDragExited(this::handleDragExited);
        tab_name.setOnDragDropped(this::handleDrop);
        tab_name.setOnDragDone(this::handleDragDone);

        final MenuItem detach = new MenuItem("Detach", detach_icon);
        detach.setOnAction(this::detach);
        final ContextMenu menu = new ContextMenu(detach);
        tab_name.setContextMenu(menu );
    }

    /** Label of this item */
    public String getLabel()
    {
        return tab_name.getText();
    }

    /** @param label Label of this item */
    public void setLabel(final String label)
    {
        tab_name.setText(label);
    }

    /**    Allow dragging this item */
    private void handleDragDetected(final MouseEvent event)
    {
        final Dragboard db = tab_name.startDragAndDrop(TransferMode.MOVE);

        final ClipboardContent content = new ClipboardContent();
        content.put(DOCK_ITEM, getLabel());
        db.setContent(content);

        final DockItem previous = dragged_item.getAndSet(this);
        if (previous != null)
        {
            System.err.println("Already dragging " + previous);
        }

        event.consume();

    }

    /** Accept other items that are dropped onto this one */
    private void handleDragOver(final DragEvent event)
    {
        final DockItem item = dragged_item.get();
        if (item != null  &&  item != this)
            event.acceptTransferModes(TransferMode.MOVE);
        event.consume();
    }

    /** Highlight while 'drop' is possible */
    private void handleDragEntered(final DragEvent event)
    {
        final DockItem item = dragged_item.get();
        if (item != null  &&  item != this)
        {
            tab_name.setBorder(DROP_ZONE_BORDER);
            tab_name.setTextFill(Color.GREEN);
        }
        event.consume();
    }

    /** Remove Highlight */
    private void handleDragExited(final DragEvent event)
    {
        tab_name.setBorder(Border.EMPTY);
        tab_name.setTextFill(Color.BLACK);
        event.consume();
    }

    /** Accept a dropped tab */
    private void handleDrop(final DragEvent event)
    {
        final DockItem item = dragged_item.get();
        if (item == null)
            System.err.println("Empty drop?");
        else
        {
            // System.out.println("Somebody dropped " + item + " onto " + this);
            final TabPane old_parent = item.getTabPane();
            final TabPane new_parent = getTabPane();

            // Unexpected, but would still "work" at this time
            if (! (old_parent instanceof DockPane))
                throw new IllegalStateException("Expected DockPane for " + item + ", got " + old_parent);
            if (! (new_parent instanceof DockPane))
                throw new IllegalStateException("Expected DockPane for " + item + ", got " + new_parent);

            if (new_parent != old_parent)
            {
                old_parent.getTabs().remove(item);
                // Insert after the item on which it was dropped
                final int index = new_parent.getTabs().indexOf(this);
                new_parent.getTabs().add(index+1, item);
            }
            else
            {
                final int index = new_parent.getTabs().indexOf(this);
                new_parent.getTabs().remove(item);
                // If item was 'left' of this, it will be added just after this.
                // If item was 'right' of this, it'll be added just before this.
                new_parent.getTabs().add(index, item);
            }
            // Select the new item
            new_parent.getSelectionModel().select(item);
        }
        event.setDropCompleted(true);
        event.consume();
    }

    /** Handle that this tab was dragged elsewhere, or drag aborted */
    private void handleDragDone(final DragEvent event)
    {
        dragged_item.set(null);
        event.consume();
    }

    private void detach(final ActionEvent event)
    {
        final Stage other = new Stage();

        final TabPane old_parent = getTabPane();

        // Unexpected, but would still "work" at this time
        if (! (old_parent instanceof DockPane))
            throw new IllegalStateException("Expected DockPane for " + this + ", got " + old_parent);

        old_parent.getTabs().remove(this);
        DockPane.configureStage(other, this);
    }

    @Override
    public String toString()
    {
        return "DockItem(\"" + getLabel() + "\")";
    }
}