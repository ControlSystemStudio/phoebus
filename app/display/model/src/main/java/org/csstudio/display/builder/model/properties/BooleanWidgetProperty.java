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

/** Widget property with Boolean value.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BooleanWidgetProperty extends MacroizedWidgetProperty<Boolean>
{
    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public BooleanWidgetProperty(
            final WidgetPropertyDescriptor<Boolean> descriptor,
            final Widget widget,
            final Boolean default_value)
    {
        super(descriptor, widget, default_value);
    }

    @Override
    protected Boolean parseExpandedSpecification(final String text) throws Exception
    {
        if (text == null  ||  text.isEmpty())
            return default_value;
        if ("true".equalsIgnoreCase(text) ||
            "yes".equalsIgnoreCase(text)  ||
            "1".equals(text))
            return true;
        if ("false".equalsIgnoreCase(text) ||
            "no".equalsIgnoreCase(text)  ||
            "0".equals(text))
            return false;

        throw new Exception("Boolean property '" + getName() +
                "' has invalid value " + text);
    }

    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (value instanceof Boolean)
            setValue( (Boolean) value);
        else
            setValue(parseExpandedSpecification(value.toString()));
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
