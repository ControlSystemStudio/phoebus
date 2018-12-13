/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetFactory;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.ArrayWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

@SuppressWarnings("nls")
public class ArrayRepresentation extends JFXBaseRepresentation<Pane, ArrayWidget>
{
    private final DirtyFlag dirty_number = new DirtyFlag(); //number of element widgets
    private final DirtyFlag dirty_look = new DirtyFlag(); //size/color of JavaFX Node
    private final WidgetPropertyListener<List<Widget>> childrenChangedListener = this::childrenChanged;
    private final WidgetPropertyListener<Integer> sizeChangedListener = this::sizeChanged;
    private final WidgetPropertyListener<WidgetColor> colorChangedListener = this::colorChanged;

    private static final int inset = 10;

    // XXX Simplify handling of children
    // Can 'children' be replaced by calls to runtimeChildren()?
    // As array size changes, copyProperties() is called multiple times
    // because widgets are added to 'children' and then runtimeChildren() is updated,
    // triggering its listener.
    private final CopyOnWriteArrayList<Widget> children = new CopyOnWriteArrayList<>();
    private volatile int numChildren = 0, width = 0, height = 0;
    private volatile boolean isArranging = false, isAddingRemoving = false;
    private volatile Widget master = null;
    private Pane inner_pane;

    @Override
    protected Pane createJFXNode() throws Exception
    {
        model_widget.runtimePropInsets().setValue(new int[] { inset, inset });
        inner_pane = new Pane();
        inner_pane.relocate(inset, inset);
        height = model_widget.propHeight().getValue();
        width = model_widget.propWidth().getValue();
        return new Pane(inner_pane);
    }

    @Override
    protected Parent getChildParent(final Parent parent)
    {
        return inner_pane;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.runtimeChildren().addPropertyListener(childrenChangedListener);
        model_widget.propHeight().addPropertyListener(sizeChangedListener);
        model_widget.propWidth().addPropertyListener(sizeChangedListener);
        model_widget.propForegroundColor().addPropertyListener(colorChangedListener);
        model_widget.displayBackgroundColor().addPropertyListener(colorChangedListener);

        childrenChanged(null, null, model_widget.runtimeChildren().getValue());
        adjustNumberByLength();
    }

    @Override
    protected void unregisterListeners()
    {
        childrenChanged(null, model_widget.runtimeChildren().getValue(), null);
        model_widget.runtimeChildren().removePropertyListener(childrenChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propForegroundColor().removePropertyListener(colorChangedListener);
        model_widget.displayBackgroundColor().removePropertyListener(colorChangedListener);
        super.unregisterListeners();
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_number.checkAndClear())
        {
            final int diff = children.size() - numChildren;
            if (diff != 0)
            {
                isAddingRemoving = true;
                if (diff > 0)
                    removeChildren(children, diff);
                else
                    addChildren(children, -diff);
                isAddingRemoving = false;
            }
            arrangeChildren();
        }
        if (dirty_look.checkAndClear())
        {
            if (height > 0)
                jfx_node.setPrefHeight(height);
            if (width > 0)
                jfx_node.setPrefWidth(width);
            Color color = JFXUtil.convert(model_widget.propForegroundColor().getValue());
            jfx_node.setBorder(new Border(
                    new BorderStroke(color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT,
                            new Insets(inset / 2))));
            color = JFXUtil.convert(model_widget.displayBackgroundColor().getValue());
            jfx_node.setBackground(new Background(new BackgroundFill(color, null, null)));
        }
    }

    private void colorChanged(final WidgetProperty<WidgetColor> property, final WidgetColor old_value, final WidgetColor new_value)
    {
        dirty_look.mark();
        toolkit.scheduleUpdate(this);
    }

    private void sizeChanged(final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value)
    {
        if (!isArranging && (old_value != new_value || old_value == null))
        {
            width = model_widget.propWidth().getValue();
            height = model_widget.propHeight().getValue();
            adjustNumberByLength();

            dirty_look.mark();
            toolkit.scheduleUpdate(this);
        } else
            isArranging = false;
    }

