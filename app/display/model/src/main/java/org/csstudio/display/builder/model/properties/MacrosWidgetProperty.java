/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
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
import org.csstudio.display.builder.model.macros.MacroXMLUtil;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.phoebus.framework.macros.Macros;
import org.w3c.dom.Element;

import java.util.Map;

/** Widget property that describes macros.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacrosWidgetProperty extends WidgetProperty<Macros>
{
    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public MacrosWidgetProperty(
            final WidgetPropertyDescriptor<Macros> descriptor,
            final Widget widget,
            final Macros default_value)
    {
        super(descriptor, widget, default_value);
        // Start with value as copy of the default.
        // When later changing value via 'put(key, value)',
        // this asserts that the default remains unchanged,
        // and isDefaultValue() will report correctly.
        value = new Macros(default_value);
    }

    /** @param value Must be {@link org.csstudio.display.builder.model.spi.ActionInfo} array(!), not List */
    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (value instanceof Macros)
        {
            setValue((Macros) value);
        }
        else if (value instanceof Map)
        {
            setValue(fromMap((Map<Object, Object>) value));
        }
        else if (value instanceof String)
        {
            setValue(Macros.fromSimpleSpec((String) value));
        }
        else
        {
            throw new Exception("Need Macros, got " + value);
        }
    }

    /**
     * Parse Macro information from a {@link Map}
     * Note: since Maps do not preserve order, this helper is for limited backward compatibility
     * @param names_and_values a map of macro names( keys ) and their values
     * @return a {@link Macros} initialized using the names and values from the map
     */
    private static Macros fromMap(Map<Object, Object> names_and_values)
    {
        Macros macros = new Macros();
        names_and_values.entrySet().forEach(e -> macros.add(String.valueOf(e.getKey()), String.valueOf(e.getValue())));
        return macros;
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {
        MacroXMLUtil.writeMacros(writer, value);
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        setValue(MacroXMLUtil.readMacros(property_xml));
    }
}
