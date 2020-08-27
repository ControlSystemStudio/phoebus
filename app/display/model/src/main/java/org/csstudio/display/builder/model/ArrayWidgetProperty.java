/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Widget property that contains array of widget properties.
 *
 *  <p>Individual elements are properties which can be modified as usual,
 *  unless they are themselves read-only.
 *  The overall list as returned by <code>getValue</code> is read-only,
 *  because direct modification of the underlying list would not allow
 *  this property to send change events.
 *  To add/remove elements, this property has dedicated <code>addElement</code>,
 *  <code>removeElement</code> API.
 *
 *  @author Kay Kasemir
 *  @param <WPE> WidgetProperty used for each array element
 */
@SuppressWarnings("nls")
public class ArrayWidgetProperty<WPE extends WidgetProperty<?>> extends WidgetProperty<List<WPE>>
{
    /** Factory that creates a new array element.
     *
     *  <p>When reading persisted data or when adding a new array element
     *  with default value, this factory is used to create that element.
     *
     *  <p>Implementation may always create an element with the same value,
     *  or base that value on the array index.
     */
    @FunctionalInterface
    public static interface ElementFactory<WPE>
    {
        /** Create a new array element
         *  @param widget Widget that contains the property
         *  @param index Index of the new array element
         *  @return Array element
         */
        WPE newElement(Widget widget, int index);
    }

    /** Descriptor of an array property */
    public static class Descriptor<WPE extends WidgetProperty<?>> extends WidgetPropertyDescriptor<List<WPE>>
    {
        final private ElementFactory<WPE> factory;
        final private int minimum_size;

        public Descriptor(final WidgetPropertyCategory category,
                          final String name, final String description,
                          final ElementFactory<WPE> factory)
        {
            this(category, name, description, factory, 1);
        }

        public Descriptor(final WidgetPropertyCategory category,
                final String name, final String description,
                final ElementFactory<WPE> factory,
                final int minimum_size)
        {
          super(category, name, description);
          this.factory = factory;
          this.minimum_size = minimum_size;
        }

        @Override
        public ArrayWidgetProperty<WPE> createProperty(
                final Widget widget, final List<WPE> elements)
        {
            return new ArrayWidgetProperty<>(this, widget, elements);
        }
    };

    protected ArrayWidgetProperty(final Descriptor<WPE> descriptor,
            final Widget widget, final List<WPE> elements)
    {
        // Default's elements can be changed, they each track their own 'default',
        // but overall default_value List<> is not modifiable
        super(descriptor, widget, Collections.unmodifiableList(elements));
        value = new CopyOnWriteArrayList<>(elements);
    }

    @Override
    public boolean isUsingWidgetClass()
    {   // Array uses class if any elements use it
        for (WidgetProperty<?> element : value)
            if (element.isUsingWidgetClass())
                return true;
        return false;
    }

    @Override
    public boolean isDefaultValue()
    {
        // Array has 'default' value if it contains
        // only the original default element.
        // Otherwise, all elements need to be written
        return value.size() == 1  &&
               value.get(0).isDefaultValue();
    }

    @Override
    protected List<WPE> restrictValue(final List<WPE> requested_value)
    {
        if (requested_value instanceof CopyOnWriteArrayList)
            return requested_value;
        return new CopyOnWriteArrayList<>(requested_value);
    }

    /** @return List<> of current array elements. List is not modifiable (elements, however, are).
     *  @see #addElement(WidgetProperty)
     *  @see #removeElement()
     */
    @Override
    public List<WPE> getValue()
    {
        return Collections.unmodifiableList(value);
    }

    /** @return Element count */
    public int size()
    {
        return value.size();
    }

    /** @return Minimum array size */
    public int getMinimumSize()
    {
        return ((Descriptor<WPE>)descriptor).minimum_size;
    }

    /** Access element
     *  @param index Element index, 0 .. (<code>getValue().size()</code>-1)
     *  @return Element of array
     *  @throws IndexOutOfBoundsException
     */
    public WPE getElement(final int index)
    {
        try
        {
            return value.get(index);
        }
        catch (IndexOutOfBoundsException ex)
        {
            throw new IndexOutOfBoundsException("Cannot get element " + index + " of " +
                                                getWidget().getName() + "." + getName() +
                                                " (Size: " + value.size() + ")");
        }
    }

    /** Remove the last element
     *  @return Removed element
     *  @throws IndexOutOfBoundsException if list is empty
     */
    public WPE removeElement()
    {   // Not fully thread safe. Two concurrent callers would each check, then remove the last element.
        if (value.size()-1 < getMinimumSize())
            throw new IndexOutOfBoundsException("Minimum list size is " + getMinimumSize());
        final WPE removed = value.remove(value.size()-1);
        firePropertyChange(Arrays.asList(removed), null);
        return removed;
    }

    /** @param element Element to add to end of list */
    public void addElement(final WPE element)
    {
        value.add(element);
        firePropertyChange(null, Arrays.asList(element));
    }

    /** Add new element to end of list
     *  @return Element that was created at end of list
     */
    public WPE addElement()
    {
        final WPE element =
                ((Descriptor<WPE>)descriptor).factory.newElement(widget, value.size());
        addElement(element);
        return element;
    }

    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (! (value instanceof Collection))
            throw new Exception("Need list of array items");

        try
        {
            final Collection<?> items = (Collection<?>) value;
            final int N = items.size();
            while (size() > N)
                removeElement();
            while (size() < N)
                addElement();

            final Iterator<?> iter = items.iterator();
            for (int i=0; i<N; ++i)
            {
                final WPE element = getElement(i);
                final Object el_value = iter.next();
                if (element  instanceof MacroizedWidgetProperty<?>  &&
                    el_value instanceof MacroizedWidgetProperty<?>)
                {
                    ((MacroizedWidgetProperty<?>)element).setSpecification(((MacroizedWidgetProperty<?>) el_value).getSpecification());
                }
                else
                    element.setValueFromObject(el_value);
            }

            // Notify listeners of the whole array
            firePropertyChange(this, null, this.value);
        }
        catch (Throwable ex)
        {
            throw new Exception("Cannot assign " + getName() + " from " + value, ex);
        }
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {   // Must always write each array element, even default,
        // because otherwise an array with "new value, default, default, other value"
        // would change from 4 into 2 elements
        for (WPE element : value)
            model_writer.writeProperty(element);
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        // Loop over XML child elements.
        // The element names are unknown at this time, only once we create an element
        // could we get its name...
        final List<WPE> elements = new ArrayList<>();
        Node child = property_xml.getFirstChild();
        while (child != null)
        {
            if (child.getNodeType() == Node.ELEMENT_NODE)
            {
                final Element child_xml = (Element) child;
                final WPE element =
                    ((Descriptor<WPE>)descriptor).factory.newElement(widget, elements.size());
                try
                {
                    element.readFromXML(model_reader, child_xml);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Error reading " + getName() + " element " + element.getName(), ex);
                }
                elements.add(element);
            }
            child = child.getNextSibling();
        }
        setValue(elements);
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder("'" + getName() + "' = [ ");
        boolean first = true;
        for (WPE element : value)
        {
            if (first)
                first = false;
            else
                buf.append(", ");
            buf.append(element);
        }
        buf.append(" ]");
        return buf.toString();
    }
}