    private void childrenChanged(final WidgetProperty<List<Widget>> property, final List<Widget> removed,
                                 final List<Widget> added)
    {
        final List<Widget> newval = new ArrayList<>(model_widget.runtimeChildren().getValue());
        if (!isAddingRemoving)
        {
            numChildren = newval.size();
            dirty_number.mark();
            toolkit.scheduleUpdate(this);
        }
        if (added != null)
        {
            for (Widget widget : added)
                addChildListeners(widget);
            //trigger child (element) listeners to copy properties of existing widgets
            if (!added.isEmpty()) //implies !newval.isEmpty()
            {
                Widget widget = added.get(0);
                copyProperties(newval.get(0), widget);
                master = widget;
            }
        }
        else //removed != null
            for (Widget widget : removed)
                removeChildListeners(widget);
        children.clear();
        children.addAll(newval);
    }

    private void addChildListeners(final Widget widget)
    {
        for (WidgetProperty<?> prop : widget.getProperties())
        {
            // Resize array when child widget changes size
            if (prop.getName().equals(CommonWidgetProperties.propWidth.getName())  ||
                prop.getName().equals(CommonWidgetProperties.propHeight.getName()) ||
                prop.getName().equals(CommonWidgetProperties.propHorizontal.getName())
               )
               prop.addUntypedPropertyListener(rearrange);
            else
                // Skip runtime props (current value..)
                // as well as widget name, type, PV name
                if (prop.getCategory() != WidgetPropertyCategory.RUNTIME  &&
                    !prop.getName().equals(CommonWidgetProperties.propType.getName()) &&
                    !prop.getName().equals(CommonWidgetProperties.propName.getName()) &&
                    !prop.getName().equals(CommonWidgetProperties.propPVName.getName())
                   )
            {
                logger.fine("Array widget adding listener to " + widget + " " + prop);
                prop.addUntypedPropertyListener(child_property_listener);
            }
        }
    }

    private void removeChildListeners(final Widget widget)
    {
        for (WidgetProperty<?> prop : widget.getProperties())
        {
            // Resize array when child widget changes size
            if (prop.getName().equals(CommonWidgetProperties.propWidth.getName())  ||
                prop.getName().equals(CommonWidgetProperties.propHeight.getName()) ||
                prop.getName().equals(CommonWidgetProperties.propHorizontal.getName())
               )
               prop.removePropertyListener(rearrange);
            else
                // Skip runtime props (current value..)
                // as well as widget name, type, PV name
                if (prop.getCategory() != WidgetPropertyCategory.RUNTIME  &&
                    !prop.getName().equals(CommonWidgetProperties.propType.getName()) &&
                    !prop.getName().equals(CommonWidgetProperties.propName.getName()) &&
                    !prop.getName().equals(CommonWidgetProperties.propPVName.getName())
                   )
            {
                logger.fine("Array widget removing listener from " + widget + " " + prop);
                prop.removePropertyListener(child_property_listener);
            }
        }
    }

    /** When property of one child widget is changed,
     *  update all other child widgets the same way
     */
    private final UntypedWidgetPropertyListener child_property_listener = (p, o, n) ->
    {
        logger.fine("Array child widget listener called: " + p.getWidget() + " " + p);
        if (!isArranging)
        {
            final String name = p.getName();
            final Object value = p.getValue();
            for (Widget w : children)
            {
                if (w.equals(p.getWidget()))
                    continue;
                try
                {
                    w.setPropertyValue(name, value);
                }
                catch (Exception ignored)
                {
                }
            }
        }
    };

    /** If a child widget changes size, update the array widget layout */
    private final UntypedWidgetPropertyListener rearrange = (p, o, n) ->
    {
        if (!isArranging)
        {
            master = p.getWidget();
            dirty_number.mark(); //dirty_number calls arrangeChildren
            toolkit.scheduleUpdate(this);
        }
    };

    /** By default, stack array element widgets vertically:
     *  <pre>
     *   #
     *   #
     *   #
     *  </pre>
     *  If the child widget has a "horizontal" preference and wants to be horizontal,
     *  also stack vertically:
     *  <pre>
     *   ======
     *   ======
     *   ======
     *  </pre>
     *  But if the child widget prefers to be vertical (!horizontal),
     *  stack them side by side:
     *  <pre>
     *   | | |
     *   | | |
     *   | | |
     *  </pre>
     *  @param element Element widget to use for test
     *  @return Stack array element widgets vertically?
     */
    private boolean testVerticalStacking(final Widget element)
    {
        final Optional<WidgetProperty<Boolean>> horizontal = element.checkProperty("horizontal");
        if (horizontal.isPresent())
            return horizontal.get().getValue();
        return true;
    }

