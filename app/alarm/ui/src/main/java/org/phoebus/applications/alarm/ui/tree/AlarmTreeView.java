/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.ui.AlarmContextMenuHelper;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.applications.email.actions.SendEmailAction;
import org.phoebus.logbook.ui.menu.SendLogbookAction;
import org.phoebus.ui.application.SaveSnapshotAction;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PrintAction;
import org.phoebus.ui.javafx.Screenshot;
import org.phoebus.ui.javafx.ToolbarHelper;
import org.phoebus.ui.javafx.TreeHelper;
import org.phoebus.ui.javafx.UpdateThrottle;
import org.phoebus.util.text.CompareNatural;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

/** Tree-based UI for alarm configuration
 *
 *  <p>Implemented as {@link BorderPane}, but should be treated
 *  as generic JavaFX Node, only calling public methods
 *  defined on this class.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTreeView extends BorderPane implements AlarmClientListener
{
    private final Label no_server = AlarmUI.createNoServerLabel();
    private final TreeView<AlarmTreeItem<?>> tree_view = new TreeView<>();

    private final AlarmClient model;

    /** Map from alarm tree path to view's TreeItem */
    private final ConcurrentHashMap<String, TreeItem<AlarmTreeItem<?>>> path2view = new ConcurrentHashMap<>();

    /** Items to update, ordered by time of original update request
     *
     *  SYNC on access
     */
    private final Set<TreeItem<AlarmTreeItem<?>>> items_to_update = new LinkedHashSet<>();

    /** Throttle [5Hz] used for updates of existing items */
    private final UpdateThrottle throttle = new UpdateThrottle(200, TimeUnit.MILLISECONDS, this::performUpdates);

    /** Cursor change doesn't work on Mac, so add indicator to toolbar */
    private final Label changing = new Label("Loading...");

    /** Is change indicator shown, and future been submitted to clear it? */
    private final AtomicReference<ScheduledFuture<?>> ongoing_change = new AtomicReference<>();

    /** Clear the change indicator */
    private final Runnable clear_change_indicator = () ->
        Platform.runLater(() ->
        {
            logger.log(Level.INFO, "Alarm tree changes end");
            ongoing_change.set(null);
            setCursor(null);
            final ObservableList<Node> items = getToolbar().getItems();
            items.remove(changing);
        });

    // Javadoc for TreeItem shows example for overriding isLeaf() and getChildren()
    // to dynamically create TreeItem as TreeView requests information.
    //
    // The alarm tree, however, keeps changing, and needs to locate the TreeItem
    // for the changed AlarmTreeItem.
    // Added code for checking if a TreeItem has been created, yet,
    // can only make things slower,
    // and the overall performance should not degrade when user opens more and more
    // sections of the overall tree.
    // --> Create the complete TreeItems ASAP and then keep updating to get
    //     constant performance?

    /** @param model Model to represent. Must <u>not</u> be running, yet */
    public AlarmTreeView(final AlarmClient model)
    {
        if (model.isRunning())
            throw new IllegalStateException();

        changing.setTextFill(Color.WHITE);
        changing.setBackground(new Background(new BackgroundFill(Color.BLUE, CornerRadii.EMPTY, Insets.EMPTY)));

        this.model = model;

        tree_view.setShowRoot(false);
        tree_view.setCellFactory(view -> new AlarmTreeViewCell());
        tree_view.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        setTop(createToolbar());
        setCenter(tree_view);

        tree_view.setRoot(createViewItem(model.getRoot()));

        model.addListener(this);

        createContextMenu();
        addClickSupport();
        addDragSupport();
    }

    private ToolBar createToolbar()
    {
        final Button collapse = new Button("",
                ImageCache.getImageView(AlarmUI.class, "/icons/collapse.png"));
        collapse.setTooltip(new Tooltip("Collapse alarm tree"));
        collapse.setOnAction(event ->
        {
            for (TreeItem<AlarmTreeItem<?>> sub : tree_view.getRoot().getChildren())
                sub.setExpanded(false);
        });

        final Button show_alarms = new Button("",
                ImageCache.getImageView(AlarmUI.class, "/icons/expand_alarms.png"));
        show_alarms.setTooltip(new Tooltip("Expand alarm tree to show active alarms"));
        show_alarms.setOnAction(event -> expandAlarms(tree_view.getRoot()));
        return new ToolBar(no_server, ToolbarHelper.createSpring(), collapse, show_alarms);
    }

    ToolBar getToolbar()
    {
        return (ToolBar) getTop();
    }

    private void expandAlarms(final TreeItem<AlarmTreeItem<?>> node)
    {
        if (node.isLeaf())
            return;

        // Always expand the root, which itself is not visible,
        // but this will show all the top-level elements.
        // In addition, expand those items which are in active alarm.
        final boolean expand = node.getValue().getState().severity.isActive() ||
                               node == tree_view.getRoot();
        node.setExpanded(expand);
        for (TreeItem<AlarmTreeItem<?>> sub : node.getChildren())
            expandAlarms(sub);
    }

    private TreeItem<AlarmTreeItem<?>> createViewItem(final AlarmTreeItem<?> model_item)
    {
        // Create view item for model item itself
        final TreeItem<AlarmTreeItem<?>> view_item = new TreeItem<>(model_item);
        final TreeItem<AlarmTreeItem<?>> previous = path2view.put(model_item.getPathName(), view_item);
        if (previous != null)
            throw new IllegalStateException("Found existing view item for " + model_item.getPathName());

        // Create view items for model item's children
        for (final AlarmTreeItem<?> model_child : model_item.getChildren())
            view_item.getChildren().add(createViewItem(model_child));

        return view_item;
    }

    /** Called when an item is added/removed to tell user
     *  that there are changes to the tree structure,
     *  may not make sense to interact with the tree right now.
     *
     *  <p>Resets on its own after 1 second without changes.
     */
    private void indicateChange()
    {
        final ScheduledFuture<?> previous = ongoing_change.getAndSet(UpdateThrottle.TIMER.schedule(clear_change_indicator, 1, TimeUnit.SECONDS));
        if (previous == null)
        {
            logger.log(Level.INFO, "Alarm tree changes start");
            setCursor(Cursor.WAIT);
            final ObservableList<Node> items = getToolbar().getItems();
            items.add(1, changing);
        }
        else
            previous.cancel(false);
    }

    // AlarmClientModelListener
    @Override
    public void serverStateChanged(final boolean alive)
    {
        Platform.runLater(() ->
        {
            final ObservableList<Node> items = getToolbar().getItems();
            items.remove(no_server);
            if (! alive)
                // Place left of spring, collapse, expand_alarms,
                // i.e. right of potential AlarmConfigSelector
                items.add(items.size()-3, no_server);
        });
    }

    // AlarmClientModelListener
    @Override
    public void serverModeChanged(final boolean maintenance_mode)
    {
        // NOP
    }

    // AlarmClientModelListener
    @Override
    public void serverDisableNotifyChanged(final boolean disable_notify)
    {
        // NOP
    }

    // AlarmClientModelListener
    @Override
    public void itemAdded(final AlarmTreeItem<?> item)
    {
        // System.out.println("Add " + item.getPathName());

        // Parent must already exist
        final AlarmTreeItem<BasicState> model_parent = item.getParent();
        final TreeItem<AlarmTreeItem<?>> view_parent = path2view.get(model_parent.getPathName());

        if (view_parent == null)
        {
        	dumpTree(tree_view.getRoot());
            throw new IllegalStateException("Missing parent view item for " + item.getPathName());
        }
        // Create view item ASAP so that following updates will find it..
        final TreeItem<AlarmTreeItem<?>> view_item = createViewItem(item);

        // .. but defer showing it on screen to UI thread
        final CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() ->
        {
            indicateChange();
            // Keep sorted by inserting at appropriate index
            final List<TreeItem<AlarmTreeItem<?>>> items = view_parent.getChildren();
            final int index = Collections.binarySearch(items, view_item,
                                                       (a, b) -> CompareNatural.compareTo(a.getValue().getName(),
                                                                                          b.getValue().getName()));
            if (index < 0)
                items.add(-index-1, view_item);
            else
                items.add(index, view_item);
            done.countDown();
        });
        updateStats();

        // Waiting on the UI thread throttles the model's updates
        // to a rate that the UI can handle.
        // The result is a slower startup when loading the model,
        // but keeping the UI responsive
        try
        {
            done.await();
        }
        catch (final InterruptedException ex)
        {
            logger.log(Level.WARNING, "Alarm tree update error for added item " + item.getPathName(), ex);
        }
    }

    // AlarmClientModelListener
    @Override
    public void itemRemoved(final AlarmTreeItem<?> item)
    {
        // System.out.println("Removed " + item.getPathName());

        // Remove item and all sub-items from model2ui
        final TreeItem<AlarmTreeItem<?>> view_item = removeViewItems(item);
        if (view_item == null)
            throw new IllegalStateException("No view item for " + item.getPathName());

        // Remove the corresponding view
        final CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() ->
        {
            indicateChange();
            // Can only locate the parent view item on UI thread,
            // because item might just have been created by itemAdded() event
            // and won't be on the screen until UI thread runs.
            final TreeItem<AlarmTreeItem<?>> view_parent = view_item.getParent();
            if (view_parent == null)
                throw new IllegalStateException("No parent in view for " + item.getPathName());
            view_parent.getChildren().remove(view_item);
            done.countDown();
        });
        updateStats();

        // Waiting on the UI thread throttles the model's updates
        // to a rate that the UI can handle.
        // The result is a slower startup when loading the model,
        // but keeping the UI responsive
        try
        {
            done.await();
        }
        catch (final InterruptedException ex)
        {
            logger.log(Level.WARNING, "Alarm tree update error for removed item " + item.getPathName(), ex);
        }
    }

    /** @param item Item for which the TreeItem should be removed from path2view. Recurses to all child entries.
     *  @return TreeItem for 'item'
     */
    private TreeItem<AlarmTreeItem<?>> removeViewItems(final AlarmTreeItem<?> item)
    {
        final TreeItem<AlarmTreeItem<?>> view_item = path2view.remove(item.getPathName());

        for (final AlarmTreeItem<?> child : item.getChildren())
            removeViewItems(child);

        return view_item;
    }

    // AlarmClientModelListener
    @Override
    public void itemUpdated(final AlarmTreeItem<?> item)
    {
        // System.out.println("Updated " + item.getPathName());
        final TreeItem<AlarmTreeItem<?>> view_item = path2view.get(item.getPathName());
        if (view_item == null)
        {
            System.out.println("Unknown view for " + item.getPathName());
            path2view.keySet().stream().forEach(System.out::println);
            throw new IllegalStateException("No view item for " + item.getPathName());
        }

        // UI update of existing item, i.e.
        //  Platform.runLater(() -> TreeHelper.triggerTreeItemRefresh(view_item));
        // is throttled.
        // If several items update, they're all redrawn in one Platform call,
        // and rapid updates of the same item are merged into just one final update
        synchronized (items_to_update)
        {
            items_to_update.add(view_item);
        }
        throttle.trigger();
        updateStats();
    }

    /** Called by throttle to perform accumulated updates */
    private void performUpdates()
    {
        final TreeItem<?>[] view_items;
        synchronized (items_to_update)
        {
            // Creating a direct copy, i.e. another new LinkedHashSet<>(items_to_update),
            // would be expensive, since we only need a _list_ of what's to update.
            // Could use type-safe
            //    new ArrayList<TreeItem<AlarmTreeItem<?>>>(items_to_update)
            // but that calls toArray() internally, so doing that directly
            view_items = items_to_update.toArray(new TreeItem[items_to_update.size()]);
            items_to_update.clear();
        }

        for (final TreeItem<?> view_item : view_items)
            TreeHelper.triggerTreeItemRefresh(view_item);
    }

    /** Context menu, details depend on selected items */
    private void createContextMenu()
    {
        final ContextMenu menu = new ContextMenu();

        tree_view.setOnContextMenuRequested(event ->
        {
            final ObservableList<MenuItem> menu_items = menu.getItems();
            menu_items.clear();

            final List<AlarmTreeItem<?>> selection = tree_view.getSelectionModel().getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.toList());

            // Add guidance etc.
            new AlarmContextMenuHelper().addSupportedEntries(tree_view, model, menu, selection);
            if (menu_items.size() > 0)
                menu_items.add(new SeparatorMenuItem());

            if (AlarmUI.mayConfigure(model))
            {
                if (selection.size() <= 0)
                    // Add first item to empty config
                    menu_items.add(new AddComponentAction(tree_view, model, model.getRoot()));
                else if (selection.size() == 1)
                {
                    final AlarmTreeItem<?> item = selection.get(0);
                    menu_items.add(new ConfigureComponentAction(tree_view, model, item));
                    menu_items.add(new SeparatorMenuItem());

                    if (item instanceof AlarmClientNode)
                        menu_items.add(new AddComponentAction(tree_view, model, item));

                    menu_items.add(new RenameTreeItemAction(tree_view, model, item));

                    if (item instanceof AlarmClientLeaf)
                        menu_items.add(new DuplicatePVAction(tree_view, model, (AlarmClientLeaf) item));

                    menu_items.add(new MoveTreeItemAction(tree_view, model, item));
                }
                if (selection.size() >= 1)
                {
                    menu_items.add(new EnableComponentAction(tree_view, model, selection));
                    menu_items.add(new DisableComponentAction(tree_view, model, selection));
                    menu_items.add(new RemoveComponentAction(tree_view, model, selection));
                }
            }

            menu_items.add(new SeparatorMenuItem());
            menu_items.add(new PrintAction(tree_view));
            menu_items.add(new SaveSnapshotAction(DockPane.getActiveDockPane()));
            menu_items.add(new SendEmailAction(tree_view, "Alarm Screenshot", "See alarm tree screenshot", () -> Screenshot.imageFromNode(tree_view)));
            menu_items.add(new SendLogbookAction(tree_view, "Alarm Screenshot", "See alarm tree screenshot", () -> Screenshot.imageFromNode(tree_view)));
            menu.show(tree_view.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    /** Double-click on item opens configuration dialog */
    private void addClickSupport()
    {
        tree_view.setOnMouseClicked(event ->
        {
            if (!AlarmUI.mayConfigure(model)       ||
                event.getClickCount() != 2    ||
                tree_view.getSelectionModel().getSelectedItems().size() != 1)
                return;

            final AlarmTreeItem<?> item = tree_view.getSelectionModel().getSelectedItems().get(0).getValue();
            final ItemConfigDialog dialog = new ItemConfigDialog(model, item);
            DialogHelper.positionDialog(dialog, tree_view, -250, -400);
            // Show dialog, not waiting for it to close with OK or Cancel
            dialog.show();
        });
    }

    /** For leaf nodes, drag PV name */
    private void addDragSupport()
    {
        tree_view.setOnDragDetected(event ->
        {
            final ObservableList<TreeItem<AlarmTreeItem<?>>> items = tree_view.getSelectionModel().getSelectedItems();
            if (items.size() != 1)
                return;
            final AlarmTreeItem<?> item = items.get(0).getValue();
            if (! (item instanceof AlarmClientLeaf))
                return;
            final Dragboard db = tree_view.startDragAndDrop(TransferMode.COPY);
            final ClipboardContent content = new ClipboardContent();
            content.putString(item.getName());
            db.setContent(content);
            event.consume();
        });
    }

//    private long next_stats = 0;
//    private final AtomicInteger update_count = new AtomicInteger();
//    private volatile double updates_per_sec = 0.0;

    private void updateStats()
    {
//        final long time = System.currentTimeMillis();
//        if (time > next_stats)
//        {
//            final int updates = update_count.getAndSet(0);
//            updates_per_sec = updates_per_sec * 0.9 + updates * 0.1;
//            next_stats = time + 1000;
//            System.out.format("%.2f updates/sec\n", updates_per_sec);
//        }
//        else
//            update_count.incrementAndGet();
    }

    private void dumpTree(TreeItem<AlarmTreeItem<?>> item)
    {
    	final ObservableList<TreeItem<AlarmTreeItem<?>>> children = item.getChildren();
    	System.out.printf("item: %s , has %d children.\n", item.getValue().getName(), children.size());
    	for (final TreeItem<AlarmTreeItem<?>> child : children)
    	{
    		System.out.println(child.getValue().getName());
    		dumpTree(child);
    	}
    }
}
