/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.LineStyle;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.w3c.dom.Element;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.*;

/** Widget that displays a static rectangle
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RectangleWidget extends MacroWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("rectangle", WidgetCategory.GRAPHIC,
            "Rectangle",
            "/icons/rectangle.png",
            "A rectangle",
            Arrays.asList("org.csstudio.opibuilder.widgets.Rectangle",
                          "org.csstudio.opibuilder.widgets.RoundedRectangle"))
    {
        @Override
        public Widget createWidget()
        {
            return new RectangleWidget();
        }
    };

    public static final WidgetPropertyDescriptor<Integer> propCornerWidth =
        CommonWidgetProperties.newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "corner_width", Messages.WidgetProperties_CornerWidth);
    public static final WidgetPropertyDescriptor<Integer> propCornerHeight =
        CommonWidgetProperties.newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "corner_height", Messages.WidgetProperties_CornerHeight);


    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<Boolean> transparent;
    private volatile WidgetProperty<WidgetColor> line_color;
    private volatile WidgetProperty<Integer> line_width;
    private volatile WidgetProperty<LineStyle> line_style;
    private volatile WidgetProperty<Integer> corner_width;
    private volatile WidgetProperty<Integer> corner_height;

    public RectangleWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(line_width = propLineWidth.createProperty(this, 3));
        properties.add(line_color = propLineColor.createProperty(this, new WidgetColor(0, 0, 255)));
        properties.add(line_style = propLineStyle.createProperty(this, LineStyle.SOLID));
        properties.add(background = propBackgroundColor.createProperty(this, new WidgetColor(30, 144, 255)));
        properties.add(transparent = propTransparent.createProperty(this, false));
        properties.add(corner_width = propCornerWidth.createProperty(this, 0));
        properties.add(corner_height = propCornerHeight.createProperty(this, 0));
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new CustomWidgetConfigurator(persisted_version);
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'transparent' property */
    public WidgetProperty<Boolean> propTransparent()
    {
        return transparent;
    }

    /** @return 'line_color' color */
    public WidgetProperty<WidgetColor> propLineColor()
    {
        return line_color;
    }

    /** @return 'line_width' property */
    public WidgetProperty<Integer> propLineWidth()
    {
        return line_width;
    }

    /** @return 'line_style' property */
    public WidgetProperty<LineStyle> propLineStyle()
    {
        return line_style;
    }

    /** @return 'corner_width' property */
    public WidgetProperty<Integer> propCornerWidth()
    {
        return corner_width;
    }

    /** @return 'corner_height' property */
    public WidgetProperty<Integer> propCornerHeight()
    {
        return corner_height;
    }

    /** Handle legacy XML format */
    private static class CustomWidgetConfigurator extends LegacyWidgetConfigurator
    {

        public CustomWidgetConfigurator(Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(ModelReader model_reader, Widget widget, Element widget_xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, widget_xml))
            {
                return false;
            }

            if (xml_version.getMajor() < 2)
            {
                // Map border properties to out'line'
                OutlineSupport.handleLegacyBorder(widget, widget_xml);
            }
            return true;
        }
    }
}
