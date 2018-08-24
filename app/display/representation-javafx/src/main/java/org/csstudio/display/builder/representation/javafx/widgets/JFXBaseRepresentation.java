/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;
import org.csstudio.display.builder.representation.WidgetRepresentation;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;

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

            if (index < 0)
                JFXRepresentation.getChildren(parent).add(jfx_node);
            else
                JFXRepresentation.getChildren(parent).add(index, jfx_node);

            if (toolkit.isEditMode())
            {   // Any visible item can be 'clicked' to allow editor to 'select' it
                final EventHandler<MouseEvent> detect_click = event ->
                {
                    if (event.isPrimaryButtonDown())
                    {
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

                // If there is a "pv_name", allow dragging it out
                final Optional<WidgetProperty<String>> pv_name = model_widget.checkProperty(CommonWidgetProperties.propPVName);
                if (pv_name.isPresent())
                    jfx_node.setOnDragDetected(event ->
                    {
                        final String pv = pv_name.get().getValue();
                        if (! pv.isEmpty())
                        {
                            final Dragboard db = jfx_node.startDragAndDrop(TransferMode.COPY);
                            final ClipboardContent content = new ClipboardContent();
                            content.putString(pv);
                            db.setContent(content);
                        }
                        event.consume();
                    });
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
        Objects.requireNonNull(jfx_node);
        JFXRepresentation.getChildren(jfx_node.getParent()).remove(jfx_node);
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
            visible.addUntypedPropertyListener(this::positionChanged);
        model_widget.propX().addUntypedPropertyListener(this::positionChanged);
        model_widget.propY().addUntypedPropertyListener(this::positionChanged);
        // Would like to also listen to positionWidth & height,
        // then call jfx_node.resizeRelocate(x, y, width, height),
        // but resizeRelocate tends to ignore the width & height on
        // several widgets (Rectangle), so have to call their
        // setWith() & setHeight() in specific representation.

        if (! toolkit.isEditMode())
            attachTooltip();
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
}
