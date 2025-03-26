/*******************************************************************************
 * Copyright (c) 2015-2024 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.tracker;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.WidgetSelectionHandler;
import org.csstudio.display.builder.editor.undo.SetMacroizedWidgetPropertyAction;
import org.csstudio.display.builder.editor.undo.UpdateWidgetLocationAction;
import org.csstudio.display.builder.editor.util.GeometryTools;
import org.csstudio.display.builder.editor.util.ParentHandler;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;
import org.phoebus.ui.javafx.PlatformInfo;
import org.phoebus.ui.javafx.Tracker;
import org.phoebus.ui.undo.CompoundUndoableAction;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.shape.Rectangle;

/** Rubber-band-type tracker of currently selected widgets in UI.
 *
 *  <p>UI element that allows selecting widgets,
 *  moving and resizing them.
 *
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class SelectedWidgetUITracker extends Tracker
{
    private final ToolkitRepresentation<Parent, Node> toolkit;
    private final ParentHandler group_handler;
    private final UndoableActionManager undo;

    private final TrackerGridConstraint grid_constraint;
    private final TrackerSnapConstraint snap_constraint;

    /** Inline editor for widget's PV name or text */
    private TextField inline_editor = null;

    /** Widgets to track */
    private List<Widget> widgets = Collections.emptyList();

    /** Break update loops JFX change -> model change -> JFX change -> ... */
    private boolean updating = false;

    /** Update tracker to match changed widget position */
    private final WidgetPropertyListener<Integer> position_listener;

    private Group widget_highlights = new Group();

    /** Construct a tracker.
     *
     *  <p>It remains invisible until it is asked to track widgets
     *  @param toolkit Toolkit
     *  @param group_handler Group handler
     *  @param selection Selection handler
     *  @param undo 'Undo' manager
     */
    public SelectedWidgetUITracker(final JFXRepresentation toolkit,
                                   final ParentHandler group_handler,
                                   final WidgetSelectionHandler selection,
                                   final UndoableActionManager undo)
    {
        this.toolkit = toolkit;
        this.group_handler = group_handler;
        this.undo = undo;
        this.snap_constraint = new TrackerSnapConstraint(toolkit, this);
        this.grid_constraint = new TrackerGridConstraint();

        // Updates to the position can originate from any thread,
        // but tracker update must be on UI thread
        position_listener = (p, o, n) ->  toolkit.execute(this::updateTrackerFromWidgets);

        setVisible(false);

        // Track currently selected widgets
        selection.addListener(this::setSelectedWidgets);

        // Pass control-click (Mac: Command) down to underlying widgets
        addEventFilter(MouseEvent.MOUSE_CLICKED, event ->
        {
            if (event.isShortcutDown())
                passClickToWidgets(event);
        });

        // Allow 'dragging' selected widgets
        setOnDragDetected(event ->
        {
            // .. when Control (Mac: Alt) is down.
            // Otherwise mouse-down-and-move moves the tracker, doesn't start D&D
            if (! (PlatformInfo.is_mac_os_x
                   ? event.isAltDown()
                   : event.isControlDown()))
                return;

            logger.log(Level.FINE, "Starting to drag {0}", widgets);
            final String xml;
            try
            {
                xml = ModelWriter.getXML(widgets);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot drag-serialize", ex);
                return;
            }
            final Dragboard db = startDragAndDrop(TransferMode.COPY);
            final ClipboardContent content = new ClipboardContent();
            content.putString(xml);
            db.setContent(content);

            // Would like to set tacker snapshot as drag image, but GTK error
            // as soon as the snapshot width is >= 128 pixel:
            // "GdkPixbuf-CRITICAL **: gdk_pixbuf_new_from_data: assertion `width > 0' failed"
            //
            //  final WritableImage snapshot = tracker.snapshot(null, null);
            //  System.out.println(snapshot.getWidth() + " x " + snapshot.getHeight());
            //  db.setDragView(snapshot);
            event.consume();
        });

        // When tracker moved, update widgets
        setListener(this::updateWidgetsFromTracker);

        // Add highlights _before_ rest of tracker so they're behind
        // the tracker and not selectable
        getChildren().add(0, widget_highlights);
    }

    /** @param model Model where to track selection */
    public void setModel(final DisplayModel model)
    {
        grid_constraint.configure(model);
    }

    /** @return Model where to track selection */
    public DisplayModel getModel()
    {
        return grid_constraint.getModel();
    }

    /** Apply enabled constraints to requested position
     *
     *  @param x Requested X position
     *  @param y Requested Y position
     *  @return Constrained coordinate
     */
    @Override
    protected Point2D constrain(final double x, final double y)
    {
        Point2D result = super.constrain(x, y);

        result = gridConstrain(result.getX(), result.getY());
        result = snapConstrain(result.getX(), result.getY());

        return result;
    }

    /** Apply enabled constraints to requested position
     *
     *  @param x Requested X position
     *  @param y Requested Y position
     *  @return Constrained coordinate
     */
    public Point2D gridConstrain(final double x, final double y)
    {
        if (grid_constraint.isEnabled())
            return grid_constraint.constrain(x, y);
        else
            return new Point2D(x, y);
    }

    /** Apply enabled constraints to requested position
     *
     *  @param x Requested X position
     *  @param y Requested Y position
     *  @return Constrained coordinate
     */
    public Point2D snapConstrain(final double x, final double y)
    {
        if (snap_constraint.isEnabled())
            return snap_constraint.constrain(x, y);
        else
            return new Point2D(x, y);
    }

    @Override
    protected void handleKeyEvent(final KeyEvent event)
    {
        // Update move value bigDelta to grid size
        {
            final DisplayModel model = getModel();
            if(model != null) {
                bigDeltaX = model.propGridStepX().getValue();
                bigDeltaY = model.propGridStepY().getValue();
            }
        }

        super.handleKeyEvent(event);
        if (event.isConsumed())
            group_handler.hide();
    }

    /** @param event {@link MouseEvent} */
    @Override
    protected void mousePressed(final MouseEvent event)
    {
        if (event.getClickCount() == 1)
            super.mousePressed(event);
        else
        {
            event.consume();
            if (enable_changes  &&  widgets.size() == 1  &&  inline_editor == null)
                createInlineEditor(widgets.get(0));
        }
    }

    /** @param event {@link MouseEvent} */
    @Override
    protected void mouseReleased(final MouseEvent event)
    {
        if (inline_editor == null)
            super.mouseReleased(event);
    }

    /** The 'global' key handlers for copy/paste/delete
     *  must be suppressed when the inline editor is active,
     *  because otherwise trying to paste a PV name will
     *  paste a new widget, or deleting a part of a PV name
     *  will delete the currently selected widget.
     *
     *  <p>Since both RCP and JavaFX listen to the keys,
     *  the most practical solution was to have global actions
     *  check this flag
     *
     *  @return Is the inline editor active?
     */
    public boolean isInlineEditorActive()
    {
        return inline_editor != null;
    }

    /** Create an inline editor
     *
     *  <p>Depending on the widget's properties, it will edit
     *  the PV name or the text.
     *
     *  @param widget Widget on which to create an inline editor
     */
    private void createInlineEditor(final Widget widget)
    {
        // Check for an inline-editable property
        Optional<WidgetProperty<String>> check;

        // Defaulting to PV name or text property with some hard-coded exceptions.
        // Alternative if the list of hard-coded widgets grows:
        // Add Widget#getInlineEditableProperty()
        if (widget instanceof ActionButtonWidget)
            check = Optional.of(((ActionButtonWidget) widget).propText());
        else if (widget instanceof GroupWidget)
            check = Optional.of(((GroupWidget) widget).propName());
        else
            check = widget.checkProperty(CommonWidgetProperties.propPVName);
        if (! check.isPresent())
            check = widget.checkProperty(CommonWidgetProperties.propText);
        if (! check.isPresent())
            return;

        // Create text field, aligned with widget, but assert minimum size
        final MacroizedWidgetProperty<String> property = (MacroizedWidgetProperty<String>)check.get();
        // Allow editing newlines as '\n'
        inline_editor = new TextField(property.getSpecification().replace("\n", "\\n"));
        // 'Managed' text field would assume some default size,
        // but we set the exact size in here
        inline_editor.setManaged(false);
        inline_editor.setPromptText(property.getDescription()); // Not really shown since TextField will have focus
        inline_editor.setTooltip(new Tooltip(property.getDescription()));
        inline_editor.relocate(tracker.getX(), tracker.getY());
        inline_editor.resize(Math.max(100, tracker.getWidth()), Math.max(20, tracker.getHeight()));
        getChildren().add(inline_editor);

        // Add autocomplete menu if editing property PVName
        if (property.getName().equals(CommonWidgetProperties.propPVName.getName()))
            PVAutocompleteMenu.INSTANCE.attachField(inline_editor);

        // On enter or lost focus, update the property. On Escape, just close.
        // Using atomic ref as holder so that focused_listener can remove itself
        final AtomicReference<ChangeListener<? super Boolean>> focused_listener = new AtomicReference<>();
        focused_listener.set((prop, old, focused) ->
        {
            if (! focused)
            {
                final String new_text = inline_editor.getText().replace("\\n", "\n");
                if (!property.getSpecification().equals(new_text))
                    undo.execute(new SetMacroizedWidgetPropertyAction(property, new_text));
                inline_editor.focusedProperty().removeListener(focused_listener.get());
                // Close when focus lost
                closeInlineEditor();
            }
        });

        inline_editor.setOnAction(event ->
        {
            final String new_text = inline_editor.getText().replace("\\n", "\n");
            if (!property.getSpecification().equals(new_text))
                undo.execute(new SetMacroizedWidgetPropertyAction(property, new_text));
            inline_editor.focusedProperty().removeListener(focused_listener.get());
            closeInlineEditor();
        });

        inline_editor.setOnKeyPressed(event ->
        {
            switch (event.getCode())
            {
            case ESCAPE:
                event.consume();
                inline_editor.focusedProperty().removeListener(focused_listener.get());
                closeInlineEditor();
            default:
            }
        });

        inline_editor.focusedProperty().addListener(focused_listener.get());

        inline_editor.selectAll();
        inline_editor.requestFocus();
    }

    private void closeInlineEditor()
    {
        getChildren().remove(inline_editor);
        inline_editor = null;
    }

    private boolean requestFocusIsDisabled = false;
    public void setDisableRequestFocus(boolean requestFocusIsDisabled) {
        this.requestFocusIsDisabled = requestFocusIsDisabled;
    }

    /** Locate widgets that would be 'clicked' by a mouse event's location */
    private class ClickWidgets extends RecursiveTask<Boolean>
    {
        private static final long serialVersionUID = 7120422764377430463L;
        private final MouseEvent event;
        private final List<Widget> widgets;

        ClickWidgets(final MouseEvent event, final List<Widget> widgets)
        {
            this.event = event;
            this.widgets = widgets;
        }

        @Override
        protected Boolean compute()
        {
            return click(widgets);
        }

        /** @param widgets Widgets to click
         *  @return Was at least one widget clicked?
         */
        private Boolean click(final List<Widget> widgets)
        {
            boolean clicked = false;

            final int N = widgets.size();
            if (N > TrackerSnapConstraint.PARALLEL_THRESHOLD)
            {
                final int split = N / 2;
                final ClickWidgets sub1 = new ClickWidgets(event, widgets.subList(0, split));
                final ClickWidgets sub2 = new ClickWidgets(event, widgets.subList(split, N));
                // Spawn sub1, handle sub2 in this thread
                sub1.fork();
                clicked = sub2.compute();
                if (sub1.join())
                    clicked = true;

            }
            else
            {
                for (Widget widget : widgets)
                {
                    // If there are child widgets, first check those.
                    // If one of them gets clicked, skip checking the parent (e.g. group)
                    // since 'selecting' a child should not toggle the parent's selection.
                    final ChildrenProperty children = ChildrenProperty.getChildren(widget);
                    if (children != null  &&
                        click(children.getValue()))
                        clicked = true;

                    // If no child widget got clicked, check widget itself
                    if (! clicked)
                        if (GeometryTools.getDisplayBounds(widget).contains(event.getX(), event.getY()))
                        {
                            logger.log(Level.FINE, () -> "Tracker passes click through to " + widget);
                            toolkit.execute(() -> toolkit.fireClick(widget, event.isShortcutDown()));
                            clicked = true;
                        }
                }
            }

            return clicked;
        }
    }

    /** Tracker is in front of the widgets that it handles,
     *  so it receives all mouse clicks.
     *  When 'Control' key is down, that event should be passed
     *  down to the widgets under the tracker, but JFX blocks them.
     *  This method locates all widgets that contain the mouse coord.
     *  and fires a 'click' on them.
     *  @param event Mouse event that needs to be passed down
     */
    private void passClickToWidgets(final MouseEvent event)
    {
        final DisplayModel model = getModel();
        if (model != null) {
            new ClickWidgets(event, model.getChildren()).compute();
        }
    }

    @Override
    public void setPosition(final double x, final double y, double width, double height)
    {
        if (width < 1.0)
            width = 1.0;
        if (height < 1.0)
            height = 1.0;
        super.setPosition(x, y, width, height);
        // As tracker is being moved, highlight group under tracker
        group_handler.locateParent(x, y, width, height);
    }

    /** Updates widgets to current tracker location and size */
    private void updateWidgetsFromTracker(final Rectangle2D original, final Rectangle2D current)
    {
        if (updating)
            return;
        updating = true;
        try
        {
            group_handler.hide();

            final List<Rectangle2D> orig_position =
                widgets.stream().map(GeometryTools::getBounds).collect(Collectors.toList());

            // If there was only one widget, the tracker bounds represent
            // the desired widget location and size.
            // But tracker bounds can apply to one or more widgets, so need to
            // determine the change in tracker bounds, apply those to each widget.
            final double dx = current.getMinX()   - original.getMinX();
            final double dy = current.getMinY()   - original.getMinY();
            final double dw = current.getWidth()  - original.getWidth();
            final double dh = current.getHeight() - original.getHeight();
            final int N = orig_position.size();

            // Use compound action if there's more than one widget
            final CompoundUndoableAction compound = N>1
                ? new CompoundUndoableAction(Messages.UpdateWidgetLocation)
                : null;
            for (int i=0; i<N; ++i)
            {
                final Widget widget = widgets.get(i);
                final Rectangle2D orig = orig_position.get(i);

                final ChildrenProperty orig_parent_children = ChildrenProperty.getParentsChildren(widget);
                ChildrenProperty parent_children = group_handler.getActiveParentChildren();
                if (parent_children == null)
                    parent_children = widget.getDisplayModel().runtimeChildren();

                final int orig_index;
                if (orig_parent_children == parent_children)
                {   // Slightly faster since parent stays the same
                    if (! widget.propX().isUsingWidgetClass())
                        widget.propX().setValue((int) (orig.getMinX() + dx));
                    if (! widget.propY().isUsingWidgetClass())
                        widget.propY().setValue((int) (orig.getMinY() + dy));
                    orig_index = -1;
                }
                else
                {   // Update to new parent
                    if (widget.getDisplayModel().isClassModel())
                    {
                        logger.log(Level.WARNING, "Widget hierarchy is not permitted for class model");
                        return;
                    }

                    final Point2D old_offset = GeometryTools.getDisplayOffset(widget);
                    orig_index = orig_parent_children.removeChild(widget);
                    parent_children.addChild(widget);
                    final Point2D new_offset = GeometryTools.getDisplayOffset(widget);

                    logger.log(Level.FINE, "{0} moves from {1} ({2}) to {3} ({4})",
                               new Object[] { widget, orig_parent_children.getWidget(), old_offset,
                                                      parent_children.getWidget(), new_offset});
                    // Account for old and new display offset
                    if (! widget.propX().isUsingWidgetClass())
                        widget.propX().setValue((int) (orig.getMinX() + dx + old_offset.getX() - new_offset.getX()));
                    if (! widget.propY().isUsingWidgetClass())
                        widget.propY().setValue((int) (orig.getMinY() + dy + old_offset.getY() - new_offset.getY()));
                }
                if (! widget.propWidth().isUsingWidgetClass())
                    widget.propWidth().setValue((int) Math.max(1, orig.getWidth() + dw));
                if (! widget.propHeight().isUsingWidgetClass())
                    widget.propHeight().setValue((int) Math.max(1, orig.getHeight() + dh));

                final UndoableAction step = new UpdateWidgetLocationAction(widget,
                                                                           orig_parent_children,
                                                                           parent_children,
                                                                           orig_index,
                                                                           (int) orig.getMinX(),  (int) orig.getMinY(),
                                                                           (int) orig.getWidth(), (int) orig.getHeight());
                if (compound == null)
                    undo.add(step);
                else
                    compound.add(step);
            }
            if (compound != null)
                undo.add(compound);
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Failed to move/resize widgets", ex);
        }
        finally
        {
            updating = false;
            updateTrackerFromWidgets();
        }
    }

    private void updateTrackerFromWidgets()
    {
        if (updating)
            return;

        final Rectangle2D rect = GeometryTools.getDisplayBounds(widgets);
        updating = true;

        // Update overall tracker rectangle
        setPosition(rect);

        // Add a highlight to each selected widget
        // (tracker area may cover widgets that are not actually selected)
        // Only do that for 2 or more widgets.
        // For a single widget, the tracker rectangle is sufficient.
        widget_highlights.getChildren().clear();
        if (widgets.size() > 1)
            for (Widget widget : widgets)
            {
                final Rectangle2D bounds = GeometryTools.getDisplayBounds(widget);
                final Rectangle highlight = new Rectangle(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
                highlight.getStyleClass().add("tracker_highlight");
                // highlight is 'behind' rest of tracker, but still pass mouse clicks through to widgets
                highlight.setMouseTransparent(true);
                widget_highlights.getChildren().add(highlight);
            }

        updating = false;
    }

    /** @param enable Enable grid? */
    public void enableGrid(final boolean enable)
    {
        grid_constraint.setEnabled(enable);
    }

    /** @return Enable grid */
    public boolean getEnableGrid()
    {
        return grid_constraint.isEnabled();
    }

    /** @param enable Enable snap? */
    public void enableSnap(final boolean enable)
    {
        snap_constraint.setEnabled(enable);
    }

    /** @return Enable snap */
    public boolean getEnableSnap()
    {
        return snap_constraint.isEnabled();
    }

    /** Activate the tracker
     *  @param widgets Widgets to control by tracker,
     *                 empty to de-select
     */
    public void setSelectedWidgets(final List<Widget> widgets)
    {
        unbindFromWidgets();

        this.widgets = Objects.requireNonNull(widgets);
        if (widgets.size() <= 0)
        {
            setVisible(false);
            group_handler.hide();
            return;
        }

        try
        {
            snap_constraint.configure(widgets.get(0).getDisplayModel(), widgets);
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot obtain widget model", ex);
        }

        setVisible(true);

        updateTrackerFromWidgets();

        startDrag(null); // Get 'orig' position for keyboard moves

        bindToWidgets();

        if (!requestFocusIsDisabled) {
            // Get focus to allow use of arrow keys
            Platform.runLater(() -> tracker.requestFocus());
        }
    }

    @Override
    protected void endMouseDrag(final MouseEvent event)
    {   // Hide snap lines when drag ends
        super.endMouseDrag(event);
        snap_constraint.setVisible(false);

        // Clear group_handler
        group_handler.hide();
    }

    private void bindToWidgets()
    {
        for (final Widget widget : widgets)
        {
            widget.propX().addPropertyListener(position_listener);
            widget.propY().addPropertyListener(position_listener);
            widget.propWidth().addPropertyListener(position_listener);
            widget.propHeight().addPropertyListener(position_listener);
        }
    }

    private void unbindFromWidgets()
    {
        for (final Widget widget : widgets)
        {
            widget.propX().removePropertyListener(position_listener);
            widget.propY().removePropertyListener(position_listener);
            widget.propWidth().removePropertyListener(position_listener);
            widget.propHeight().removePropertyListener(position_listener);
        }
    }
}
