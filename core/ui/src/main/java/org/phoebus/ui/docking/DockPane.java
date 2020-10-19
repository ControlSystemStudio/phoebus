/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Styles;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
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
    public static final Logger logger = Logger.getLogger(DockPane.class.getPackageName());

    final static Image close_icon = ImageCache.getImage(DockItem.class, "/icons/remove.png");

    private static CopyOnWriteArrayList<DockPaneListener> listeners = new CopyOnWriteArrayList<>();

    private static WeakReference<DockPane> active = new WeakReference<>(null);

    private static boolean always_show_tabs = true;

    private Stage stage;

    public void setStage(Stage stage){
        this.stage = stage;
    }

    public void closeStage(){
        stage.close();
        stage = null;
    }

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

    /** @param pane {@link DockPane}
     *  @return <code>false</code> if pane is fixed or not visible on screen
     */
    private static boolean isDockPaneUsable(final DockPane pane)
    {
        if (pane.isFixed())
            return false;
        if (pane.getScene() == null)
            return false;
        if (! pane.getScene().getWindow().isShowing())
            return false;
        return true;
    }

    /** @return The last known active dock pane */
    public static DockPane getActiveDockPane()
    {
        final DockPane pane = active.get();
        if (pane != null  &&  !isDockPaneUsable(pane))
        {
            // The Window for the previously active dock pane was closed
            // Use the first one that's still open
            for (Stage stage : DockStage.getDockStages())
                for (DockPane check : DockStage.getDockPanes(stage))
                    if (isDockPaneUsable(check))
                    {
                        setActiveDockPane(check);
                        return check;
                    }
        }
        return pane;
    }

    /** Set the 'active' dock pane
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
        active = new WeakReference<>(pane);

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

    /** Optional name of the pane */
    private String name = "";

    /** Is this dock pane 'fixed' ? */
    private boolean fixed = false;

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

        setOnContextMenuRequested(this::showContextMenu);
    }


    private void showContextMenu(final ContextMenuEvent event)
    {
        final ContextMenu menu = new ContextMenu();
        final ObservableList<MenuItem> items = menu.getItems();

        // If this pane is empty, offer 'name', 'un-lock', 'close' in context menu
        if (getTabs().isEmpty())
        {
            // Always possible to name a pane
            items.add(new NamePaneMenuItem(this));

            // If 'fixed', offer menu to un-lock.
            // Happens if content of a locked pane failed to load,
            // leaving an empty, locked pane to which nothing can be added until unlocked.
            if (isFixed())
                items.add(new UnlockMenuItem(this));
            else
            {
                // Not fixed, but empty.
                // Offer 'close', if possible.
                if (dock_parent instanceof SplitDock  &&
                    ((SplitDock) dock_parent).canMerge())
                {
                    final MenuItem close = new MenuItem(Messages.DockClose, new ImageView(close_icon));
                    close.setOnAction(evt ->
                    {
                        if (!getName().isBlank())
                        {
                            // Warn about named pane
                            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.initOwner(dock_parent.getScene().getWindow());
                            alert.setTitle(Messages.DockCloseNamedPaneTitle);
                            alert.setContentText(MessageFormat.format(Messages.DockCloseNamedPaneText, getName()));
                            alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                            DialogHelper.positionDialog(alert, this, 0, 0);
                            alert.showAndWait().ifPresent(type ->
                            {
                                if (type == ButtonType.NO)
                                    return; // Keep open
                                // Turn into un-named pane so it'll be merged
                                setName("");
                            });
                        }
                        // Merge/close unnamed panes
                        mergeEmptyAnonymousSplit();
                    });
                    items.addAll(new SeparatorMenuItem(), close);
                }
            }
        }

        if (! items.isEmpty())
            menu.show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
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

    /** @param name Name, may not be <code>null</code> */
    public void setName(final String name)
    {
        this.name = Objects.requireNonNull(name);
    }

    /** @return Name of this pane, may be empty */
    public String getName()
    {
        return name;
    }

    /** @param fixed Mark as 'fixed', i.e. tabs cannot be added/removed/closed? */
    public void setFixed(final boolean fixed)
    {
        this.fixed = fixed;
        // Prevent closing items in 'fixed' pane
        for (DockItem tab : getDockItems())
            tab.setClosable(! fixed);
    }

    /** @return Is this pane 'fixed', i.e. tabs cannot be added/removed/closed? */
    public boolean isFixed()
    {
        return fixed;
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
            event.consume();
        }
        else if (key == KeyCode.W)
        {
            if (!isFixed())
            {
                JobManager.schedule("Close " + item.getLabel(), monitor ->
                {
                    if (item.prepareToClose())
                        Platform.runLater(item::close);
                });
            }
            event.consume();
        }
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
        logger.log(Level.INFO, "DockPane handling tab changes");
        // Schedule merge on later UI tick.
        // That way an ongoing scene graph change that might move
        // (i.e. remove item and then add it elsewhere)
        // can complete before we merge,
        // instead of remove, merge, .. add fails because scene graph
        // change in unforeseen ways
        if (getTabs().isEmpty())
            Platform.runLater(this::mergeEmptyAnonymousSplit);
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

    /** Somewhat hacky:
     *  Need the scene of this dock pane to adjust the style sheet
     *  or to interact with the Window.
     *
     *  We _have_ added this DockPane to a scene graph, so getScene() should
     *  return the scene.
     *  But if this dock pane is nested inside a newly created {@link SplitDock},
     *  it will not have a scene until it is rendered.
     *  So keep deferring to the next UI pulse until there is a scene.
     *  @param user_of_scene Something that needs to run once there is a scene
     */
    public void deferUntilInScene(final Consumer<Scene> user_of_scene)
    {
        // Tried to optimize this based on
        //     sceneProperty().addListener(...),
        // creating list of registered users_of_scene,
        // invoking once the scene property changes to != null,
        // then deleting the list and removing the listener,
        // but that added quite some code and failed for
        // strange endless-loop type reasons.
        deferUntilInScene(0, user_of_scene);
    }

    // See deferUntilInScene, giving up after 10 attempts
    private void deferUntilInScene(final int level, final Consumer<Scene> user_of_scene)
    {
        if (getScene() != null)
            user_of_scene.accept(getScene());
        else if (level < 10)
            Platform.runLater(() -> deferUntilInScene(level+1, user_of_scene));
        else
            logger.log(Level.WARNING, this + " has no scene for deferred call to " + user_of_scene);
    }

    /** Hide or show tabs
     *
     *  <p>When there's more than one tab, or always_show_tabs,
     *  then show the tabs.
     *  If there's just one tab, and ! always_show_tabs, hide that one tab
     *  to get a more compact UI.
     */
    private void autoHideTabs()
    {
        // Anything to update?
        // This also handles the case where called on disposed DockPane:
        // No scene (which would cause NPE), but then also no tabs.
        if (getTabs().isEmpty())
            return;

        deferUntilInScene(this::doAutoHideTabs);
    }

    private void doAutoHideTabs(final Scene scene)
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
        // and this is the only tab in the window,
        // put its label into the window tile
        if (! (scene.getWindow() instanceof Stage))
            throw new IllegalStateException("Expect Stage, got " + scene.getWindow());
        final Stage stage = ((Stage) scene.getWindow());
        if (do_hide  &&  DockStage.getPaneOrSplit(stage) == this)
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
            stage.setTitle(Messages.FixedTitle);
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
        if (!isFixed()  &&
            DockItem.dragged_item.get() != null)
            event.acceptTransferModes(TransferMode.MOVE);
        event.consume();
    }

    /** Highlight while 'drop' is possible */
    private void handleDragEntered(final DragEvent event)
    {
        if (!isFixed()  &&
            DockItem.dragged_item.get() != null)
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
            logger.log(Level.INFO, "Somebody dropped " + item + " into " + this);
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

            // Move tab. In principle,
            // (1) first remove from old parent,
            // (2) then add to new parent.
            // But modifying tabs triggers tab listener, which registers SplitPane.merge()
            // in Platform.runLater(). The merge could re-arrange tab panes,
            // we when we later want to add the tab, we'll face a different scene graph.
            // Issue the tab addition (2) with runlater right now so it'll happen before any
            // split pane cleanup.
            Platform.runLater(() ->
            {
                // When adding the tab to its new parent (this dock) right away,
                // the tab would sometimes not properly render until the pane is resized.
                // Moving to the next UI tick helps
                logger.log(Level.INFO, "Adding " + item + " to " + this);
                addTab(item);
                Platform.runLater(this::autoHideTabs);
            });

            // With tab addition already in the UI thread queue, remove item from old tab
            logger.log(Level.INFO, "Removing " + item + " from " + old_parent);
            old_parent.getTabs().remove(item);
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
            // Remove this dock pane from parent
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

    /** Split this dock pane horizontally, creating a new named pane to the right
     *  @param name Name of the new pane
     *  @return That new, named DockPane on the right
     */
    public DockPane split(final String name)
    {
        final SplitDock split = split(true);
        final DockPane new_pane = (DockPane) split.getItems().get(1);
        new_pane.setName(name);
        return new_pane;
    }

    /** If this pane is within a SplitDock, not named, and empty, merge! */
    void mergeEmptyAnonymousSplit()
    {
        if (! (dock_parent instanceof SplitDock))
        {
            Platform.runLater(this::applyEmptyDockPanePolicy);
            return;
        }
        if (name.length() > 0)
            return;

        ((SplitDock) dock_parent).merge();
        Platform.runLater(this::applyEmptyDockPanePolicy);
    }

    @Override
    public String toString()
    {
        return (isFixed() ? "FIXED DockPane " : "DockPane ") +
               Integer.toHexString(System.identityHashCode(this)) + " '" + name + "' "+ getTabs();
    }

    /** Closes empty windows.
     *  
     *  <p>Windows become empty when all tabs have been dragged out, or closed explicitly.
     *  The main window is never closed, though.
     */
    private void applyEmptyDockPanePolicy()
    {
        final Scene scene = getScene();
        if(scene == null)
            return;

        final Object id = scene.getWindow().getProperties().get(DockStage.KEY_ID);
        if (!DockStage.ID_MAIN.equals(id))
            if (!SplitDock.class.isInstance(dock_parent))
                getScene().getWindow().hide();
    }
}
