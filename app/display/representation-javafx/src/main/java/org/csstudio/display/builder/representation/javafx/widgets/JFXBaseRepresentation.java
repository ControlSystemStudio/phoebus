/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;
import org.csstudio.display.builder.representation.WidgetRepresentation;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

/** Base class for all JavaFX widget representations
 *  @param <JFX> JFX Widget
 *  @param <MW> Model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class JFXBaseRepresentation<JFX extends Node, MW extends Widget> extends WidgetRepresentation<Parent, Node, MW>
{
    /** JFX node (or root of sub scene graph) that represents the widget
     *  <p>Only accessed on the JFX thread
     */
    protected JFX jfx_node;

    private volatile WidgetProperty<Boolean> visible;

    private final DirtyFlag dirty_position = new DirtyFlag();
    private final UntypedWidgetPropertyListener positionChangedListener = this::positionChanged;

    private void addToParent(final Parent parent)
    {
        // Order JFX children same as model widgets within their container
        int index = -1;
        final Optional<Widget> container = model_widget.getParent();
        if (container.isPresent())
        {
            if (container.get() instanceof TabsWidget)
            {   // Locate model_widget inside one of the Tab's children
                final List<TabItemProperty> tabs = ((TabsWidget) container.get()).propTabs().getValue();
                for (TabItemProperty tab : tabs)
                {
                    final int i = tab.children().getValue().indexOf(model_widget);
                    if (i >= 0)
                    {
                        index = i;
                        break;
                    }
                }
            }
            else
                index = container.get().getProperty(ChildrenProperty.DESCRIPTOR).getValue().indexOf(model_widget);
        }

        final ObservableList<Node> children = JFXRepresentation.getChildren(parent);
        if (index < 0)
            children.add(jfx_node);
        else if (index <= children.size())
            children.add(index, jfx_node);
        else
        {
            // If one of the other sibling widgets cannot be represented,
            // the 'index' will be useless.
            logger.log(Level.WARNING, "Cannot represent " + model_widget + " at index " + index + " within parent, which has only " + children.size() + " nodes");
            children.add(jfx_node);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Parent createComponents(final Parent parent) throws Exception
    {
        jfx_node = createJFXNode();
        if (jfx_node != null)
        {
            // Perform initial update _before_ node is in the scene graph
            // to minimize calls up the parent tree about changed size etc.
            registerListeners();
            updateChanges();

            addToParent(parent);

            if (toolkit.isEditMode())
            {   // Any visible item can be 'clicked' to allow editor to 'select' it
                final EventHandler<MouseEvent> detect_click = event ->
                {
                    // Clicking with the primary (left) button selects the widget ..
                    if (event.isPrimaryButtonDown())
                    {
                        // .. but ignore the group widget when Alt key is held.
                        // This allows 'rubberbanding' within a group while Alt is held.
                        // Without Alt, a click within a group would select-click the group,
                        // consuming the event and preventing a any rubberband selection.
                        if (event.isAltDown()  &&
                            (model_widget instanceof GroupWidget ||
                             model_widget instanceof TabsWidget))
                        {
                            // System.out.println("Ignoring click in " + model_widget);
                            return;
                        }
                        event.consume();
                        toolkit.fireClick(model_widget, event.isShortcutDown());
                    }
                };
                if (isFilteringEditModeClicks())
                    jfx_node.addEventFilter(MouseEvent.MOUSE_PRESSED, detect_click);
                else
                    jfx_node.addEventHandler(MouseEvent.MOUSE_PRESSED, detect_click);
            }
            else
            {   // Runtime context menu
                jfx_node.setOnContextMenuRequested((event) ->
                {
                    event.consume();
                    toolkit.fireContextMenu(model_widget, (int)event.getScreenX(), (int)event.getScreenY());
                });

                configurePVNameDrag();
            }
        }
        return getChildParent(parent);
    }

    /** In edit mode, does this widget "filter" all mouse clicks
     *  and use that to select the widget,
     *  or does it only "handle" mouse clicks that way,
     *  while still allowing clicks to pass on to the 'body' of the widget?
     *  @return <code>true</code> to filter instead of handle mouse presses in edit mode
     */
    protected boolean isFilteringEditModeClicks()
    {
        return false;
    }

    /** By default, allow dragging a "pv_name" from the widget in runtime mode
     *
     *  <p>Derived classes can override, for example to disable
     *  in case the representation already handles dragging moves
     *  to for example operate a slider or pan something.
     */
    // Note: In RCP version, the D&D had to be handled in SWT,
    // and was implemented in the org.csstudio.display.builder.rcp.RuntimeViewPart.
    // Widget.isDragEnabled() was used to enable/disable.
    // Now the enablement is left to the representation,
    // and since only Ctrl-drag is supported,
    // only very few widget representations need to disable it.
    protected void configurePVNameDrag()
    {
        // If there is a "pv_name", allow dragging it out
        final Optional<WidgetProperty<String>> pv_name = model_widget.checkProperty(CommonWidgetProperties.propPVName);
        if (! pv_name.isPresent())
            return;

        jfx_node.addEventFilter(MouseEvent.DRAG_DETECTED, event ->
        {
            // Ignore drag unless Ctrl is held.
            // When plain drag starts a PV name move,
            // this prevents selecting content within a text field
            // via a mouse drag.
            // Ctrl-drag is thus required to start dragging a PV name.
            if (! event.isControlDown())
                return;

            final String pv = pv_name.get().getValue();
            if (! pv.isEmpty())
            {
                final Dragboard db = jfx_node.startDragAndDrop(TransferMode.COPY);
                final ClipboardContent content = new ClipboardContent();
                content.putString(pv);
                db.setContent(content);
            }
        });
    }

    // For what it's worth, in case the node eventually has a JFX contest menu:
    //
    // While functional on other platforms, a menu set via
    //    Control#setContextMenu(menu)
    // will not activate on Linux for a JFX scene inside FXCanvas/SWT.
    // Directly handling the context menu event works on all platforms,
    // plus allows attaching a menu to even a basic Node.
    //
    // jfx_node.setOnContextMenuRequested((event) ->
    // {
    //     event.consume();
    //     final ContextMenu menu = new ContextMenu();
    //     menu.getItems().add(new MenuItem("Demo"));
    //     menu.show(jfx_node, event.getScreenX(), event.getScreenY());
    // });

    /** Implementation needs to create the JavaFX node
     *  or node tree for the model widget.
     *
     *  @return (Primary) JavaFX node
     *  @throws Exception on error
     */
    abstract protected JFX createJFXNode() throws Exception;

    /** @param widget Widget
     *  @return JFX node used to represent the widget
     */
    public static Node getJFXNode(final Widget widget)
    {
        if (widget instanceof DisplayModel)
        {   // Display doesn't have a representation.
            // Use its container.
            final DisplayModel model = (DisplayModel) widget;
            return model.getUserData(Widget.USER_DATA_TOOLKIT_PARENT);
        }
        final JFXBaseRepresentation<Node, Widget> representation =
                widget.getUserData(Widget.USER_DATA_REPRESENTATION);
        if (representation == null)
            throw new NullPointerException("Missing representation for " + widget);
        return representation.jfx_node;
    }

    /** {@inheritDoc} */
    @Override
    public void dispose()
    {
        unregisterListeners();
        Objects.requireNonNull(jfx_node);
        final Parent parent = jfx_node.getParent();
        if (parent == null)
            logger.log(Level.WARNING, "Missing JFX parent for " + model_widget);
        else
            JFXRepresentation.getChildren(parent).remove(jfx_node);
        jfx_node = null;
    }

    /** Get parent that would be used for child-widgets.
     *
     *  <p>By default, the representation does not itself host
     *  child widgets, so the parent of this widget is used.
     *
     *  <p>Specific implementation can override to return an
     *  inner container which holds child widgets.
     *
     *  @param parent parent of this JavaFX representation
     *  @return Desired parent for child nodes
     */
    protected Parent getChildParent(final Parent parent)
    {
        return parent;
    }

    /** Register model widget listeners.
     *
     *  <p>Override must call base class
     */
    protected void registerListeners()
    {
        visible = model_widget.checkProperty(CommonWidgetProperties.propVisible).orElse(null);
        if (visible != null)
            visible.addUntypedPropertyListener(positionChangedListener);
        model_widget.propX().addUntypedPropertyListener(positionChangedListener);
        model_widget.propY().addUntypedPropertyListener(positionChangedListener);
        // Would like to also listen to positionWidth & height,
        // then call jfx_node.resizeRelocate(x, y, width, height),
        // but resizeRelocate tends to ignore the width & height on
        // several widgets (Rectangle), so have to call their
        // setWith() & setHeight() in specific representation.

        if (! toolkit.isEditMode())
            attachTooltip();
    }

    /** Unregister model widget listeners.
     *
     *  <p>Override must call base class
     */
    protected void unregisterListeners()
    {
        visible = model_widget.checkProperty(CommonWidgetProperties.propVisible).orElse(null);
        if (visible != null)
            visible.removePropertyListener(positionChangedListener);
        model_widget.propX().removePropertyListener(positionChangedListener);
        model_widget.propY().removePropertyListener(positionChangedListener);
        if ( !toolkit.isEditMode())
            detachTooltip();
    }

    /** Attach tool tip support
     *
     *  <p>By default, each widget that has a tool tip property
     *  will get TooltipSupport attached to the jfx_node,
     *  but derived classes can override in case the tool tip
     *  needs to be attached to some other sub-node.
     */
    protected void attachTooltip()
    {
        model_widget.checkProperty(CommonWidgetProperties.propTooltip)
                    .ifPresent(prop -> TooltipSupport.attach(jfx_node, prop));
    }

    /** Detach tool tip support */
    protected void detachTooltip ( )
    {
        TooltipSupport.detach(jfx_node);
    }

    private void positionChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_position.mark();
        toolkit.scheduleUpdate(this);
    }

    /** {@inheritDoc} */
    @Override
    public void updateChanges()
    {
        if (dirty_position.checkAndClear())
        {
            jfx_node.relocate(model_widget.propX().getValue(),
                              model_widget.propY().getValue());
            if (visible != null)
                jfx_node.setVisible(visible.getValue());
        }
    }

    @Override
    public void updateOrder()
    {
        final Parent parent = jfx_node.getParent();
        if (parent == null)
            logger.log(Level.WARNING, "Missing JFX parent for " + model_widget);
        else
        {
            // Cannot use Collections.swap() because it results in an IllegalArgumentException: Children: duplicate children added
            JFXRepresentation.getChildren(parent).remove(jfx_node);
            addToParent(parent);
        }
    }
}
