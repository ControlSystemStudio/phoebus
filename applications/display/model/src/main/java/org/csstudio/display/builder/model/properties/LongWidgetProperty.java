/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.model.properties;


import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;


/**
 * Widget property with Long value.
 *
 * @author Claudio Rosati, European Spallation Source ERIC
 * @version 1.0.0 6 Feb 2017
 */
@SuppressWarnings( "nls" )
public class LongWidgetProperty extends MacroizedWidgetProperty<Long> {

    final private Long min, max;

    /**
     * Constructor
     *
     * @param descriptor Property descriptor.
     * @param widget Widget that holds the property and handles listeners.
     * @param default_value Default and initial value.
     */
    public LongWidgetProperty ( final WidgetPropertyDescriptor<Long> descriptor, final Widget widget, final Long default_value ) {
        this(descriptor, widget, default_value, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * Constructor
     *
     * @param descriptor Property descriptor.
     * @param widget Widget that holds the property and handles listeners.
     * @param default_value Default and initial value.
     * @param min Minimum value.
     * @param max Maximum value.
     */
    public LongWidgetProperty ( final WidgetPropertyDescriptor<Long> descriptor, final Widget widget, final Long default_value, final Long min, final Long max ) {

        super(descriptor, widget, default_value);

        this.min = min;
        this.max = max;

        if ( !restrictValue(default_value).equals(default_value) ) {
            throw new IllegalArgumentException("Default value outside range.");
        }

    }

    @Override
    public void readFromXML ( final ModelReader model_reader, final Element property_xml ) throws Exception {
        setSpecification(XMLUtil.getString(property_xml));
    }

    @Override
    public void setValueFromObject ( final Object value ) throws Exception {
        if ( value instanceof Long ) {
            setValue((Long) value);
        } else if ( value instanceof Number ) {
            setValue(( (Number) value ).longValue());
        } else if ( value instanceof String ) {
            setValue(parseExpandedSpecification((String) value));
        } else {
            throw new IllegalArgumentException("Property '" + getName() + "' requires long, but received: " + value.getClass().getName());
        }
    }

    @Override
    public void writeToXML ( final ModelWriter model_writer, final XMLStreamWriter writer ) throws Exception {
        writer.writeCharacters(specification);
    }

    @Override
    protected Long parseExpandedSpecification ( final String text ) throws Exception {
        try {
            //  Should be long..
            return Long.valueOf(text);
        } catch ( final NumberFormatException ex ) {
            //  .. but also allow "1e9", strictly a double, then truncate.
            try {
                return Double.valueOf(text).longValue();
            } catch ( final NumberFormatException ex2 ) {
                throw new Exception("Long property '" + getName() + "' has invalid value text '" + text + "'");
            }
        }
    }

    @Override
    protected Long restrictValue ( final Long requested_value ) {
        if ( requested_value.compareTo(min) < 0 ) {
            return min;
        } else if ( requested_value.compareTo(max) > 0 ) {
            return max;
        } else {
            return requested_value;
        }
    }

}