    private void arrangeChildren()
    {
        List<Widget> children = new ArrayList<>(model_widget.runtimeChildren().getValue());
        Widget master = this.master;
        if (children.isEmpty())
            return;
        if (master == null)
            master = children.get(0);

        isArranging = true;
        numChildren = children.size();

        final boolean vertical = testVerticalStacking(master);
        final int h = vertical ? master.propHeight().getValue()
                               : (height = model_widget.propHeight().getValue()) - inset * 2;
        final int w = vertical ? (width = model_widget.propWidth().getValue()) - inset * 2
                               : master.propWidth().getValue();
        int len = 0;
        for (Widget child : children)
        {
            child.propHeight().setValue(h);
            child.propWidth().setValue(w);
            child.propX().setValue(vertical ? 0 : len);
            child.propY().setValue(vertical ? len : 0);
            len += vertical ? h : w;
        }
        len += inset * 2;
        if (vertical && len != height)
            model_widget.propHeight().setValue(height = len);
        else if (!vertical && len != width)
            model_widget.propWidth().setValue(width = len);
        else
            isArranging = false;
        dirty_look.mark();
        toolkit.scheduleUpdate(this);
    }

    private void adjustNumberByLength()
    {
        final List<Widget> children = new ArrayList<>(model_widget.runtimeChildren().getValue());
        if (children.isEmpty())
            return;
        final boolean vertical = testVerticalStacking(children.get(0));
        final int l = vertical ? children.get(0).propHeight().getValue()
                               : children.get(0).propWidth().getValue();
        numChildren = vertical ? (height - inset * 2) / l
                               : (width - inset * 2) / l;
        dirty_number.mark();
        toolkit.scheduleUpdate(this);
    }

    /** Add more per-element child widgets
     *  @param children Child widgets, must contain at least element [0] for prototype
     *  @param number Number to add
     */
    private void addChildren(final List<Widget> children, final int number)
    {
        if (children.isEmpty())
        {
            logger.log(Level.WARNING, "Cannot add array elements, no prototype widget");
            return;
        }
        for (int i=0; i<number; ++i)
        {
            final Widget child = copyWidget(children.get(0));
            child.propName().setValue(model_widget.getName() + "-" + child.getType() + "-" + this.children.size());
            model_widget.runtimeChildren().addChild(child);
        }
    }

    /** Remove per-element child widgets
     *  @param children Child widgets, will retain at least one entry for prototype
     *  @param number Number to remove
     */
    private void removeChildren(final List<Widget> children, int number)
    {
        for (int i=0; i<number; ++i)
        {   // Leave the prototype
            if (children.size() == 1)
                return;
            final Widget child = children.remove(children.size() - 1);
            model_widget.runtimeChildren().removeChild(child);
        }
    }

    private Widget copyWidget(final Widget original)
    {
        try
        {
            final Widget copy = WidgetFactory.getInstance().getWidgetDescriptor(original.getType()).createWidget();
            copyProperties(original, copy);
            return copy;
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot copy " + original, ex);
        }
        return null;
    }

    private void copyProperties(final Widget original, final Widget copy)
    {
        if (original.equals(copy))
            return;

        // Copy (most) properties onto matching name in copy
        for (WidgetProperty<?> prop : copy.getProperties())
        {
            // Don't change the 'name' property
            if (prop.getName().equals(original.propName().getName()))
                continue;

            // Don't copy the 'children' of a GroupWidget
            if (prop.getName().equals(ChildrenProperty.DESCRIPTOR.getName()))
                continue;

            try
            {
                final String prop_name = prop.getName();
                prop.setValue(original.getPropertyValue(prop_name));
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot copy " + original + " " + prop, ex);
            }
        }

        // For a Group widget, copy the child widgets
        if (original instanceof GroupWidget)
        {
            final GroupWidget orig_group = (GroupWidget) original;
            final GroupWidget copy_group = (GroupWidget) copy;

            // Remove existing child elements
            while (copy_group.runtimeChildren().getValue().size() > 0)
                copy_group.runtimeChildren().removeChild(copy_group.runtimeChildren().getValue().get(0));
            // Set child elements from original group
            for (Widget child : orig_group.runtimeChildren().getValue())
                copy_group.runtimeChildren().addChild(copyWidget(child));
        }
    }
}
