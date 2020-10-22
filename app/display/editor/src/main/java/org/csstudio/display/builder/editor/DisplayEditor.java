/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import org.csstudio.display.builder.editor.actions.ActionDescription;
import org.csstudio.display.builder.editor.app.DisplayEditorInstance;
import org.csstudio.display.builder.editor.palette.Palette;
import org.csstudio.display.builder.editor.poly.PointsBinding;
import org.csstudio.display.builder.editor.tracker.SelectedWidgetUITracker;
import org.csstudio.display.builder.editor.tree.WidgetTree;
import org.csstudio.display.builder.editor.undo.AddWidgetAction;
import org.csstudio.display.builder.editor.undo.RemoveWidgetsAction;
import org.csstudio.display.builder.editor.util.AutoScrollHandler;
import org.csstudio.display.builder.editor.util.GeometryTools;
import org.csstudio.display.builder.editor.util.JFXGeometryTools;
import org.csstudio.display.builder.editor.util.ParentHandler;
import org.csstudio.display.builder.editor.util.Rubberband;
import org.csstudio.display.builder.editor.util.WidgetNaming;
import org.csstudio.display.builder.editor.util.WidgetTransfer;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.model.widgets.ArrayWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.undo.UndoButtons;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

/** Display editor UI
 *
 *  <p>Shows DisplayModel, has Toolbar, Palette to add widgets.
 *  Allows interactive move/resize.
 *
 *  <p>Extends the basic JFXRepresentation scene layout:
 *  <pre>
 *  root (Border Pane)
 *   |
 *   + top:
 *   | Toolbar
 *   |
 *   + center:
 *     model_and_palette (SplitPane)
 *      |
 *      +----------------------------+
 *      |                            |
 *     model_root (Scroll)          palette
 *      |
 *     scroll_body (Group)
 *      |
 *      +-------------------------+
 *      |                         |
 *     widget_parent (Pane)      edit_tools
 *     (model rep. in back)      (on top)
 *      |                         |
 *     widget representations    selection tracker, points, rubberband
 *  </pre>
 *
 *  <p>widget_parent hosts representations of model widgets
 *
 *  <p>edit_tools holds GroupHandler, SelectionTracker.
 *  It's zoom level is updated to match the widget_parent's zoom level.
 *
 *  <p>scroll_body automatically resizes to hold all widget representations.
 *  Shows 'rubberband'
 *
 *  <p>model_root is ScrollPane, drop target for new widgets, starts 'rubberband'.
 *
 *  <p>The scroll_body is initially empty.
 *  As widget representations are added in the widget_parent,
 *  the scroll_body grows.
 *  The scroll bars of the editor automatically enable
 *  as the content of the scroll_body grows beyond the editor.
 *
 *  <p>The Rubberband hooks into editor mouse events to allow starting
 *  a rubberband anywhere in the visible region. Connecting the Rubberband
 *  to the scroll_body would limit selections to the region that bounds the
 *  visible widgets, one could not rubberband starting from 'below' the bottommost widget.
 *  The Rubberband, however, cannot add itself as a child to the scroll, so
 *  it uses the edit_tools for that.
 *
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class DisplayEditor
{
    private final JFXRepresentation toolkit;
    private final WidgetNaming widget_naming = new WidgetNaming();
    private final UndoableActionManager undo;
    private final WidgetSelectionHandler selection = new WidgetSelectionHandler();
    private final ParentHandler group_handler;
    private final SelectedWidgetUITracker selection_tracker;
    private AutoScrollHandler autoScrollHandler;
    private DisplayModel model;

    private final BorderPane root = new BorderPane();
    private ToolBar toolbar;
    private ScrollPane model_root;
    private Palette palette;
    private Node palette_node;
    private final SplitPane model_and_palette = new SplitPane();
    private Pane widget_parent;
    private final Group edit_tools = new Group();
    private ToggleButton grid;
    private ToggleButton snap;
    private ToggleButton coords;

    public static final String
            SNAP_GRID = "snap_grid",
            SNAP_WIDGETS = "snap_widgets",
            SHOW_COORDS = "show_coords";

    private static final Preferences prefs = PhoebusPreferenceService.userNodeForClass(DisplayEditorInstance.class);


    /** @param toolkit JFX Toolkit
     *  @param stack_size Number of undo/redo entries
     */
    public DisplayEditor(final JFXRepresentation toolkit, final int stack_size)
    {
        this.toolkit = toolkit;
        undo = new UndoableActionManager(stack_size);

        group_handler = new ParentHandler(edit_tools, selection);

        selection_tracker = new SelectedWidgetUITracker(toolkit, group_handler, selection, undo);
        selection_tracker.enableSnap(true);
        selection_tracker.enableGrid(true);
    }

    /** Create UI elements
     *  @return Root Node
     */
    public Parent create ()
    {
        model_root = toolkit.createModelRoot();
        model_root.getStyleClass().add("widget_pane_unfocused");
        autoScrollHandler = new AutoScrollHandler(model_root);

        final Group scroll_body = (Group) model_root.getContent();

        widget_parent = (Pane) scroll_body.getChildren().get(0);

        scroll_body.getChildren().add(edit_tools);

        palette = new Palette(this);
        palette_node = palette.create();

        SplitPane.setResizableWithParent(palette_node, false);
        edit_tools.getChildren().addAll(selection_tracker);
        hookListeners();

        toolbar = createToolbar();

        root.setCenter(model_and_palette);
        root.getStyleClass().add("no-border");

        configureReadonly(false);
        setGrid(prefs.getBoolean(SNAP_GRID, true));
        setSnap(prefs.getBoolean(SNAP_WIDGETS, true));
        setCoords(prefs.getBoolean(SHOW_COORDS, true));

        model_root.focusedProperty().addListener((observableValue, aBoolean, focused) -> {
            if (focused) {
                model_root.getStyleClass().add("widget_pane_focused");
            } else {
                model_root.getStyleClass().remove("widget_pane_focused");
            }
        });
        return root;
    }

    private ToolBar createToolbar()
    {
        final Button[] undo_redo = UndoButtons.createButtons(undo);

        final ComboBox<String> zoom_levels = new ComboBox<>();
        zoom_levels.getItems().addAll(JFXRepresentation.ZOOM_LEVELS);
        zoom_levels.setEditable(true);
        zoom_levels.setValue(JFXRepresentation.DEFAULT_ZOOM_LEVEL);
        zoom_levels.setTooltip(new Tooltip(Messages.SelectZoomLevel));
        zoom_levels.setPrefWidth(100.0);
        // For Ctrl-Wheel zoom gesture
        zoomListener zl = new zoomListener(zoom_levels);
        toolkit.setZoomListener(zl);
        zoom_levels.setOnAction(event ->
        {
            final String before = zoom_levels.getValue();
            if (before == null)
                return;
            final String actual = requestZoom(before);
            // Java 9 results in IndexOutOfBoundException
            // when combo is updated within the action handler,
            // so defer to another UI tick
            Platform.runLater(() ->
                zoom_levels.setValue(actual));
        });

        final MenuButton order = new MenuButton(null, null,
            createMenuItem(ActionDescription.TO_BACK),
            createMenuItem(ActionDescription.MOVE_UP),
            createMenuItem(ActionDescription.MOVE_DOWN),
            createMenuItem(ActionDescription.TO_FRONT));
        order.setTooltip(new Tooltip(Messages.Order));

        final MenuButton align = new MenuButton(null, null,
            createMenuItem(ActionDescription.ALIGN_LEFT),
            createMenuItem(ActionDescription.ALIGN_CENTER),
            createMenuItem(ActionDescription.ALIGN_RIGHT),
            createMenuItem(ActionDescription.ALIGN_TOP),
            createMenuItem(ActionDescription.ALIGN_MIDDLE),
            createMenuItem(ActionDescription.ALIGN_BOTTOM),
            createMenuItem(ActionDescription.ALIGN_GRID));
        align.setTooltip(new Tooltip(Messages.Align));

        final MenuButton size = new MenuButton(null, null,
            createMenuItem(ActionDescription.MATCH_WIDTH),
            createMenuItem(ActionDescription.MATCH_HEIGHT));
        size.setTooltip(new Tooltip(Messages.Size));

        final MenuButton dist = new MenuButton(null, null,
            createMenuItem(ActionDescription.DIST_HORIZ),
            createMenuItem(ActionDescription.DIST_VERT),
            createMenuItem(ActionDescription.DIST_HORIZ_GAP),
            createMenuItem(ActionDescription.DIST_VERT_GAP));
        dist.setTooltip(new Tooltip(Messages.Distribute));

        // Use the first item as the icon for the drop-down...
        try
        {
            order.setGraphic(ImageCache.getImageView(ActionDescription.TO_BACK.getIconResourcePath()));
            align.setGraphic(ImageCache.getImageView(ActionDescription.ALIGN_LEFT.getIconResourcePath()));
            size.setGraphic(ImageCache.getImageView(ActionDescription.MATCH_WIDTH.getIconResourcePath()));
            dist.setGraphic(ImageCache.getImageView(ActionDescription.DIST_HORIZ.getIconResourcePath()));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot load icon", ex);
        }


        return new ToolBar(
            grid = createToggleButton(ActionDescription.ENABLE_GRID),
            snap = createToggleButton(ActionDescription.ENABLE_SNAP),
            coords = createToggleButton(ActionDescription.ENABLE_COORDS),
            new Separator(),
            order,
            align,
            size,
            dist,
            new Separator(),
            undo_redo[0],
            undo_redo[1],
            new Separator(),
            zoom_levels);
    }

    public class zoomListener implements Consumer<String>
    {
        ComboBox<String> zoom_levels;

        public zoomListener(ComboBox<String> zoom_levels)
        {
            this.zoom_levels = zoom_levels;
        }
        @Override
        public void accept(String zoom_level)
        {
            zoom_levels.getSelectionModel().clearSelection();
            zoom_levels.getEditor().setText(zoom_level);
            edit_tools.getTransforms().setAll(widget_parent.getTransforms());
        }
    }

    private MenuItem createMenuItem(final ActionDescription action)
    {
        final MenuItem item = new MenuItem();
        try
        {
            item.setGraphic(ImageCache.getImageView(action.getIconResourcePath()));
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot load action icon", ex);
        }
        item.setText(action.getToolTip());
        item.setOnAction(event -> action.run(this));
        return item;
    }

    private ToggleButton createToggleButton(final ActionDescription action)
    {
        final ToggleButton button = new ToggleButton();
        try
        {
            button.setGraphic(ImageCache.getImageView(action.getIconResourcePath()));
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot load action icon", ex);
        }
        button.setTooltip(new Tooltip(action.getToolTip()));
        button.setSelected(true);
        button.selectedProperty()
              .addListener((observable, old_value, enabled) -> action.run(this, enabled) );
        return button;
    }

    /** @return ToolBar */
    public ToolBar getToolBar()
    {
        return toolbar;
    }

    /** @return Control in the central editor region to which a context menu could be attached */
    public Control getContextMenuNode()
    {
        return model_root;
    }

    /** @return Selection tracker */
    public SelectedWidgetUITracker getSelectedWidgetUITracker()
    {
        return selection_tracker;
    }

    /** @return Selection tracker */
    public WidgetSelectionHandler getWidgetSelectionHandler()
    {
        return selection;
    }

    public AutoScrollHandler getAutoScrollHandler()
    {
        return autoScrollHandler;
    }

    /** @return Undo manager */
    public UndoableActionManager getUndoableActionManager()
    {
        return undo;
    }

    private void hookListeners ()
    {
        toolkit.addListener(new ToolkitListener()
        {
            @Override
            public void handleClick (final Widget widget, final boolean with_control)
            {
                logger.log(Level.FINE, "Selected {0}", widget);
                // Toggle selection of widget when Ctrl is held
                if ( with_control )
                    selection.toggleSelection(widget);
                else
                    selection.setSelection(Arrays.asList(widget));
            }
        });

        // De-select all widgets if plain left mouse button is clicked on background
        model_root.setOnMousePressed(event ->
        {
            // Don't do that on control-click (to add/remove to current selection)
            // nor on right button (to open context menu)
            if (event.isShortcutDown()   ||   ! event.isPrimaryButtonDown())
                return;
            logger.log(Level.FINE, "Mouse pressed in 'editor', de-select all widgets");
            event.consume();
            selection.clear();
            model_root.requestFocus(); // Request focus to be able to intercept CTRL/CMD+A
        });

        new Rubberband(model_root, edit_tools, this::handleRubberbandSelection);
        new PointsBinding(edit_tools, selection_tracker::gridConstrain, selection, undo);

        // Attach D&Drop to the widget_parent which is zoomed,
        // so drop will have the zoomed coordinate system
        WidgetTransfer.addDropSupport(widget_parent, group_handler, selection_tracker, widgets -> addWidgets(widgets, false));

        model_root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
    }

    private void handleKeyPress(final KeyEvent event)
    {
        if (isReadonly())
            return;

        if (event.isShortcutDown() && event.getCode().equals(KeyCode.A)) {
            getWidgetSelectionHandler().setSelection(model.getChildren());
        }
        else {
            WidgetTree.handleGroupOrOrderKeys(event, this);
        }
    }

    private void handleRubberbandSelection(final Rectangle2D region, final boolean update_existing)
    {
        // Is a widget type to be created selected in the palette?
        final WidgetDescriptor desc = palette.getSelectedWidgetType();
        if (desc == null)
            selectWidgetsInRegion(region, update_existing);
        else
            createWidget(region, desc);
    }

    private void selectWidgetsInRegion(final Rectangle2D region, final boolean update_existing)
    {
        final List<Widget> found = GeometryTools.findWidgets(model, region);
        logger.log(Level.FINE, "Selected widgets in {0}: {1}",  new Object[] { region, found });
        if (update_existing)
            for (Widget widget : found)
                selection.toggleSelection(widget);
        else
            selection.setSelection(found);
    }

    /** @param region Requested location and size of the new widget
     *  @param desc Description for widget to create
     */
    private void createWidget(final Rectangle2D region, final WidgetDescriptor desc)
    {
        // Create widget of that type
        final Widget widget = desc.createWidget();

        // Size to rubberbanded region, optionally constrained by grid
        final Point2D location = selection_tracker.gridConstrain(region.getMinX(), region.getMinY());
        widget.propX().setValue((int) location.getX());
        widget.propY().setValue((int) location.getY());
        final Point2D size = selection_tracker.gridConstrain(region.getWidth(), region.getHeight());
        widget.propWidth().setValue((int) size.getX());
        widget.propHeight().setValue((int) size.getY());

        // Add to model
        final ChildrenProperty target = model.runtimeChildren();
        widget_naming.setDefaultName(model, widget);
        undo.execute(new AddWidgetAction(selection, target, widget));

        // De-activate the palette, so rubberband will from now on select widgets
        palette.clearSelectedWidgetType();

        // Select the new widget
        selection.setSelection(Arrays.asList(widget));
    }

    /** @param widgets Widgets to be added to existing model
     *  @param correct_scroll_origin Correct widget locations by scroll pane origin?
     */
    private void addWidgets(final List<Widget> widgets, final boolean correct_scroll_origin)
    {
        // Dropped into a sub-group or the main display?
        ChildrenProperty target = group_handler.getActiveParentChildren();
        if (target == null)
            target = model.runtimeChildren();
        Widget container = target.getWidget();
        // Correct all dropped widget locations relative to container
        Point2D offset = GeometryTools.getContainerOffset(container);

        final Point2D origin;
        if (correct_scroll_origin)
        {
            // Account for scroll pane and zoom
            final double zoom = toolkit.getZoom();
            final Point2D zoomed = JFXGeometryTools.getContentOrigin(model_root);
            origin = new Point2D(zoomed.getX() / zoom,
                                 zoomed.getY() / zoom);
        }
        else
            origin = new Point2D(0, 0);

        int dx = (int) (offset.getX() - origin.getX());
        int dy = (int) (offset.getY() - origin.getY());

        // Add dropped widgets
        try
        {
            final ListIterator<Widget> it = widgets.listIterator();
            if (container instanceof ArrayWidget)
            {
                if (target.getValue().isEmpty())
                {   // Drop first widget into ArrayWidget
                    Widget widget = it.next();
                    widget.propX().setValue(widget.propX().getValue() - dx);
                    widget.propY().setValue(widget.propY().getValue() - dy);
                    widget_naming.setDefaultName(container.getDisplayModel(), widget);
                    undo.execute(new AddWidgetAction(selection, target, widget));
                }

                // Hide highlight, since not adding to ArrayWidget container
                if (it.hasNext())
                    group_handler.hide();

                // Re-assign target, container, etc. to use ArrayWidget's parent
                target = ChildrenProperty.getParentsChildren(container);
                container = target.getWidget();
                offset = GeometryTools.getContainerOffset(container);
                dx = (int) (offset.getX() - origin.getX());
                dy = (int) (offset.getY() - origin.getY());
            }
            while (it.hasNext())
            {
                Widget widget = it.next();
                widget.propX().setValue(widget.propX().getValue() - dx);
                widget.propY().setValue(widget.propY().getValue() - dy);
                widget_naming.setDefaultName(container.getDisplayModel(), widget);
                undo.execute(new AddWidgetAction(selection, target, widget));
            }
            selection.setSelection(widgets);
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot add widgets", ex);
        }
    }

    private void configureReadonly(final boolean read_only)
    {
        // toolbar visibility is used in isReadonly()
        if (read_only)
        {
            model_and_palette.getItems().setAll(model_root);
            selection_tracker.enableChanges(false);
            root.setTop(null);
            autoScrollHandler.enable(false);
        }
        else
        {
            model_and_palette.getItems().setAll(model_root, palette_node);
            model_and_palette.setDividerPositions(1);
            selection_tracker.enableChanges(true);
            root.setTop(toolbar);
            autoScrollHandler.enable(true);
        }
    }

    boolean isReadonly()
    {
        // Use toolbar visibility as read/write flag
        return root.getTop() == null;
    }

    /** Set Model
     *  @param model Model to show and edit
     */
    public void setModel(final DisplayModel model)
    {
        // Model in editor should have input file information
        // to allow resolving images etc. relative to that file
        if (model.getUserData(DisplayModel.USER_DATA_INPUT_FILE) == null)
            logger.log(Level.SEVERE, "Model lacks input file information");

        undo.clear();
        widget_naming.clear();
        selection.clear();
        group_handler.setModel(model);
        selection_tracker.setModel(model);

        final DisplayModel old_model = this.model;
        if (old_model != null)
            toolkit.disposeRepresentation(old_model);
        this.model = Objects.requireNonNull(model);

        // Create representation for model items
        try
        {
            configureReadonly(EditorUtil.isDisplayReadOnly(model));
            toolkit.representModel(widget_parent, model);
        }
        catch (final Exception ex)
        {
            logger.log(Level.SEVERE, "Error representing model", ex);
        }


        // Bring up the model's properties.
        try
        {
            toolkit.execute(() -> selection.clear());
        }
        catch (final Exception ex)
        {
            logger.log(Level.SEVERE, "Error selecting model", ex);
        }
    }

    /** @return Currently edited model */
    public DisplayModel getModel()
    {
        return model;
    }

    /** Copy currently selected widgets to clipboard
     *  @return Widgets that were copied or <code>null</code>
     */
    public List<Widget> copyToClipboard()
    {
        if (selection_tracker.isInlineEditorActive())
            return null;

        final List<Widget> widgets = selection.getSelection();
        if (widgets.isEmpty())
            return null;

        final String xml;
        try
        {
            xml = ModelWriter.getXML(widgets);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create content for clipboard", ex);
            return null;
        }

        final ClipboardContent content = new ClipboardContent();
        content.putString(xml);
        Clipboard.getSystemClipboard().setContent(content);
        return widgets;
    }

    /** Cut (delete) selected widgets, placing them on the clipboard */
    public void cutToClipboard()
    {
        if (selection_tracker.isInlineEditorActive())
            return;

        // Strictly speaking, delete would not copy to the clipboard...
        final List<Widget> widgets = copyToClipboard();
        if (widgets == null)
            return;
        undo.execute(new RemoveWidgetsAction(selection, widgets));
    }

    /** Paste widgets from clipboard
     *  @param x Desired coordinate of upper-left widget ..
     *  @param y .. when pasted
     */
    public void pasteFromClipboard(int x, int y)
    {
        if (selection_tracker.isInlineEditorActive())
            return;

        final String xml = Clipboard.getSystemClipboard().getString();
        // Anything on clipboard?
        if (xml == null)
            return;
        // Does it look like widget XML?
        if (! (xml.startsWith("<?xml")  &&
               xml.contains("<display")))
            return;

        // Correct the y coordinate, measured inside this editor,
        // by height of toolbar
        y -= toolbar.getHeight();

        // Correct coordinates by zoom factor
        final double zoom = toolkit.getZoom();
        x = (int) Math.round(x / zoom);
        y = (int) Math.round(y / zoom);

        try
        {
            final DisplayModel model = ModelReader.parseXML(xml);
            final List<Widget> widgets = model.getChildren();
            logger.log(Level.FINE, "Pasted {0} widgets", widgets.size());

            Point2D constr = selection_tracker.gridConstrain(x, y);
            x = (int)constr.getX();
            y = (int)constr.getY();
            GeometryTools.moveWidgets(x, y, widgets);
            final Rectangle2D bounds = GeometryTools.getBounds(widgets);
            // Potentially activate group at drop point
            group_handler.locateParent(x, y, bounds.getWidth(), bounds.getHeight());
            addWidgets(widgets, true);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Failed to paste content of clipboard", ex);
        }
    }

    /** Remove (delete) selected widgets */
    public void removeWidgets()
    {
        if (selection_tracker.isInlineEditorActive())
            return;

        final List<Widget> widgets = selection.getSelection();
        if (widgets.isEmpty())
            return;

        undo.execute(new RemoveWidgetsAction(selection, widgets));
    }

    /** Duplicate currently selected widgets without overwriting clipboard
     *  @return Widgets that were duplicated or <code>null</code>
     */
    public List<Widget> duplicateWidgets()
    {
        if (selection_tracker.isInlineEditorActive())
            return null;

        List<Widget> widgets = selection.getSelection();
        if (widgets.isEmpty())
            return null;

        final String xml;
        try
        {
        	//Export to XML
            xml = ModelWriter.getXML(widgets);
            List<Rectangle2D> display_bounds = new ArrayList<>();
            for (Widget widget : widgets)
            {
                display_bounds.add(GeometryTools.getDisplayBounds(widget));
            }
            //Import back from XML
            final DisplayModel model = ModelReader.parseXML(xml);
            widgets = model.getChildren();
            logger.log(Level.FINE, "Duplicated {0} widgets", widgets.size());

            for (int index = 0; index < widgets.size(); index++)
            {
                widgets.get(index).propX().setValue((int)display_bounds.get(index).getMinX() + 20);
                widgets.get(index).propY().setValue((int)display_bounds.get(index).getMinY() + 20);
            }
            final Rectangle2D bounds = GeometryTools.getBounds(widgets);
            // Potentially activate group at duplicate point
            group_handler.locateParent(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
            addWidgets(widgets, false);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot duplicate widgets", ex);
            return null;
        }

        return widgets;
    }

    /** @param pattern (Partial) name of widgets to select */
    public void selectWidgetsByName(final String pattern)
    {
        if (selection_tracker.isInlineEditorActive())
            return;

        // Search in background thread..
        ModelThreadPool.getExecutor().submit(() ->
        {
            final List<Widget> widgets = new ArrayList<>();
            addWidgetsByName(widgets, getModel(), pattern);

            // Update selection in UI thread
            Platform.runLater(() -> selection.setSelection(widgets));
        });
    }

    private void addWidgetsByName(final List<Widget> widgets, Widget widget, final String pattern)
    {
        if (widget.getName().contains(pattern))
            widgets.add(widget);

        ChildrenProperty children = ChildrenProperty.getChildren(widget);
        if (children != null)
            for (Widget child : children.getValue())
                addWidgetsByName(widgets, child, pattern);
        else if (widget instanceof TabsWidget)
        {
            final TabsWidget tabs = (TabsWidget) widget;
            for (TabItemProperty tab : tabs.propTabs().getValue())
                for (Widget child : tab.children().getValue())
                    addWidgetsByName(widgets, child, pattern);
        }
    }

    /** @param level_spec Zoom level specification like "123 %"
     *  @return Zoom spec actually used
     */
    public String requestZoom(String level_spec)
    {
        level_spec = toolkit.requestZoom(level_spec);
        // Toolkit sets the widget_parent transforms to one Scale().
        // Apply same to the edit_tools
        edit_tools.getTransforms().setAll(widget_parent.getTransforms());

        return level_spec;
    }

    /** @return Zoom level, 2.0 for '200 %' */
    public double getZoom()
    {
        return toolkit.getZoom();
    }

    /** @param snap Snap Grid on/off */
    public void setGrid(final boolean snap)
    {
        grid.setSelected(snap);
        selection_tracker.enableGrid(snap);
        // Update pref about last snap state
        saveGrid(snap);
    }

    /** @param snap Snap Grid on/off */
    public static void saveGrid(final boolean snap)
    {
        prefs.putBoolean(SNAP_GRID, snap);
    }

    /** @param snap Snap Widgets on/off */
    public void setSnap(final boolean snap)
    {
        this.snap.setSelected(snap);
        selection_tracker.enableSnap(snap);
        // Update pref about last snap state
        saveSnap(snap);
    }

    /** @param snap Snap Widgets on/off */
    public static void saveSnap(final boolean snap)
    {
        prefs.putBoolean(SNAP_WIDGETS, snap);
    }

    /** @param show Show Coordinates on/off */
    public void setCoords(final boolean show)
    {
        coords.setSelected(show);
        getSelectedWidgetUITracker().setShowLocationAndSize(show);
        // Update pref about last show state
        saveCoords(show);
    }

    /** @param show Show Coordinates on/off */
    public static void saveCoords(final boolean show)
    {
        prefs.putBoolean(SHOW_COORDS, show);
    }

    public void dispose()
    {
        autoScrollHandler.enable(false);
        if (model != null)
            toolkit.disposeRepresentation(model);
        model = null;
    }
}
