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
import org.csstudio.display.builder.model.WidgetClassSupport;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget property for widget class.
 *
 *  @author Kay Kasemir
 */
public class WidgetClassProperty extends WidgetProperty<String>
{

    public WidgetClassProperty(final WidgetPropertyDescriptor<String> descriptor,
                               final Widget widget)
    {
        super(descriptor, widget, WidgetClassSupport.DEFAULT);
    }

    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        setValue(value.toString());
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer)
            throws Exception
    {
        writer.writeCharacters(value);
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml)
            throws Exception
    {
        setValue(XMLUtil.getString(property_xml));
    }
}
