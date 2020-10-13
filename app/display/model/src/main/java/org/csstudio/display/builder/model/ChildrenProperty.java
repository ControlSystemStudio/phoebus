/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;
import org.w3c.dom.Element;

/** Widget property that holds list of child widgets.
 *
 *  <p>A 'ContainerWidget' is a widget that has this 'children' property.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ChildrenProperty extends RuntimeWidgetProperty<List<Widget>>
{
    // 'value' is a thread-safe CopyOnWriteArrayList
    // that's effectively final because it's assigned in the constructor
    // and never changed.
    // addChild/removeChild checks atomically for duplicates/presence

    /** 'children' is a property to allow notifications,
     *  but setting its value or creating additional property instances
     *  is not supported.
     *
     *  <p>All access must be via the ContainerWidget.add/removeChild() methods.
     *
     *  <p>Notifications are sent with a list of elements added or removed,
     *  <u>not</u> the complete old resp. new value.
     */
    public static final WidgetPropertyDescriptor<List<Widget>> DESCRIPTOR =
            new WidgetPropertyDescriptor<>(
                    WidgetPropertyCategory.RUNTIME, "children", "Child widgets")
    {
        @Override
        public WidgetProperty<List<Widget>> createProperty(final Widget widget,
                final List<Widget> ignored)
        {
            throw new UnsupportedOperationException("Only created by ChildrenProperty constructor");
        }
    };

    /** Check if widget is a 'container' by fetching its children
     *  @param widget Widget
     *  @return {@link ChildrenProperty} or <code>null</code> if widget is not a container
     */
    public static ChildrenProperty getChildren(final Widget widget)
    {
        final Optional<WidgetProperty<List<Widget>>> children = widget.checkProperty(DESCRIPTOR);
        if (children.isPresent())
            return (ChildrenProperty) children.get();
        return null;
    }

    /** Get the 'children' list of a widget's parent.
     *
     *  <p>This is either the children list of a plain parent,
     *  or the children list inside a TabWidget's tab.
     *
     *  @param widget Widget where parent (plain or TabWidget) is checked for
     *  @return {@link ChildrenProperty} that contains the widget
     *  @throws NoSuchElementException if widget has no parent
     *  @throws IllegalArgumentException If no tab of parent {@link TabWidget} contains the widget,
     *  @throws IllegalStateException If parent has no children
     */
    public static ChildrenProperty getParentsChildren(final Widget widget)
    {
        final Widget parent = widget.getParent().get();
        if (parent instanceof TabsWidget)
        {
            final List<TabItemProperty> tabs = ((TabsWidget)parent).propTabs().getValue();
            for (TabItemProperty tab : tabs)
            {
                final ChildrenProperty children = tab.children();
                if (children.getValue().contains(widget))
                    return children;
            }
            throw new IllegalArgumentException("Cannot locate " + widget + " in " + parent);
        }

        final Optional<WidgetProperty<List<Widget>>> children = parent.checkProperty(DESCRIPTOR);
        if (children.isPresent())
            return (ChildrenProperty) children.get();
        throw new IllegalStateException("Parent of " + widget + " has no 'children': " + parent);
    }

    public ChildrenProperty(final Widget widget)
    {
        super(DESCRIPTOR, widget, Collections.emptyList());
        value = new CopyOnWriteArrayList<>();
    }

    @Override
    public List<Widget> getValue()
    {   // Override normal access to value, only provide read-only version of list
        return Collections.unmodifiableList(super.getValue());
    }

    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        throw new UnsupportedOperationException("Use ChildrenProperty#addChild()/removeChild()");
    }

    @Override
    public void setValue(final List<Widget> new_value)
    {
        final List<Widget> old;
        // Atomically replace all elements of 'value' with new value
        synchronized (value)
        {
            old = new ArrayList<>(value.size());
            old.addAll(value);
            value.clear();
            value.addAll(new_value);
        }
        firePropertyChange(old, new_value);
    }

    /** Locate a child widget by name
     *
     *  <p>Recurses through all child widgets,
     *  including groups and sub-groups.
     *
     *  @param name Name of widget
     *  @return First widget with given name or <code>null</code>
     */
    public Widget getChildByName(final String name)
    {
        // Could back this with a Map<String, Widget>,
        // but note that there can be duplicates:
        // addChild(WidgetNamedFred);
        // addChild(AnotherWidgetNamedFred);
        // removeChild(AnotherWidgetNamedFred);
        // -> Must still find the first WidgetNamedFred,
        //    and thus need  Map<String, List<Widget>>
        // Update that map in addChild() and removeChild()
        for (final Widget child : value)
        {
            if (child.getName().equals(name))
                return child;
            if (child instanceof TabsWidget)
            {
                for (TabItemProperty tab : ((TabsWidget)child).propTabs().getValue())
                {
                    final Widget maybe = tab.children().getChildByName(name);
                    if (maybe != null)
                        return maybe;
                }
            }
            else
            {
                final ChildrenProperty grandkids = getChildren(child);
                if (grandkids != null)
                {
                    final Widget maybe = grandkids.getChildByName(name);
                    if (maybe != null)
                        return maybe;
                }
            }
        }
        return null;
    }

    /** @param index Index where to add child, or -1 to append at end
     *  @param child Widget to add as child
     */
    public void addChild(final int index, final Widget child)
    {
        if (child == null)
            throw new NullPointerException("Cannot add null to " + getWidget());
        final List<Widget> list = value;
        synchronized (list)
        {   // Atomically check-then-add
            if (list.contains(child))
                throw new IllegalArgumentException(this +
                        " already has child widget " + child);
            if (index < 0)
                list.add(child);
            else
                list.add(index, child);
        }
        child.setParent(getWidget());
        firePropertyChange(null, Arrays.asList(child));
    }

    /** @param child Widget to add as child */
    public void addChild(final Widget child)
    {
        addChild(-1, child);
    }

    /** @param child Widget to remove as child
     *  @return Index of removed child in list of children
     */
    public int removeChild(final Widget child)
    {
        if (child == null)
            throw new NullPointerException("Cannot remove null from " + getWidget());
        final List<Widget> list = value;
        final int index;
        synchronized (list)
        {
            index = list.indexOf(child);
            if (index < 0)
                throw new IllegalArgumentException("Widget hierarchy error: " + child + " is not known to " + this);
            list.remove(index);
        }
        child.setParent(null);
        firePropertyChange(Arrays.asList(child), null);
        return index;
    }

    public int moveChildTo(int index, final Widget child)
    {
        if (child == null)
            throw new NullPointerException("Cannot move null in " + getWidget());
        final List<Widget> list = value;
        final int current_index;
        synchronized (list)
        {
            current_index = list.indexOf(child);
            if (current_index < 0)
                throw new IllegalArgumentException("Widget hierarchy error: " + child + " is not known to " + this);
            if (index < 0)
                index = list.size() - 1;

            final int index_diff = index - current_index;
            if (index_diff == 1 || index_diff == -1)
            {
                // Move up/down by 1 step
                Collections.swap(list, current_index, index);
            }
            else
            {
                // Move to front/back
                Collections.rotate(list.subList(java.lang.Math.min(index, current_index), java.lang.Math.max(index, current_index) + 1), index_diff);
            }
        }
        final List change = Arrays.asList(child);
        firePropertyChange(change, change, true);
        return index;
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {
        model_writer.writeWidgets(getValue());
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        model_reader.readWidgets(this, property_xml);
    }

    /** Dispose, i.e. clear list
     *
     *  <p>Prevents further use by replacing it with immutable, empty list
     */
    void dispose()
    {
        value = Collections.emptyList();
    }
}
