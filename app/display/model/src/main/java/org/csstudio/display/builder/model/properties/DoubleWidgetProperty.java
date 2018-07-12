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

/** Widget property with Double value.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DoubleWidgetProperty extends MacroizedWidgetProperty<Double>
{
    final private Double min, max;

    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public DoubleWidgetProperty(
            final WidgetPropertyDescriptor<Double> descriptor,
            final Widget widget,
            final Double default_value)
    {
        this(descriptor, widget, default_value,
             Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     *  @param min Minimum value
     *  @param max Maximum value
     */
    public DoubleWidgetProperty(
            final WidgetPropertyDescriptor<Double> descriptor,
            final Widget widget,
            final Double default_value,
            final Double min, final Double max)
    {
        super(descriptor, widget, default_value);
        this.min = min;
        this.max = max;
        if (! restrictValue(default_value).equals(default_value))
            throw new IllegalArgumentException("Default value outside range");
    }

    @Override
    protected Double parseExpandedSpecification(final String text) throws Exception
    {
        try
        {
            return Double.valueOf(text);
        }
        catch (final NumberFormatException ex)
        {
            throw new Exception("Double property '" + getName() +
                                "' has invalid value text '" + text + "'", ex);
        }
    }

    @Override
    protected Double restrictValue(final Double requested_value)
    {
        if (Double.isFinite(requested_value))
        {   // Only check limits for finite value. NaN is passed through.
            if (requested_value.compareTo(min) < 0)
                return min;
            if (requested_value.compareTo(max) > 0)
                return max;
        }
        return requested_value;
    }

    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (value instanceof Number)
            setValue( ((Number)value).doubleValue());
        else if (value instanceof String)
            setValue(parseExpandedSpecification((String)value));
        else
            throw new IllegalArgumentException("Property '" + getName() +
                "' requires double, but received " + value.getClass().getName());
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
