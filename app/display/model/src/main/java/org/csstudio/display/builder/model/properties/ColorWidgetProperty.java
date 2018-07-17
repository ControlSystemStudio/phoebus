/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.List;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget property with Color as value.
 *
 *  <p>Named colors are written with their name and the current RGB data.
 *
 *  <p>When loading a named color, an attempt is made to obtain the
 *  current definition of that color from the {@link WidgetColorService}.
 *  If the color is not known by name, the RGB data from the saved config
 *  is used, but the color still keeps its name so that it can be saved
 *  with that name and later loaded as a known color.
 *
 *  <p>Property allows writing as {@link WidgetColor} but also
 *  as list of red, green, blue integers 0-255.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ColorWidgetProperty extends WidgetProperty<WidgetColor>
{
    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public ColorWidgetProperty(
            final WidgetPropertyDescriptor<WidgetColor> descriptor,
            final Widget widget,
            final WidgetColor default_value)
    {
        super(descriptor, widget, default_value);
    }

    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (value instanceof WidgetColor)
            setValue( (WidgetColor) value);
        else if (value instanceof List)
        {
            final List<?> components = (List<?>) value;
            if (components.size() < 3  ||
                components.size() > 4  ||
                ! (components.get(0) instanceof Number))
                throw new IllegalArgumentException("Expect list of [ red, green, blue (, alpha) ] values 0..255");
            final int red = ((Number) components.get(0)).intValue();
            final int green = ((Number) components.get(1)).intValue();
            final int blue = ((Number) components.get(2)).intValue();
            final int alpha = components.size() == 4 ? ((Number) components.get(3)).intValue() : 255;
            setValue(new WidgetColor(red, green, blue, alpha));
        }
        else
            throw new IllegalArgumentException(String.valueOf(value));
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {
        writer.writeStartElement(XMLTags.COLOR);
        if (value instanceof NamedWidgetColor)
            writer.writeAttribute(XMLTags.NAME, ((NamedWidgetColor) value).getName());
        writer.writeAttribute(XMLTags.RED, Integer.toString(value.getRed()));
        writer.writeAttribute(XMLTags.GREEN, Integer.toString(value.getGreen()));
        writer.writeAttribute(XMLTags.BLUE, Integer.toString(value.getBlue()));
        if (value.getAlpha() != 255)
            writer.writeAttribute(XMLTags.ALPHA, Integer.toString(value.getAlpha()));
        writer.writeEndElement();
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        final Element col_el = XMLUtil.getChildElement(property_xml, XMLTags.COLOR);
        if (col_el == null)
            return;

        final String name = col_el.getAttribute(XMLTags.NAME);
        final int red, green, blue, alpha;
        try
        {
            red = getAttrib(col_el, XMLTags.RED);
            green = getAttrib(col_el, XMLTags.GREEN);
            blue = getAttrib(col_el, XMLTags.BLUE);
            final String al_txt = col_el.getAttribute(XMLTags.ALPHA);
            alpha = al_txt.isEmpty() ? 255 : Integer.parseInt(al_txt);
        }
        catch (Exception ex)
        {   // Older legacy files had no red/green/blue info for named colors
            logger.log(Level.WARNING, "Line " + XMLUtil.getLineInfo(property_xml), ex);
            if (name.isEmpty())
                setValue(WidgetColorService.getColor(NamedWidgetColors.TEXT));
            else
                setValue(WidgetColorService.getColor(name));
            return;
        }
        final WidgetColor color;
        if (name.isEmpty())
            // Plain color
            color = new WidgetColor(red, green, blue, alpha);
        else
            color = WidgetColorService.getColors().resolve(new NamedWidgetColor(name, red, green, blue, alpha));
        setValue(color);
    }

    private int getAttrib(final Element element, final String attrib) throws Exception
    {
        final String text = element.getAttribute(attrib);
        if (text.isEmpty())
            throw new Exception("<color> without " + attrib);
        return Integer.parseInt(text);
    }
}
