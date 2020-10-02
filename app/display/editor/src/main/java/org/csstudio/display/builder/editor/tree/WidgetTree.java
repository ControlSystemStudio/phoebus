/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.tree;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.EditorUtil;
import org.csstudio.display.builder.editor.actions.ActionDescription;
import org.csstudio.display.builder.editor.app.CreateGroupAction;
import org.csstudio.display.builder.editor.app.RemoveGroupAction;
import org.csstudio.display.builder.editor.undo.UpdateWidgetOrderAction;
import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.VisibleWidget;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;
import org.phoebus.ui.javafx.TreeHelper;
import org.phoebus.ui.undo.CompoundReversedUndoableAction;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;

/** Tree view of widget hierarchy
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class WidgetTree
{
    /** Is this class updating the selection of tree or model? */
    private final AtomicBoolean active = new AtomicBoolean();

    /** Associated Editor */
    private final DisplayEditor editor;

    private final TreeView<WidgetOrTab> tree_view = new TreeView<>();

    private DisplayModel model = null;

    /** Map model widgets to their tree items in <code>tree_view</code>
     *
     *  <p>When model notifies about changed Widget,
     *  this map provides the corresponding TreeItem.
     */
    private final Map<Widget, TreeItem<WidgetOrTab>> widget2tree = new ConcurrentHashMap<>();

    /** Map of tab's name property to TreeItem */
    private final Map<WidgetProperty<String>, TreeItem<WidgetOrTab>> tab_name2tree = new ConcurrentHashMap<>();

    /** Listener to changes in Widget's children */
    private final WidgetPropertyListener<List<Widget>> children_listener;

    /** Listener to change of Widget's name */
    private final WidgetPropertyListener<String> name_listener = (property, old, new_name) ->
    {
        final Widget widget = property.getWidget();
        logger.log(Level.FINE, "{0} changed name", widget);

        final TreeItem<WidgetOrTab> item = Objects.requireNonNull(widget2tree.get(widget));
        Platform.runLater(() -> TreeHelper.triggerTreeItemRefresh(item));
    };

    /** Listener to changes of Widget's visibility */
    private final WidgetPropertyListener<Boolean> visible_listener = (property, old, new_name) ->
    {
        final Widget widget = property.getWidget();
        logger.log(Level.FINE, "{0} changed visibility", widget);

        final TreeItem<WidgetOrTab> item = Objects.requireNonNull(widget2tree.get(widget));
        Platform.runLater(() -> TreeHelper.triggerTreeItemRefresh(item));
    };

    /** Listener to changes of Widget's embedded_model */
    private final WidgetPropertyListener<DisplayModel> embedded_listener = (property, old, new_name) ->
    {
        final Widget widget = property.getWidget();
        logger.log(Level.FINE, "{0} changed embedded model", widget);

        final TreeItem<WidgetOrTab> item = Objects.requireNonNull(widget2tree.get(widget));
        Platform.runLater(() -> TreeHelper.triggerTreeItemRefresh(item));
    };

    /** Listener to changes in a TabWidget's tabs */
    private final WidgetPropertyListener<List<TabItemProperty>> tabs_property_listener = (tabs, removed, added) ->
    {
        if (added != null)
            addTabs(added);
        if (removed != null)
            removeTabs(removed);
    };

    /** Update the name of a tab item in the tree */
    private final WidgetPropertyListener<String> tab_name_listener = (tab_name, old_name, new_name) ->
    {
        final TreeItem<WidgetOrTab> tab_item = Objects.requireNonNull(tab_name2tree.get(tab_name));
        TreeHelper.triggerTreeItemRefresh(tab_item);
    };

    /** Cell factory that displays {@link WidgetOrTab} info in tree cell */
    private final Callback<TreeView<WidgetOrTab>, TreeCell<WidgetOrTab>> cell_factory;

    /** Construct widget tree
     *  @param selection Handler of selected widgets
     */
    public WidgetTree(final DisplayEditor editor)
    {
        this.editor = editor;
        cell_factory = cell -> 
        {
            WidgetTreeCell tc = new WidgetTreeCell(editor.getUndoableActionManager());
            tc.setOnDragDetected((MouseEvent event) -> dragDetected(event, tc));
            tc.setOnDragOver((DragEvent event) -> dragOver(event, tc));
            tc.setOnDragDropped((DragEvent event) -> drop(event, tc, null));
            tc.setOnDragDone((DragEvent event) -> clearDropLocation());
            tc.setOnDragExited((DragEvent event) -> clearDropLocation());
            tc.setOnDragEntered((DragEvent event) -> decorateDropZone());
            return tc;
        };

        children_listener = (p, removed, added) ->
        {
            // Update must be on UI thread.
            // Even if already on UI thread, decouple.
            if (removed != null && added != null && removed.size() == 1 && removed.equals(added))
            {
                // This was a move up/down of a widget
                // Need to determine the index of moved item in model _now_,
                // not in decoupled thread.
                final Widget widget = removed.get(0);
                final int model_index = p.getValue().indexOf(widget);

                Platform.runLater(() ->
                {
                    active.set(true);
                    try
                    {
                        moveWidget(widget, model_index);
                    }
                    finally
                    {
                        active.set(false);
                    }
                    // Restore tree's selection to match model
                    // after removing/adding items may have changed it.
                    setSelectedWidgets(editor.getWidgetSelectionHandler().getSelection());
                });

                return;
            }

            if (removed != null)
                Platform.runLater(() ->
                {
                    active.set(true);
                    try
                    {
                        for (Widget removed_widget : removed)
                            removeWidget(removed_widget);
                    }
                    finally
                    {
                        active.set(false);
                    }
                });

            if (added != null)
            {   // Need to determine the index of added item in model _now_,
                // not in decoupled thread.
                // Assume model [ a, b, c, d ] that moves a, b, to the end: [ b, c, d, a ], then [ c, d, a, b ]
                // By the time decoupled thread moves a, it will already see the model as [ c, d, a, b ]
                // and thus determine that a needs to be at index 2 -> [ b, c, a, d ]
                // Then it moves b, determined that it needs to be at index 3 -> [ c, a, d, b ]
                final int[] indices = new int[added.size()];
                for (int i=0; i<indices.length; ++i)
                    indices[i] = determineWidgetIndex(added.get(i));

                Platform.runLater(() ->
                {
                    active.set(true);
                    try
                    {
                        for (int i=0; i<indices.length; ++i)
                            addWidget(added.get(i), indices[i]);
                    }
                    finally
                    {
                        active.set(false);
                    }
                    // Restore tree's selection to match model
                    // after removing/adding items may have changed it.
                    setSelectedWidgets(editor.getWidgetSelectionHandler().getSelection());
                });
            }
        };
    }

    /** Create UI components
     *  @return Root {@link Control}
     */
    public Control create()
    {
        tree_view.setShowRoot(false);
        tree_view.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tree_view.setCellFactory(cell_factory);
        tree_view.setEditable(true);

        bindSelections();
        tree_view.setOnKeyPressed(this::handleKeyPress);

        return tree_view;
    }

    private void handleKeyPress(final KeyEvent event)
    {
        WidgetTree.handleGroupOrOrderKeys(event, editor);
    }

    /** Handle keys to group or change widget hierarchy
     *
     *  @param event {@link KeyEvent}
     *  @param editor {@link DisplayEditor}
     *  @return <code>true</code> if key was handled
     */
    public static boolean handleGroupOrOrderKeys(final KeyEvent event, final DisplayEditor editor)
    {
        if (event.isShortcutDown())
        {
            switch (event.getCode())
            {
            case G:
            {
                event.consume();
                final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
                if (widgets.size() > 0)
                    new CreateGroupAction(editor, widgets).run();
                return true;
            }
            case U:
            {
                event.consume();
                final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
                if (widgets.size() == 1  &&  widgets.get(0) instanceof GroupWidget)
                    new RemoveGroupAction(editor, (GroupWidget)widgets.get(0)).run();
                return true;
            }
            default:
                break;
            }
        }
        else if (event.isAltDown())
        {
            switch (event.getCode())
            {
            case B:
                event.consume();
                if (event.isShiftDown())
                    ActionDescription.TO_BACK.run(editor);
                else
                    ActionDescription.MOVE_UP.run(editor);
                return true;
            case F:
                event.consume();
                if (event.isShiftDown())
                    ActionDescription.TO_FRONT.run(editor);
                else
                    ActionDescription.MOVE_DOWN.run(editor);
                return true;
            default:
                break;
            }
        }
        return false;
    }

    /** Link selections in tree view and model */
    private void bindSelections()
    {
        // Update selected widgets in model from selection in tree_view
        final ObservableList<TreeItem<WidgetOrTab>> tree_selection = tree_view.getSelectionModel().getSelectedItems();
        InvalidationListener listener = (Observable observable) ->
        {
            if (! active.compareAndSet(false, true))
                return;
            try
            {
                final List<Widget> widgets = new ArrayList<>(tree_selection.size());

                for (TreeItem<WidgetOrTab> item : tree_selection)
                {
                    final WidgetOrTab wot = item.getValue();
                    final Widget widget = wot.isWidget()
                        ? wot.getWidget()
                        : wot.getTab().getWidget();
                    if (! widgets.contains(widget))
                        widgets.add(widget);
                };
                logger.log(Level.FINE, "Selected in tree: {0}", widgets);
                editor.getWidgetSelectionHandler().setSelection(widgets);
            }
            finally
            {
                active.set(false);
            }
        };
        tree_selection.addListener(listener);

        // Update selection in tree_view from selected widgets in model
        editor.getWidgetSelectionHandler().addListener(this::setSelectedWidgets);
    }

    /** @param model Model to display as widget tree */
    public void setModel(final DisplayModel model)
    {
        // Could recursively remove all old model tree elements,
        // on UI thread, one by one.
        // Faster: Unlink listeners and then replace the whole
        // tree model which was created in background.
        final DisplayModel old_model = this.model;
        if (old_model != null)
        {
            old_model.runtimeChildren().removePropertyListener(children_listener);
            for (Widget widget : old_model.runtimeChildren().getValue())
                removeWidgetListeners(widget);
            widget2tree.clear();
            tab_name2tree.clear();
        }
        this.model = model;

        // Might be called on UI thread, move off
        EditorUtil.getExecutor().execute(() ->
        {
            final TreeItem<WidgetOrTab> root = new TreeItem<>(WidgetOrTab.of(model));
            if (model != null)
            {
                widget2tree.put(model, root);
                for (Widget widget : model.runtimeChildren().getValue())
                    addWidget(widget, -1);
                root.setExpanded(true);
                model.runtimeChildren().addPropertyListener(children_listener);
            }
            logger.log(Level.FINE, "Computed new tree on {0}, updating UI", Thread.currentThread().getName());
            Platform.runLater(() ->
            {
                tree_view.setRoot(root);
                setSelectedWidgets(editor.getWidgetSelectionHandler().getSelection());
            });
        });
    }

    /** Called by selection handler when selected widgets have changed, or on new model
     *  @param widgets Widgets to select in tree
     */
    public void setSelectedWidgets(final List<Widget> widgets)
    {
        if (! active.compareAndSet(false, true))
            return;
        try
        {
            final MultipleSelectionModel<TreeItem<WidgetOrTab>> selection = tree_view.getSelectionModel();
            selection.clearSelection();
            for (Widget widget : widgets)
                selection.select(widget2tree.get(widget));

            // If something's selected, show it.
            // Otherwise leave tree at current position.
            final int index = selection.getSelectedIndex();
            if (index >= 0)
                tree_view.scrollTo(index);
        }
        finally
        {
            active.set(false);
        }
    }

    /** Determine location of widget within parent of model
     *  @param widget Widget
     *  @return Index of widget in model's parent
     */
    private int determineWidgetIndex(final Widget widget)
    {
        final Widget widget_parent = Objects.requireNonNull(widget.getParent().get());
        if (widget_parent instanceof TabsWidget)
        {
            for (TabItemProperty tab : ((TabsWidget)widget_parent).propTabs().getValue())
            {
                int index = tab.children().getValue().indexOf(widget);
                if (index >= 0)
                    return index;
            }
        }
        else
            return ChildrenProperty.getChildren(widget_parent).getValue().indexOf(widget);
        return -1;
    }

    /** Moves a widget to match its position in the model
     *  @param widget The widget that was moved in the model
     *  @param model_index Index of widget in model's parent
     */
    private void moveWidget(final Widget widget, final int model_index)
    {
        final TreeItem<WidgetOrTab> item = widget2tree.get(widget);
        final Widget widget_parent = Objects.requireNonNull(widget.getParent().get());
        List children = null;
        int current_index = -1;

        if (widget_parent instanceof TabsWidget)
        {
            for (TreeItem<WidgetOrTab> cit : widget2tree.get(widget_parent).getChildren())
            {
                WidgetOrTab wot = cit.getValue();
                if (wot.isWidget())
                    continue;

                if ((current_index = cit.getChildren().indexOf(item)) != -1)
                {
                    children = cit.getChildren();
                    break;
                }
            }
        }
        else
        {
            children = widget2tree.get(widget_parent).getChildren();
            current_index = children.indexOf(item);
        }

        if (current_index == -1)
        {
            logger.log(Level.SEVERE, "Couldn't find widget to move in Widget Tree: " + widget);
            return;
        }

        final int index_diff = model_index - current_index;
        if (index_diff == 1 || index_diff == -1)
        {
            // Move up/down by 1 step
            Collections.swap(children, current_index, model_index);
        }
        else
        {
            // Move to front/back
            Collections.rotate(children.subList(java.lang.Math.min(model_index, current_index), java.lang.Math.max(model_index, current_index) + 1), index_diff);
        }
    }

    /** Add widget to existing model & tree
     *  @param added_widget Widget to add
     *  @param index Index of widget within parent. -1 to add at end
     */
    private void addWidget(final Widget added_widget, final int index)
    {
        // Have widget and its parent in model
        final Widget widget_parent = added_widget.getParent().get();
        // Determine parent tree item
        TreeItem<WidgetOrTab> item_parent = null;
        if (widget_parent instanceof TabsWidget)
        {
            for (TabItemProperty tab : ((TabsWidget)widget_parent).propTabs().getValue())
                if (tab.children().getValue().contains(added_widget))
                {
                    item_parent = tab_name2tree.get(tab.name());
                    break;
                }
        }
        else
            item_parent = widget2tree.get(widget_parent);

        Objects.requireNonNull(item_parent, "Cannot obtain parent item for " + added_widget);

        // Create Tree item
        final TreeItem<WidgetOrTab> item = new TreeItem<>(WidgetOrTab.of(added_widget));
        widget2tree.put(added_widget, item);
        item.setExpanded(true);
        if (index >= 0)
            // Add at same index into Tree
            item_parent.getChildren().add(index, item);
        else// Append to end
            item_parent.getChildren().add(item);

        added_widget.propName().addPropertyListener(name_listener);
        if (added_widget instanceof VisibleWidget)
            ((VisibleWidget)added_widget).propVisible().addPropertyListener(visible_listener);
        Optional<WidgetProperty<DisplayModel>> embedded = added_widget.checkProperty(EmbeddedDisplayWidget.runtimeModel.getName());
        if (embedded.isPresent())
            embedded.get().addPropertyListener(embedded_listener);

        if (added_widget instanceof TabsWidget)
        {
            final ArrayWidgetProperty<TabItemProperty> tabs = ((TabsWidget)added_widget).propTabs();
            addTabs(tabs.getValue());
            tabs.addPropertyListener(tabs_property_listener);
        }
        else
        {
            final ChildrenProperty children = ChildrenProperty.getChildren(added_widget);
            if (children != null)
            {
                children.addPropertyListener(children_listener);
                for (Widget child : children.getValue())
                    addWidget(child, -1);
            }
        }
    }

    private void addTabs(final List<TabItemProperty> added)
    {
        for (TabItemProperty tab : added)
        {
            final TreeItem<WidgetOrTab> widget_item = widget2tree.get(tab.getWidget());
            final TreeItem<WidgetOrTab> tab_item = new TreeItem<>(WidgetOrTab.of(tab));
            widget_item.getChildren().add(tab_item);
            tab_name2tree.put(tab.name(), tab_item);
            tab.name().addPropertyListener(tab_name_listener);

            for (Widget child : tab.children().getValue())
                addWidget(child, -1);

            tab.children().addPropertyListener(children_listener);
        }
    }

    private void removeTabs(final List<TabItemProperty> removed)
    {
        for (TabItemProperty tab : removed)
        {
            tab.children().removePropertyListener(children_listener);

            tab.name().removePropertyListener(tab_name_listener);
            final TreeItem<WidgetOrTab> tab_item = tab_name2tree.remove(tab.name());
            tab_item.getParent().getChildren().remove(tab_item);
        }
    }

    /** Remove widget from existing model & tree
     *  @param removed_widget
     */
    private void removeWidget(final Widget removed_widget)
    {
        if (removed_widget instanceof TabsWidget)
        {
            final ArrayWidgetProperty<TabItemProperty> tabs = ((TabsWidget)removed_widget).propTabs();
            tabs.removePropertyListener(tabs_property_listener);
            removeTabs(tabs.getValue());
        }

        removed_widget.propName().removePropertyListener(name_listener);
        if (removed_widget instanceof VisibleWidget)
            ((VisibleWidget)removed_widget).propVisible().removePropertyListener(visible_listener);
        Optional<WidgetProperty<DisplayModel>> embedded = removed_widget.checkProperty(EmbeddedDisplayWidget.runtimeModel.getName());
        if (embedded.isPresent())
            embedded.get().removePropertyListener(embedded_listener);

        final ChildrenProperty children = ChildrenProperty.getChildren(removed_widget);
        if (children != null)
        {
            children.removePropertyListener(children_listener);
            for (Widget child : children.getValue())
                removeWidget(child);
        }

        final TreeItem<WidgetOrTab> item = widget2tree.remove(removed_widget);
        item.getParent().getChildren().remove(item);
    }

    /** Recursively remove model widget listeners
     *  @param container Widgets to unlink
     */
    private void removeWidgetListeners(final Widget widget)
    {
        if (widget instanceof TabsWidget)
        {
            final ArrayWidgetProperty<TabItemProperty> tabs = ((TabsWidget)widget).propTabs();
            tabs.removePropertyListener(tabs_property_listener);
            for (TabItemProperty tab : tabs.getValue())
            {
                tab.children().removePropertyListener(children_listener);
                tab.name().removePropertyListener(tab_name_listener);
            }
        }

        widget.propName().removePropertyListener(name_listener);
        if (widget instanceof VisibleWidget)
            ((VisibleWidget)widget).propVisible().removePropertyListener(visible_listener);
        final ChildrenProperty children = ChildrenProperty.getChildren(widget);
        if (children != null)
        {
            children.removePropertyListener(children_listener);
            for (Widget child : children.getValue())
                removeWidgetListeners(child);
        }
    }

    /** @return Is the tree currently editing a widget name? */
    public boolean isInlineEditorActive()
    {
        return tree_view.editingItemProperty().get() != null;
    }

    /**
     * Expand all levels of the view
     */
    public void expandAllTreeItems()
    {
        TreeHelper.setExpandedEx(tree_view, true);
    }

    /**
     * Collapse all levels of the view
     */
    public void collapseAllTreeItems()
    {
        TreeHelper.setExpandedEx(tree_view, false);
    }

    // DragDrop Implementation
    private static final String TREE_ID = "text/DisplayBuiderEditor-TreeView-Id";
    private static final DataFormat TREE_FORMAT = new DataFormat(TREE_ID);
    private static final String DROP_HINT_STYLE = "-fx-border-color: #eea82f; -fx-border-width: 0 0 2 0; -fx-padding: 3 3 1 3";
    private TreeItem<WidgetOrTab> draggedItem;
    private TreeCell<WidgetOrTab> dropZone;

    /**Code executed when a drag from a WidgetTreeCell is detected 
     * 
     * @param event - source event
     * @param treeCell - dragged cell
     */
    private void dragDetected(MouseEvent event, WidgetTreeCell treeCell) {
        draggedItem = treeCell.getTreeItem();

        // root can't be dragged
        if (draggedItem.getParent() == null) return;
        Dragboard db = treeCell.startDragAndDrop(TransferMode.MOVE);

        ClipboardContent content = new ClipboardContent();
        content.put(TREE_FORMAT, TREE_ID);
        db.setContent(content);
        db.setDragView(treeCell.snapshot(null, null));
        event.consume();
    }

    /**Code executed when a drag over a WidgetTreeCell is detected 
     * 
     * @param event - source event
     * @param treeCell - possibly destination cell
     */
    private void dragOver(DragEvent event, WidgetTreeCell treeCell) {
        if (!event.getDragboard().hasContent(TREE_FORMAT)) return;
        if (!event.getDragboard().getContent(TREE_FORMAT).equals(TREE_ID)) return;

        TreeItem<WidgetOrTab> thisItem = treeCell.getTreeItem();

        // can't drop on itself
        if (draggedItem == null || thisItem == null || thisItem == draggedItem) return;
        // ignore if not common Parent and not Parent itself 
        if (!Objects.equals(draggedItem.getParent(), thisItem.getParent()) && !Objects.equals(draggedItem.getParent(), thisItem)) {
            clearDropLocation();
            return;
        }

        // Not supported the case of multiple widgets selected: too much effort for edge cases
        // (drag of discontinuous selection sets dropped inside selection etc.)
        if (editor.getWidgetSelectionHandler().getSelection().size() > 1) {
            clearDropLocation();
            return;
        }

        event.acceptTransferModes(TransferMode.MOVE);
        if (!Objects.equals(dropZone, treeCell)) {
            clearDropLocation();
            this.dropZone = treeCell;
            decorateDropZone();
        }
    }

    /**Paint a decoration under drop target cell
     * 
     */
    private void decorateDropZone()
    {
        if (dropZone != null && editor.getWidgetSelectionHandler().getSelection().size() <= 1)
            dropZone.setStyle(DROP_HINT_STYLE);
    }

    /**Clear a decoration under drop target cell
     * 
     */
    private void clearDropLocation() {
        if (dropZone != null) dropZone.setStyle("");
    }

    /**Code executed when a drop to a WidgetTreeCell is detected
     * Actual implementation uses selection from editor and relies on changes in model updating the tree
     * @param event - source event
     * @param treeCell - possibly destination cell, if null, alternItem is used
     * @param alternItem - alternatively to treeCell use this TreeItem
     */
    private void drop(DragEvent event, WidgetTreeCell treeCell, TreeItem<WidgetOrTab> alternItem) {
        boolean success = false;

        if (!event.getDragboard().hasContent(TREE_FORMAT)) return;
        if (!event.getDragboard().getContent(TREE_FORMAT).equals(TREE_ID)) return;
        final TreeItem<WidgetOrTab> thisItem;
        if (treeCell == null)
            thisItem = alternItem;
        else
            thisItem = treeCell.getTreeItem();
        if (draggedItem == null || thisItem == null) return;

        final TreeItem<WidgetOrTab> droppedItemParent = draggedItem.getParent();

        // dropping on parent node makes it the first child
        if (Objects.equals(droppedItemParent, thisItem)) {
            reorder(editor, 0, droppedItemParent.getChildren().size());
        }
        else {
            int frIndex = draggedItem.getParent().getChildren().indexOf(draggedItem);
            int toIndex = thisItem.getParent().getChildren().indexOf(thisItem);
            reorder(editor, toIndex + (frIndex > toIndex ? 1 : 0), droppedItemParent.getChildren().size());
        }
        // All actual tree changes will be done as a reaction to the reorder action
        event.setDropCompleted(success);
        event.consume();
    }

    /**Reordering widgets in model, as UndoableAction
     * 
     * @param editor - needed to get the getUndoableActionManager
     * @param toIndex - target index for widgets
     */
    public void reorder(final DisplayEditor editor, int toIndex, int listSize)
    {
        List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
        if (widgets.isEmpty())
            return;
        // Not supported the case of multiple widgets selected: too much effort for edge cases
        // (drag of discontinuous selection sets dropped inside selection etc.)
        if (widgets.size() > 1)
            return;

        // Using compound action to be prepared for possible extension to multiple selection case
        final CompoundReversedUndoableAction compound = new CompoundReversedUndoableAction("TreeView Reorder");
        compound.add(new UpdateWidgetOrderAction(widgets.get(0), toIndex));
        editor.getUndoableActionManager().execute(compound);
    }

    /**Configuring external control for drop
     * To resolve an issue when it is not possible to drop to treeWiew root when it is not shown,
     * enable drop to the pane header.
     * The header control (actually a Label) must be configured for that, see EditorGUI
     */
    public void configureHeaderDnD(Control header)
    {
        header.setOnDragDropped((DragEvent event) -> drop(event, null, tree_view.getRoot()));
        header.setOnDragOver((DragEvent event) -> 
        {
            if (!event.getDragboard().hasContent(TREE_FORMAT)) return;
            if (!event.getDragboard().getContent(TREE_FORMAT).equals(TREE_ID)) return;

            // ignore if Parent is not Root 
            if (draggedItem == null || !Objects.equals(draggedItem.getParent(), tree_view.getRoot())) return;

            event.acceptTransferModes(TransferMode.MOVE);
        });
    }
}
