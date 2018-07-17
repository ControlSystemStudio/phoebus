/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget property with Points as value.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PointsWidgetProperty extends WidgetProperty<Points>
{
    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public PointsWidgetProperty(
            final WidgetPropertyDescriptor<Points> descriptor,
            final Widget widget,
            final Points default_value)
    {
        super(descriptor, widget, default_value);
        // Store copy of original value so that default_value remains unchanged
        value = default_value.clone();
    }

    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (value instanceof Points)
            setValue((Points)value);
        else
            throw new IllegalArgumentException("Property '" + getName() +
                "' requires Points, but received " + value.getClass().getName());
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {
        for (Point p : value)
        {   // <point x="48" y="102" />
            writer.writeStartElement("point");
            writer.writeAttribute("x", Double.toString(p.getX()));
            writer.writeAttribute("y", Double.toString(p.getY()));
            writer.writeEndElement();
        }
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        final Points points = new Points();
        for (Element p_xml : XMLUtil.getChildElements(property_xml, "point"))
        {
            // TODO Error handling
            final double x = Double.parseDouble(p_xml.getAttribute("x"));
            final double y = Double.parseDouble(p_xml.getAttribute("y"));
            points.add(x, y);
        }
        setValue(points);
    }
}
