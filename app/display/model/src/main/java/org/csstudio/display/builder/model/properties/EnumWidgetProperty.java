/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import java.util.Objects;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget property with enumerated value.
 *
 *  <p>Requires an actual enum as a value,
 *  and code that is aware of that enum can
 *  get/set the property as such.
 *
 *  <p>Enums that are exposed to the user should
 *  have a localized string representation (Label)
 *  that is provided by the <code>toString()</code>
 *  method of the enum.
 *  The user interface should present the value
 *  as a label.
 *  Internal code should use the ordinal or,
 *  if exact type is known, the actual enum.
 *
 *  <p>The 'specification', i.e. the potentially
 *  macro-based string for the property, must evaluate
 *  to a number for a valid ordinal.
 *
 *  <p>Property offers helpers to obtain all 'Labels'
 *  and to set an unknown enum from its ordinal.
 *
 *  <p>Note that there is no support to set the property
 *  from a 'Label' or 'Name' to avoid any ambiguity between
 *  labels and names, or labels [ "1", "2", "3"] that could
 *  be mistaken for ordinals [ 1, 2, 3 ] but actually correspond
 *  to ordinals [ 0, 1, 2 ].
 *
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EnumWidgetProperty<E extends Enum<E>> extends MacroizedWidgetProperty<E>
{
    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public EnumWidgetProperty(final WidgetPropertyDescriptor<E> descriptor,
                              final Widget widget,
                              final E default_value)
    {
        // Default value must be non-null because it's used to determine
        // labels, names, ordinals
        super(descriptor, widget, Objects.requireNonNull(default_value));
    }

    /** Get labels, i.e. localized representations of all enum values
     *
     *  <p>Size of the array also provides the valid ordinal range
     *  @return Labels
     */
    public String[] getLabels()
    {
        final Enum<?>[] values = default_value.getDeclaringClass().getEnumConstants();
        final String[] labels = new String[values.length];
        for (int i=0; i<labels.length; ++i)
            labels[i] = values[i].toString();
        return labels;
    }

    @Override
    protected String computeSpecification(final E value)
    {   // Specification uses ordinal.
        return Integer.toString(value.ordinal());
    }

    @Override
    protected E parseExpandedSpecification(final String text) throws Exception
    {
        if (text == null  ||  text.isEmpty())
            return default_value;
        // Parse ordinal from specification text
        final int ordinal;
        try
        {
            ordinal = Integer.parseInt(text);
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Enum property '" + getName() + "' expects ordinal but received '" + text + "'");
        }
        return getValueByOrdinal(ordinal);
    }

    /** @param ordinal Ordinal
     *  @return Enum for that ordinal
     *  @throws Exception if ordinal out of range
     */
    private E getValueByOrdinal(final int ordinal) throws Exception
    {
        final E[] values = default_value.getDeclaringClass().getEnumConstants();
        if (ordinal < 0  ||  ordinal >= values.length)
            throw new Exception("Invalid ordinal " + ordinal + " for " + getName());
        return values[ordinal];
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValueFromObject(final Object value) throws Exception
    {   // Proper type?
        if (value instanceof Enum<?>  &&
            ((Enum<?>)value).getDeclaringClass() == default_value.getDeclaringClass())
            setValue((E) value);
        else if (value instanceof Number)
        {   // Use ordinal
            final int ordinal = ((Number)value).intValue();
            setValue(getValueByOrdinal(ordinal));
        }
        else // Use name
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

    @Override
    public String toString()
    {
        final E safe_copy = value;
        if (safe_copy == null)
            return "'" + getName() + "' = \"" + specification + "\"";
        else
            return "'" + getName() + "' = " + value.name() + " (" + value.ordinal() + ", '" + value.toString() + "')";
    }
}
