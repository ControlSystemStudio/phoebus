/*******************************************************************************
 * Copyright (c) 2017-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
import javafx.scene.paint.Color;
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

    /** Drop zone within a {@link DockPane} as determined by mouse position during a drag */
    enum DropZone { CENTER, LEFT, RIGHT, TOP, BOTTOM }

    /** Fraction of pane width/height from each edge that acts as a split drop zone */
    private static final double SPLIT_ZONE_FRACTION = 0.25;

    /** Fallback tab-strip height used before the first layout pass completes.
     *  Matches the default JavaFX TabPane header height at 100% scaling. */
    private static final double DEFAULT_TAB_BAR_HEIGHT = 35.0;

    private static CopyOnWriteArrayList<DockPaneListener> listeners = new CopyOnWriteArrayList<>();

    private static WeakReference<DockPane> active = new WeakReference<>(null);

    private static boolean always_show_tabs = true;

    private List<DockPaneEmptyListener> dockPaneEmptyListeners = new ArrayList<>();

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

    /** Drop zone last seen under the mouse — used only to skip redundant border redraws in handleDragOver */
    private DropZone active_drop_zone = DropZone.CENTER;

    /** Bottom Y of the tab header strip in DockPane-local coordinates.
     *  Drops at or above this line merge as a tab rather than split.
     *  Updated in handleTabChanges() so it stays accurate without a scene-graph
     *  walk on every DRAG_OVER event. */
    private double tab_bar_bottom = DEFAULT_TAB_BAR_HEIGHT;

    /** Create DockPane
     *  @param tabs
     */
    // Only accessible within this package (DockStage)
    DockPane(final DockItem... tabs)
    {
        super(tabs);

        // Show 'x' to close on all tabs
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);

        // Allow dropping a DockItem.
        //
        // DRAG_OVER and DRAG_DROPPED use capture-phase filters.  When the pointer
        // is over a DockItem tab-label, the filter does NOT consume the event so
        // DockItem's own handlers can fire (green highlight, swap/insert logic).
        // When the pointer is over the pane body or empty tab-bar space, the filter
        // consumes the event and DockPane handles the drop (merge or split).
        //
        // DRAG_ENTERED/EXITED use bubble handlers so they only fire when the drag
        // truly enters/exits DockPane, not for every child-node transition.
        addEventFilter(DragEvent.DRAG_OVER,    this::handleDragOver);
        setOnDragEntered(this::handleDragEntered);
        setOnDragExited(this::handleDragExited);
        addEventFilter(DragEvent.DRAG_DROPPED, this::handleDrop);

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

        getSelectionModel().selectedItemProperty().addListener((observable, previous_item, new_item) -> {
            Platform.runLater(() -> {
                // Keep track of the order of focus of tabs:
                if (new_item != null) {
                    tabsInOrderOfFocus.remove(new_item);
                    tabsInOrderOfFocus.push((DockItem) new_item);
                }
            });
        });
    }

    protected LinkedList<DockItem> tabsInOrderOfFocus = new LinkedList<>();

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
    public void setDockParent(final Parent dock_parent)
    {
        if (dock_parent == null ||
            dock_parent instanceof BorderPane  ||
            dock_parent instanceof SplitDock   ||
            dock_parent instanceof SplitPane) // "dock_parent instanceof SplitPane" is for the case of the ESS-specific Navigator application running
            this.dock_parent = dock_parent;
        else
            throw new IllegalArgumentException("Expected BorderPane or SplitDock or SplitPane, got " + dock_parent);
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

                if (event.isShiftDown()) {
                    JobManager.schedule(Messages.SaveAs, monitor -> active_item_with_input.save_as(monitor, active_item_with_input.getTabPane().getScene().getWindow()));
                }
                else if (active_item_with_input.isDirty()) {
                    JobManager.schedule(Messages.Save, monitor -> active_item_with_input.save(monitor, active_item_with_input.getTabPane().getScene().getWindow()));
                }
            }
            event.consume();
        }
        else if (key == KeyCode.W)
        {
            if (!isFixed())
            {
                JobManager.schedule("Close " + item.getLabel(), monitor ->
                {
                    boolean shouldClose = item instanceof DockItemWithInput ? ((DockItemWithInput) item).okToClose().get() : true;

                    if (shouldClose) {
                        item.prepareToClose();
                        Platform.runLater(item::close);
                    }
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
        if (getTabs().isEmpty()) {
            Platform.runLater(this::mergeEmptyAnonymousSplit);
        }
        else
        {
            // Update tabs on next UI tick so that findTabHeader() can succeed
            // in case this is in a newly created SplitDock
            Platform.runLater(this::autoHideTabs);
            // Refresh the cached tab-strip boundary here (post-layout) rather than
            // on every DRAG_OVER event, avoiding a scene-graph walk during dragging.
            Platform.runLater(this::refreshTabBarBottom);
        }
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

    private Deque<Consumer<Scene>> functionsDeferredUntilInScene = new LinkedList<>();
    private boolean changeListenerAdded = false;
    /** Need the scene of this dock pane to adjust the style sheet
     *  or to interact with the Window.
     *
     *  We _have_ added this DockPane to a scene graph, so getScene() should
     *  return the scene.
     *  But if this dock pane is nested inside a newly created {@link SplitDock},
     *  it will not have a scene until it is rendered.
     *
     *  If calls to deferUntilInScene() are not nested (i.e., there is no
     *  call of the form deferUntilInScene(f) where f() in turn contains further
     *  calls of the form deferUntilInScene(g) for some g), then the relative
     *  ordering in time of deferred function calls is preserved: if f1() is
     *  deferred before f2() is deferred, then f1() will be invoked before f2()
     *  is invoked.
     *
     *  If, on the other hand, there *is* a call of the form deferUntilInScene(f)
     *  where f() in turn contains a nested call of the form deferUntilInScene(g),
     *  then the invocation of g() that is deferred by the call deferUntilInScene(g)
     *  will occur as part of the (possibly deferred) invocation of f(). I.e.,  it
     *  will *not* be deferred until after all other deferred function invocations
     *  have completed, but will be invoked as part of the (possibly deferred)
     *  invocation of f().
     *
     *  @param function Something that needs to run once there is a scene
     */
    public void deferUntilInScene(Consumer<Scene> function) {
        Scene scene = sceneProperty().get();
        if (scene != null) {
            function.accept(scene);
        }
        else {
            functionsDeferredUntilInScene.addLast(function);

            if (!changeListenerAdded) {
                ChangeListener changeListener = new ChangeListener() {
                    @Override
                    public void changed(ObservableValue observableValue, Object oldValue, Object newValue) {
                        if (newValue != null) {
                            while(!functionsDeferredUntilInScene.isEmpty()) {
                                Consumer<Scene> f = functionsDeferredUntilInScene.removeFirst();
                                f.accept((Scene) newValue);
                            }
                            sceneProperty().removeListener(this);
                            changeListenerAdded = false;
                        }
                    }
                };
                sceneProperty().addListener(changeListener);
                changeListenerAdded = true;
            }
        }
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
    }

    /** @param tabs One or more tabs to add */
    public void addTab(final DockItem... tabs)
    {
        getTabs().addAll(tabs);
        // Select the newly added tab
        getSelectionModel().select(getTabs().size()-1);
        // Set this as the active dock pane
        setActiveDockPane(this);
    }

    /** @return All {@link DockItem}s in this pane (safe copy) */
    public List<DockItem> getDockItems()
    {
        return getTabs().stream()
                        .map(tab -> (DockItem) tab)
                        .collect(Collectors.toList());
    }

    /** Remove the drop-zone highlight, restoring the pane's normal appearance.
     *
     *  <p>Two calls are required because two different mechanisms were used to set
     *  the highlight: split-zone edges use an inline CSS style (highest cascade
     *  priority, overrides stylesheets), while the CENTER green border uses a
     *  programmatic {@link #setBorder} call (lower priority).  Clearing only one
     *  would leave the other visible. */
    private void clearDropZoneStyle()
    {
        setStyle("");
        setBorder(Border.EMPTY);
    }

    /** Accept dock items, tracking the drop zone as the pointer moves.
     *
     *  <p>This is a capture-phase filter: it fires before any child node.
     *  When the pointer is over a DockItem tab-label we accept the transfer mode
     *  (so the drop is not rejected by content nodes below) but do NOT consume,
     *  letting DockItem's own handlers provide the green highlight and handle
     *  the tab-on-tab swap/insert.  For all other positions we consume and
     *  DockPane handles zone borders and the eventual drop.
     */
    private void handleDragOver(final DragEvent event)
    {
        if (!isFixed()  &&  DockItem.dragged_item.get() != null)
        {
            event.acceptTransferModes(TransferMode.MOVE);

            final DropZone zone = getDropZone(event.getX(), event.getY());
            if (zone != active_drop_zone)
            {
                active_drop_zone = zone;
                updateZoneBorder(zone);
            }

            // Let DockItem tab-label handlers fire when the cursor is over
            // the tab header strip; consume everywhere else so child content
            // nodes (TreeView, etc.) cannot silently reject the drop.
            if (event.getY() > tab_bar_bottom)
                event.consume();
        }
    }

    /** Highlight on entry, initialising the zone for the current pointer position */
    private void handleDragEntered(final DragEvent event)
    {
        if (!isFixed()  &&  DockItem.dragged_item.get() != null)
        {
            active_drop_zone = getDropZone(event.getX(), event.getY());
            updateZoneBorder(active_drop_zone);
        }
        event.consume();
    }

    /** Remove highlight and reset zone on exit */
    private void handleDragExited(final DragEvent event)
    {
        active_drop_zone = DropZone.CENTER;
        clearDropZoneStyle();
        event.consume();
    }

    /** Check whether the node under the cursor belongs to a tab header.
     *  Walks up from the pick result looking for a {@code .tab} style-class node.
     *  This is O(scene-graph depth) — typically 3-5 parent nodes — and only called
     *  once per drop, not during the hot DRAG_OVER path.
     */
    private boolean isOverTabNode(final DragEvent event)
    {
        Node n = event.getPickResult().getIntersectedNode();
        while (n != null  &&  n != this)
        {
            if (n.getStyleClass().contains("tab"))
                return true;
            n = n.getParent();
        }
        return false;
    }

    /** Determine drop zone from pointer position relative to this pane.
     *  Drops within the tab header strip always map to CENTER (merge-as-tab).
     *  The outer {@value #SPLIT_ZONE_FRACTION} of each remaining edge is a split zone.
     *  @param x Pointer x in pane-local coordinates
     *  @param y Pointer y in pane-local coordinates
     *  @return {@link DropZone}
     */
    private DropZone getDropZone(final double x, final double y)
    {
        // tab_bar_bottom is refreshed by handleTabChanges(), not here, to avoid
        // a scene-graph walk on every pointer-move event during a drag.
        if (y <= tab_bar_bottom)
            return DropZone.CENTER;

        final double edge_w = getWidth()  * SPLIT_ZONE_FRACTION;
        final double edge_h = getHeight() * SPLIT_ZONE_FRACTION;
        if (x < edge_w)               return DropZone.LEFT;
        if (x > getWidth()  - edge_w) return DropZone.RIGHT;
        if (y < edge_h)               return DropZone.TOP;
        if (y > getHeight() - edge_h) return DropZone.BOTTOM;
        return DropZone.CENTER;
    }

    /** Update the cached tab-strip bottom boundary.
     *  Called after layout changes (tab add/remove), not during drag events.
     *  getBoundsInParent() gives DockPane-local coordinates directly because
     *  findTabHeader() confirms the header's parent is this pane.
     */
    private void refreshTabBarBottom()
    {
        final StackPane header = findTabHeader();
        if (header != null)
            tab_bar_bottom = header.getBoundsInParent().getMaxY();
    }

    /** Highlight the edge where the new pane will appear if the user drops here.
     *  Green full border = merge-as-tab (CENTER zone).
     *  Blue single-edge highlight = split on that edge.
     *
     *  <p>Inline CSS (-fx-border-*) is used because it sits at highest cascade priority
     *  and cannot be overridden by the application's author stylesheet.
     *  @param zone Active {@link DropZone}
     */
    private void updateZoneBorder(final DropZone zone)
    {
        if (zone == DropZone.CENTER)
        {
            // Restore CSS control, then apply the programmatic green border
            setStyle("");
            setBorder(DockItem.DROP_ZONE_BORDER);
            return;
        }
        // Inline style overrides any stylesheet border; clear the programmatic border first
        // so only the inline style is active.
        setBorder(Border.EMPTY);
        setStyle(splitEdgeStyle(zone));
    }

    /** @param zone A split zone (LEFT/RIGHT/TOP/BOTTOM) — never CENTER
     *  @return Inline CSS that draws a 4px blue line on the edge where the new pane will appear
     *  @throws IllegalArgumentException if called with CENTER (caller must guard this)
     */
    private static String splitEdgeStyle(final DropZone zone)
    {
        switch (zone)
        {
            case LEFT:   return "-fx-border-color: transparent transparent transparent dodgerblue; -fx-border-width: 0 0 0 4;";
            case RIGHT:  return "-fx-border-color: transparent dodgerblue transparent transparent; -fx-border-width: 0 4 0 0;";
            case TOP:    return "-fx-border-color: dodgerblue transparent transparent transparent; -fx-border-width: 4 0 0 0;";
            case BOTTOM: return "-fx-border-color: transparent transparent dodgerblue transparent; -fx-border-width: 0 0 4 0;";
            default:     throw new IllegalArgumentException("splitEdgeStyle requires a split zone, got: " + zone);
        }
    }

    /** Accept a dropped tab: merge into this pane (centre zone) or split (edge zone).
     *
     *  <p>Tab-on-tab drops (reorder within same pane, or insert next to a specific
     *  tab from another pane) are handled by {@link DockItem}'s own handlers on
     *  {@code name_tab}.  Those handlers fire after this capture filter because they
     *  are bubble-phase; this filter returns early so the event can reach them.
     *  DockPane handles drops on the pane body, empty tab-bar space, and edge zones.
     */
    private void handleDrop(final DragEvent event)
    {
        if (!event.getDragboard().hasContent(DockItem.DOCK_ITEM))
            return;

        // When the cursor is directly over a tab header, let DockItem's own
        // handler on name_tab manage the swap/insert.  Clean up the pane border
        // since no further DockPane handler will run for this drop.
        if (isOverTabNode(event))
        {
            clearDropZoneStyle();
            active_drop_zone = DropZone.CENTER;
            return;
        }

        final DockItem item = DockItem.dragged_item.getAndSet(null);
        if (item == null)
            return;

        final DropZone zone = getDropZone(event.getX(), event.getY());
        clearDropZoneStyle();
        active_drop_zone = DropZone.CENTER;

        logger.log(Level.INFO, "Dropped " + item + " into " + this + " zone=" + zone);

        if (zone == DropZone.CENTER)
            mergeTabIntoPaneDeferred(item);
        else
        {
            copyStylesFromScene(item);
            splitAndPlaceTabAsync(item, zone);
        }

        event.setDropCompleted(true);
        event.consume();
    }

    /** When a tab moves to a different scene, ensure that scene has the same stylesheets. */
    private void copyStylesFromScene(final DockItem item)
    {
        final TabPane old_parent = item.getTabPane();
        if (!(old_parent instanceof DockPane))
            logger.log(Level.SEVERE, "DockItem is not in DockPane but " + old_parent);
        final Scene old_scene = old_parent.getScene();
        final Scene scene = getScene();
        if (scene != old_scene)
            for (String css : old_scene.getStylesheets())
                Styles.set(scene, css);
    }

    /** Move a dragged tab into this pane (centre-zone drop).
     *
     *  <p>No-ops when the item is already in this pane to avoid a transient empty state
     *  that would trigger a spurious {@code mergeEmptyAnonymousSplit}.
     *  For cross-pane moves, the remove and add happen in the same deferred UI pulse
     *  so the source pane is never empty long enough to trigger a merge.
     */
    private void mergeTabIntoPaneDeferred(final DockItem item)
    {
        final TabPane old_parent = item.getTabPane();
        if (old_parent == this)
            return;  // Tab is already home; nothing to do
        copyStylesFromScene(item);
        Platform.runLater(() ->
        {
            logger.log(Level.INFO, "Adding " + item + " to " + this);
            old_parent.getTabs().remove(item);
            addTab(item);
            Platform.runLater(this::autoHideTabs);
        });
    }

    /** Split this pane in the direction implied by {@code zone} and place the dropped
     *  tab into the newly created pane.
     *
     *  <p>The remove and add are both deferred into the same UI tick so the source
     *  pane is never visibly empty between the two operations.
     */
    private void splitAndPlaceTabAsync(final DockItem item, final DropZone zone)
    {
        final TabPane old_parent = item.getTabPane();
        final boolean horizontally = (zone == DropZone.LEFT  || zone == DropZone.RIGHT);
        final boolean newPaneFirst = (zone == DropZone.LEFT  || zone == DropZone.TOP);

        // split() modifies the scene graph on the UI thread synchronously
        final SplitDock new_split = split(horizontally, newPaneFirst);
        final int new_pane_index  = newPaneFirst ? 0 : 1;
        final DockPane new_pane   = (DockPane) new_split.getItems().get(new_pane_index);

        Platform.runLater(() ->
        {
            logger.log(Level.INFO, "Adding " + item + " to split pane " + new_pane);
            old_parent.getTabs().remove(item);
            new_pane.addTab(item);
            Platform.runLater(new_pane::autoHideTabs);
        });
    }

    /** Split this dock pane.
     *  This pane becomes the first (left/top) item; a new empty pane becomes second (right/bottom).
     *  @param horizontally <code>true</code> for a left/right split, <code>false</code> for top/bottom
     *  @return SplitDock containing this pane and the new empty DockPane
     */
    public SplitDock split(final boolean horizontally)
    {
        return split(horizontally, false);
    }

    /** Split this dock pane.
     *  @param horizontally  <code>true</code> for a left/right split, <code>false</code> for top/bottom
     *  @param newPaneFirst  <code>true</code> to place the new empty pane as the first (left/top) item
     *  @return SplitDock containing this pane and the new empty DockPane
     */
    SplitDock split(final boolean horizontally, final boolean newPaneFirst)
    {
        final DockPane new_pane = new DockPane();
        // The DockPane() constructor calls setActiveDockPane(new_pane), advertising
        // the empty new pane as active and firing activeDockItemChanged(null) to all
        // listeners.  Restore 'this' immediately so listeners never see a null-item
        // state, especially during a live drag.  When the dragged tab lands in new_pane,
        // addTab() will call setActiveDockPane(new_pane) correctly.
        setActiveDockPane(this);
        dockPaneEmptyListeners.stream().forEach(new_pane::addDockPaneEmptyListener);

        final Control first  = newPaneFirst ? new_pane : this;
        final Control second = newPaneFirst ? this     : new_pane;

        final SplitDock split;

        if (dock_parent instanceof SplitDock)
        {
            final SplitDock parent = (SplitDock) dock_parent;
            // Remove this dock pane from parent
            final boolean was_first = parent.removeItem(this);
            split = new SplitDock(parent, horizontally, first, second);
            setDockParent(split);
            new_pane.setDockParent(split);
            parent.addItem(was_first, split);
        }
        else if (dock_parent instanceof BorderPane)
        {
            final BorderPane parent = (BorderPane) dock_parent;
            parent.setCenter(null);
            split = new SplitDock(parent, horizontally, first, second);
            setDockParent(split);
            new_pane.setDockParent(split);
            parent.setCenter(split);
        }
        else if (dock_parent instanceof SplitPane) // ESS-specific Navigator application
        {
            final SplitPane parent = (SplitPane) dock_parent;
            Optional<Double> dividerPosition = parent.getDividerPositions().length > 0
                    ? Optional.of(parent.getDividerPositions()[0])
                    : Optional.empty();
            parent.getItems().remove(this);
            split = new SplitDock(parent, horizontally, first, second);
            setDockParent(split);
            new_pane.setDockParent(split);
            parent.getItems().add(split);
            dividerPosition.ifPresent(pos -> parent.setDividerPosition(0, pos));
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
        // This is called via Platform.runLater.  In the window between scheduling
        // and execution, a tab may have been added back (e.g. during a drag-drop
        // async dance).  Only act when the pane is actually empty.
        if (!getTabs().isEmpty())
            return;

        if (! (dock_parent instanceof SplitDock))
        {
            dockPaneEmptyListeners.forEach(DockPaneEmptyListener::allTabsClosed);
            return;
        }
        if (name.length() > 0)
            return;

        SplitDock splitDock = (SplitDock)dock_parent;
        splitDock.merge();
        dockPaneEmptyListeners.forEach(DockPaneEmptyListener::allTabsClosed);
    }

    @Override
    public String toString()
    {
        return (isFixed() ? "FIXED DockPane " : "DockPane ") +
               Integer.toHexString(System.identityHashCode(this)) + " '" + name + "' "+ getTabs();
    }

    public void addDockPaneEmptyListener(DockPaneEmptyListener listener){
        dockPaneEmptyListeners.add(listener);
    }

    public void removeDockPaneEmptyListener(DockPaneEmptyListener listener){
        dockPaneEmptyListeners.remove(listener);
    }
}
