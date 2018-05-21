/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.javafx.Styles;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
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

    private static CopyOnWriteArrayList<DockPaneListener> listeners = new CopyOnWriteArrayList<>();

    private static DockPane active = null;

    private static boolean always_show_tabs = true;

    /** @param listener Listener to add
     *  @throws IllegalStateException if listener already added
     */
    public static void addListener(final DockPaneListener listener)
    {
        if (! listeners.addIfAbsent(listener))
            throw new IllegalStateException("Duplicate listener");
    }

    /** @param listener Listener to remove
     *  @throws IllegalStateException if listener not known
     */
    public static void removeListener(final DockPaneListener listener)
    {
        if (! listeners.remove(listener))
            throw new IllegalStateException("Unknown listener");
    }

    /** @return The last known active dock pane */
    public static DockPane getActiveDockPane()
    {
        if (active.getScene().getWindow().isShowing())
        {
            // The Window for the previously active dock pane was closed
            // Use the first one that's still open
            for (Stage stage : DockStage.getDockStages())
            {
                active = DockStage.getDockPanes(stage).get(0);
                break;
            }
        }
        return active;
    }

    /** Set the 'active' dock stage
     *
     *  <p>Called within the phoebus framework,
     *  for example by DockStage or when restoring
     *  memento.
     *  User code should not call, because framework
     *  automatically tracks the current dock pane
     *  and item.
     *
     *  @param pane Active DockPane
     */
    public static void setActiveDockPane(final DockPane pane)
    {
        active = pane;

        final DockItem item = (DockItem) pane.getSelectionModel().getSelectedItem();
        for (DockPaneListener listener : listeners)
            listener.activeDockItemChanged(item);
    }

    /** @return true if even single tab is shown */
    public static boolean isAlwaysShowingTabs()
    {
        return always_show_tabs;
    }

    /** @param do_show_single_tabs Should even a single tab be shown? */
    public static void alwaysShowTabs(final boolean do_show_single_tabs)
    {
        if (always_show_tabs == do_show_single_tabs)
            return;
        always_show_tabs = do_show_single_tabs;
        for (Stage stage : DockStage.getDockStages())
            for (DockPane pane : DockStage.getDockPanes(stage))
            {
                pane.autoHideTabs();
                pane.requestLayout();
            }
    }

    /** In principle, 'getParent()' provides the parent of a node,
     *  which should either be a {@link BorderPane} or a {@link SplitDock}.
     *  JFX, however, will only update the parent when the node is rendered.
     *  While assembling or updating the scene, getParent() can return null.
     *  Further, a node added to a SplitDock (SplitPane) will actually have
     *  a SplitPaneSkin$Content as a parent and not a SplitPane let alone SplitDock.
     *
     *  We therefore track the parent that matters for our needs
     *  in the user data under this key.
     */
    private Parent dock_parent = null;

    /** Create DockPane
     *  @param tabs
     */
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
        setActiveDockPane(this);

        // Track changes of active tab, remember its pane as active
        getSelectionModel().selectedItemProperty().addListener((p, old, tab) ->
        {
            final DockItem item = (DockItem) tab;
            if (item == null)
                return;
            final DockPane pane = (DockPane)item.getTabPane();
            if (pane != null)
                setActiveDockPane(pane);
        });

        addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalKeys);

        // Show/hide tabs as tab count changes
        getTabs().addListener((InvalidationListener) change -> handleTabChanges());
    }

    /** @param dock_parent {@link BorderPane}, {@link SplitDock} or <code>null</code> */
    void setDockParent(final Parent dock_parent)
    {
        if (dock_parent == null ||
            dock_parent instanceof BorderPane  ||
            dock_parent instanceof SplitDock)
            this.dock_parent = dock_parent;
        else
            throw new IllegalArgumentException("Expect BorderPane or SplitDock, got " + dock_parent);
    }

    /** Handle key presses of global significance like Ctrl-S to save */
    private void handleGlobalKeys(final KeyEvent event)
    {
        // Check for Ctrl (Windows, Linux) resp. Command (Mac)
        if (! event.isShortcutDown())
            return;

        final DockItem item = (DockItem) getSelectionModel().getSelectedItem();
        if (item == null)
            return;

        final KeyCode key = event.getCode();
        if (key == KeyCode.S)
        {
            if (item instanceof DockItemWithInput)
            {
                final DockItemWithInput active_item_with_input = (DockItemWithInput) item;
                if (active_item_with_input.isDirty())
                    JobManager.schedule(Messages.Save, monitor -> active_item_with_input.save(monitor));
            }
        }
        else if (key == KeyCode.W)
            item.close();
    }

    // lookup() in findTabHeader/autoHideTabs only works when the scene has been rendered.
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

    /** Called when number of tabs changed */
    private void handleTabChanges()
    {
        if (getTabs().isEmpty())
            mergeEmptySplit();
        else
            // Update tabs on next UI tick so that findTabHeader() can succeed
            // in case this is in a newly created SplitDock
            Platform.runLater(this::autoHideTabs);
    }

    private StackPane findTabHeader()
    {
        // Need to locate the header for _this_ pane.
        // lookup() will locate the header of the first tab anywhere down the scene graph.
        // lookupAll() locates them all, so identify the correct one based on its parent.
        for (Node header : lookupAll(".tab-header-area"))
            if (header instanceof StackPane  &&  header.getParent() == this)
                return (StackPane) header;
        return null;
    }


    void autoHideTabs()
    {
        final boolean do_hide = getTabs().size() == 1  &&  !always_show_tabs;

        // Hack from https://www.snip2code.com/Snippet/300911/A-trick-to-hide-the-tab-area-in-a-JavaFX :
        // Locate the header's pane and set height to zero
        final StackPane header = findTabHeader();
        if (header == null)
            logger.log(Level.WARNING, "Cannot locate tab header for " + getTabs());
        else
            header.setPrefHeight(do_hide  ?  0  :  -1);

        // If header for single tab is not shown,
        // put its label into the window tile
        if (! (getScene().getWindow() instanceof Stage))
            throw new IllegalStateException("Expect Stage, got " + getScene().getWindow());
        final Stage stage = ((Stage) getScene().getWindow());
        if (do_hide)
        {   // Bind to get actual header, which for DockItemWithInput may contain 'dirty' marker,
            // and keep updating as it changes
            final Tab tab = getTabs().get(0);
            if (! (tab instanceof DockItem))
                throw new IllegalStateException("Expected DockItem, got " + tab);
            stage.titleProperty().bind(((DockItem)tab).labelTextProperty());
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

    /** @return All {@link DockItem}s in this pane (safe copy) */
    public List<DockItem> getDockItems()
    {
        return getTabs().stream()
                        .map(tab -> (DockItem) tab)
                        .collect(Collectors.toList());
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
        final DockItem item = DockItem.dragged_item.getAndSet(null);
        if (item == null)
            logger.log(Level.SEVERE, "Empty drop, " + event);
        else
        {
            final TabPane old_parent = item.getTabPane();

            // Unexpected, but would still "work" at this time
            if (! (old_parent instanceof DockPane))
                logger.log(Level.SEVERE, "DockItem is not in DockPane but " + old_parent);

            // When moving to a new scene,
            // assert that styles used in old scene are still available
            final Scene old_scene = old_parent.getScene();
            final Scene scene = getScene();
            if (scene != old_scene)
                for (String css : old_scene.getStylesheets())
                    Styles.set(scene, css);

            // Move item to new tab
            old_parent.getTabs().remove(item);
            getTabs().add(item);

            autoHideTabs();

            // Select the new item
            getSelectionModel().select(item);
        }
        event.setDropCompleted(true);
        event.consume();
    }

    /** Split this dock pane
     *  @param horizontally <code>true</code> for horizontal, else vertical split
     *  @return SplitDock, which contains this dock pane as first (top, left) item, and a new DockPane as the second (bottom, left) item
     */
    public SplitDock split(final boolean horizontally)
    {
        final SplitDock split;
        if (dock_parent instanceof BorderPane)
        {
            final BorderPane parent = (BorderPane) dock_parent;
            // Remove this dock pane from BorderPane
            parent.setCenter(null);
            // Place in split alongside a new dock pane
            final DockPane new_pane = new DockPane();
            split = new SplitDock(parent, horizontally, this, new_pane);
            setDockParent(split);
            new_pane.setDockParent(split);
            // Place that new split in the border pane
            parent.setCenter(split);
        }
        else if (dock_parent instanceof SplitDock)
        {
            final SplitDock parent = (SplitDock) dock_parent;
            // Remove this dock pane from BorderPane
            final boolean first = parent.removeItem(this);
            // Place in split alongside a new dock pane
            final DockPane new_pane = new DockPane();
            split = new SplitDock(parent, horizontally, this, new_pane);
            setDockParent(split);
            new_pane.setDockParent(split);
            // Place that new split in the border pane
            parent.addItem(first, split);
        }
        else
            throw new IllegalStateException("Cannot split, dock_parent is " + dock_parent);
        return split;
    }

    /** If this pane is within a SplitDock and empty, merge */
    void mergeEmptySplit()
    {
        if (! (dock_parent instanceof SplitDock))
            return;
        ((SplitDock) dock_parent).merge();
    }

    @Override
    public String toString()
    {
        return "DockPane " + getTabs();
    }
}
