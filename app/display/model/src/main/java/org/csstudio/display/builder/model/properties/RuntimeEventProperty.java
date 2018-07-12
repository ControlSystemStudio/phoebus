/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import java.time.Instant;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.w3c.dom.Element;

/** Runtime event property
 *
 *  <p>This type of property is meant to trigger something,
 *  its value is the {@link Instant} in time when it was last triggered.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RuntimeEventProperty extends WidgetProperty<Instant>
{
    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public RuntimeEventProperty(
            final WidgetPropertyDescriptor<Instant> descriptor,
            final Widget widget,
            final Instant default_value)
    {
        super(descriptor, widget, default_value);
        if (descriptor.getCategory() != WidgetPropertyCategory.RUNTIME)
            throw new IllegalArgumentException("Must be a runtime property");
    }

    /** Trigger this property */
    public void trigger()
    {
        setValue(Instant.now());
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {
        throw new Exception("Runtime property " + getName() + " is not persisted");
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        // Runtime properties are not read from XML
    }

    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (value instanceof Instant)
            setValue((Instant) value);
        else
            throw new Exception("Need Instant, got " + value);
    }
}
