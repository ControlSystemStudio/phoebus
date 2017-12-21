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

/** Widget property with String value.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StringWidgetProperty extends MacroizedWidgetProperty<String>
{
    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     *  @param use_class Follow value suggested by class?
     */
    public StringWidgetProperty(
            final WidgetPropertyDescriptor<String> descriptor,
            final Widget widget,
            final String default_value)
    {
        super(descriptor, widget, default_value);
    }

    @Override
    protected String parseExpandedSpecification(final String text) throws Exception
    {
        return text;
    }

    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        setValue(value.toString());
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

    /** @return Debug representation */
    @Override
    public String toString()
    {
        final String safe_copy = value;
        if (safe_copy == null)
            return "'" + getName() + "' = '" + specification + "'";
        else
            return "'" + getName() + "' = '" + value + "'";
    }
}
