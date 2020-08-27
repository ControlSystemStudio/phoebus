/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static java.lang.Boolean.parseBoolean;
import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.List;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Structured widget property, contains several basic widget properties.
 *
 *  <p>Persisted based on the name of each structure element
 *  to allow for adding (or removing) structure elements
 *  when creating a new version of a widget.
 *
 *  <p>The individual elements can be set unless they are read-only.
 *  The structure will be read-only if all its elements are read-only.
 *
 *  <p>The overall structure cannot be redefined,
 *  i.e. it is not permitted to set a new 'value'
 *  to the structure.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StructuredWidgetProperty extends WidgetProperty<List<WidgetProperty<?>>>
{
    /** Descriptor of a structured property */
    public static class Descriptor extends WidgetPropertyDescriptor<List<WidgetProperty<?>>>
    {
        public Descriptor(final WidgetPropertyCategory category,
                          final String name, final String description)
        {
            super(category, name, description);
        }

        @Override
        public StructuredWidgetProperty createProperty(
                final Widget widget, final List<WidgetProperty<?>> elements)
        {
            return new StructuredWidgetProperty(this, widget, elements);
        }
    };

    protected StructuredWidgetProperty(final Descriptor descriptor,
            final Widget widget, final List<WidgetProperty<?>> elements)
    {
        super(descriptor, widget, elements);
    }

    /** @return <code>true</code> if any element is using class support */
    @Override
    public boolean isUsingWidgetClass()
    {
        for (WidgetProperty<?> element : value)
            if (element.isUsingWidgetClass())
                return true;
        return false;
    }

    /** @return <code>true</code> if all elements have default value */
    @Override
    public boolean isDefaultValue()
    {
        for (WidgetProperty<?> element : value)
            if (! element.isDefaultValue())
                return false;
        return true;
    }

    /** @return <code>true</code> if all elements are read-only */
    @Override
    public boolean isReadonly()
    {
        for (WidgetProperty<?> element : value)
            if (! element.isReadonly())
                return false;
        return true;
    }

    /** @return Number of structure elements */
    public int size()
    {
        return value.size();
    }

    /** Access element as known type
     *
     *  @param index Element index, 0 .. (<code>getValue().size()</code>-1)
     *  @return Widget property cast to receiving type
     */
    // Not perfect: Caller needs to know the type.
    // Still, at least there's a runtime error when attempting to cast to the wrong type,
    // and since the structure cannot change, this is almost as good as a compile time check.
    @SuppressWarnings("unchecked")
    public <TYPE> WidgetProperty<TYPE> getElement(final int index)
    {
        return (WidgetProperty<TYPE>) value.get(index);
    }

    /** Access element as known type
     *
     *  @param element_name Element name
     *  @return Widget property cast to receiving type
     *  @throws IllegalArgumentException if element name is not found in structure
     */
    @SuppressWarnings("unchecked")
    public <TYPE> WidgetProperty<TYPE> getElement(final String element_name)
    {
        for (WidgetProperty<?> element : value)
            if (element.getName().equals(element_name))
                return (WidgetProperty<TYPE>) element;
        throw new IllegalArgumentException("Structure has no element named " + element_name);
    }

    @Override
    public void setValue(final List<WidgetProperty<?>> value)
    {
        throw new IllegalAccessError("Elements of structure " + getName() + " cannot be re-defined");
    }

    @Override
    public void setValueFromObject(final Object new_value) throws Exception
    {
        if (new_value instanceof List)
        {   // Allow assignment of another structure's value, i.e. list with same elements
            final List<?> new_elements = (List<?>)new_value;
            if (new_elements.size() != value.size())
                throw new Exception("Elements of structure " + getName() + " must provide " + value.size() + " elements, got " + new_elements.size());
            for (int i=0; i<value.size(); ++i)
            {
                final WidgetProperty<?> element = value.get(i);
                final Object new_object = new_elements.get(i);
                if (element.getClass() != new_object.getClass())
                    throw new Exception("Cannot set structure " + getName() + "." + element.getName() + " to " + new_object);

                final WidgetProperty<?> new_element = (WidgetProperty<?>)new_object;
                if (element.getName() != new_element.getName())
                    throw new Exception("Cannot set structure " + getName() + "." + element.getName() + " to " + new_element.getName());
                try
                {
                    element.setValueFromObject(new_element.getValue());
                }
                catch (Exception ex)
                {
                    throw new Exception("Cannot set structure " + getName() + "." + element.getName() + " to " + new_element, ex);
                }
            }
            // Notify listeners of the whole array
            firePropertyChange(this, null, this.value);
        }
        else
            throw new Exception("Elements of structure " + getName() + " cannot be assigned from " + new_value);
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {
        for (WidgetProperty<?> element : value)
        {   // In general, don't persist runtime properties, except for 'children'
            // as used by the TabsWidget.TabItemProperty
            if ( element.getCategory() != WidgetPropertyCategory.RUNTIME  ||
                 element.getName().equals(ChildrenProperty.DESCRIPTOR.getName()) )
                model_writer.writeProperty(element);
        }
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        for (WidgetProperty<?> element : value)
        {
            final Element xml = XMLUtil.getChildElement(property_xml, element.getName());
            if (xml == null)
                continue;
            try
            {
                element.readFromXML(model_reader, xml);
                element.useWidgetClass(parseBoolean(xml.getAttribute(XMLTags.USE_CLASS)));
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error reading " + getName() + " element " + element.getName(), ex);
            }
        }
        // Notify listeners of the whole array
        firePropertyChange(this, null, this.value);
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder("'" + getName() + "' = { ");
        boolean first = true;
        for (WidgetProperty<?> element : value)
        {
            if (first)
                first = false;
            else
                buf.append(", ");
            buf.append(element);
        }
        buf.append(" }");
        return buf.toString();
    }
}
