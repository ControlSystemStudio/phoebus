/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget property with Integer value.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class IntegerWidgetProperty extends MacroizedWidgetProperty<Integer>
{
    final private Integer min, max;

    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public IntegerWidgetProperty(
            final WidgetPropertyDescriptor<Integer> descriptor,
            final Widget widget,
            final Integer default_value)
    {
        this(descriptor, widget, default_value,
             Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     *  @param min Minimum value
     *  @param max Maximum value
     */
    public IntegerWidgetProperty(
            final WidgetPropertyDescriptor<Integer> descriptor,
            final Widget widget,
            final Integer default_value,
            final Integer min, final Integer max)
    {
        super(descriptor, widget, default_value);
        this.min = min;
        this.max = max;
        if (! restrictValue(default_value).equals(default_value))
            throw new IllegalArgumentException("Default value outside range");
    }

    @Override
    protected Integer parseExpandedSpecification(final String text) throws Exception
    {
        try
        {   // Should be integer..
            return Integer.valueOf(text);
        }
        catch (final NumberFormatException ex)
        {   // .. but also allow "1e9", strictly a double, then truncate
            try
            {
                return Double.valueOf(text).intValue();
            }
            catch (final NumberFormatException ex2)
            {
                throw new Exception("Integer property '" + getName() +
                                    "' has invalid value text '" + text + "'");
            }
        }
    }

    @Override
    protected Integer restrictValue(final Integer requested_value)
    {
        if (requested_value.compareTo(min) < 0)
            return min;
        if (requested_value.compareTo(max) > 0)
            return max;
        return requested_value;
    }

    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (value instanceof Integer)
            setValue((Integer)value);
        else if (value instanceof Number)
            setValue( ((Number)value).intValue());
        else if (value instanceof String)
            setValue(parseExpandedSpecification((String)value));
        else
            throw new IllegalArgumentException("Property '" + getName() +
                "' requires int, but received " + value.getClass().getName());
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {
        writer.writeCharacters(specification);
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        setSpecification(XMLUtil.getString(property_xml));
    }
}
