/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newIntegerPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineStyle;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineWidth;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

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
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.csstudio.display.builder.model.properties.LineStyle;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Widget that displays a static line of points
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PolylineWidget extends PolyBaseWidget
{
    /** Legacy polyline used 1.0.0 */
    private static final Version version = new Version(2, 0, 0);

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("polyline", WidgetCategory.GRAPHIC,
            "Polyline",
            "/icons/polyline.png",
            "Line with two or more points",
            Arrays.asList("org.csstudio.opibuilder.widgets.polyline"))
    {
        @Override
        public Widget createWidget()
        {
            return new PolylineWidget();
        }
    };

    /** Polyline widget arrows */
    public enum Arrows
    {
        //The order of these enum constants is important.
        //The bits of the number returned by calling ordinal() on one
        //of them is useful for determining which arrows are used.
        NONE(Messages.Arrows_None), //NONE.ordinal() = 0 = 0b00 has no arrows
        FROM(Messages.Arrows_From), //FROM.ordinal() = 1 = 0b01 has only a from-arrow
        TO(Messages.Arrows_To),     //  TO.ordinal() = 2 = 0b10 has only a to-arrow
        BOTH(Messages.Arrows_Both); //BOTH.ordinal() = 3 = 0b11 has both arrows

        private final String name;

        private Arrows(final String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    /** Display 'arrows' */
    private static final WidgetPropertyDescriptor<Arrows> propArrows =
        new WidgetPropertyDescriptor<>(
            WidgetPropertyCategory.DISPLAY, "arrows", Messages.Arrows)
    {
        @Override
        public EnumWidgetProperty<Arrows> createProperty(final Widget widget,
                                                        final Arrows default_value)
        {
            return new EnumWidgetProperty<>(this, widget, default_value);
        }
    };

    private static final WidgetPropertyDescriptor<Integer> propArrowLength =
        newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "arrow_length", Messages.ArrowLength,
                                     2, Integer.MAX_VALUE);

    /** Handle legacy XML format */
    static class LegacyWidgetConfigurator extends WidgetConfigurator
    {
        public LegacyWidgetConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element widget_xml) throws Exception
        {
            if (xml_version.getMajor() < 2)
            {
                PolygonWidget.adjustXMLPoints(widget_xml);
                // Legacy used background color for the line
                Element xml = XMLUtil.getChildElement(widget_xml, "background_color");
                if (xml != null)
                {
                    final Document doc = widget_xml.getOwnerDocument();
                    Element line = doc.createElement(propLineColor.getName());
                    final Element c = XMLUtil.getChildElement(xml, "color");
                    line.appendChild(c.cloneNode(true));
                    widget_xml.appendChild(line);
                    widget_xml.removeChild(xml);

                    MacroWidget.importPVName(model_reader, widget, widget_xml);
                }
                // In case a re-parse is triggered, prevent another XMLPoints adjustment
                // by marking as current version
                widget_xml.setAttribute(XMLTags.VERSION, version.toString());
            }

            // Parse updated XML
            return super.configureFromXML(model_reader, widget, widget_xml);
        }
    };

    private volatile WidgetProperty<WidgetColor> line_color;
    private volatile WidgetProperty<Integer> line_width;
    private volatile WidgetProperty<LineStyle> line_style;
    private volatile WidgetProperty<Arrows> arrows;
    private volatile WidgetProperty<Integer> arrow_length;

    public PolylineWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    @Override
    public Version getVersion()
    {
        return version;
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(line_width = propLineWidth.createProperty(this, 3));
        properties.add(line_color = propLineColor.createProperty(this, new WidgetColor(0, 0, 255)));
        properties.add(line_style = propLineStyle.createProperty(this, LineStyle.SOLID));
        properties.add(arrows = propArrows.createProperty(this, Arrows.NONE));
        properties.add(arrow_length = propArrowLength.createProperty(this, 20));
    }

    @Override
    public WidgetProperty<?> getProperty(final String name)
    {
        if ("background_color".equals(name))
        {
            logger.log(Level.WARNING, "Deprecated access to " + this + " property 'background_color'. Use 'line_color'");
            return line_color;
        }
        return super.getProperty(name);
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        if (persisted_version.compareTo(version) < 0)
            return new LegacyWidgetConfigurator(persisted_version);
        else
            return super.getConfigurator(persisted_version);
    }

    /** @return 'line_color' property */
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

    /** @return 'arrows' property */
    public WidgetProperty<Arrows> propArrows()
    {
        return arrows;
    }

    /** @return 'arrow_length' property */
    public WidgetProperty<Integer> propArrowLength()
    {
        return arrow_length;
    }
}
