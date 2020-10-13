/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineStyle;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineWidth;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.LineStyle;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that displays a static area defined by points
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PolygonWidget extends PolyBaseWidget
{
    /** Legacy polygon used 1.0.0 */
    private static final Version version = new Version(2, 0, 0);

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("polygon", WidgetCategory.GRAPHIC,
            "Polygon",
            "/icons/polygon.png",
            "Area defined by points",
            Arrays.asList("org.csstudio.opibuilder.widgets.polygon"))
    {
        @Override
        public Widget createWidget()
        {
            return new PolygonWidget();
        }
    };

    /** Adjust "&lt;points>" in the XML
     *
     *  <p>Legacy coordinates were relative to display or parent group.
     *  New coords. are relative to this widget's x/y position.
     *  @param widget_xml XML for widget where "points" will be adjusted
     *  @throws Exception on error
     */
    static void adjustXMLPoints(final Element widget_xml) throws Exception
    {
        final int x0 = XMLUtil.getChildInteger(widget_xml, "x").orElse(0);
        final int y0 = XMLUtil.getChildInteger(widget_xml, "y").orElse(0);
        Element xml = XMLUtil.getChildElement(widget_xml, "points");
        if (xml != null)
        {
            for (Element p_xml : XMLUtil.getChildElements(xml, "point"))
            {   // Fetch legacy x, y attributes
                final int x = Integer.parseInt(p_xml.getAttribute("x"));
                final int y = Integer.parseInt(p_xml.getAttribute("y"));
                // Adjust to be relative to x0/y0
                final int nx = x - x0;
                final int ny = y - y0;
                p_xml.setAttribute("x", Integer.toString(nx));
                p_xml.setAttribute("y", Integer.toString(ny));
            }
        }
    }

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
                adjustXMLPoints(widget_xml);
                // In case a re-parse is triggered, prevent another XMLPoints adjustment
                // by marking as current version
                widget_xml.setAttribute(XMLTags.VERSION, version.toString());

                MacroWidget.importPVName(model_reader, widget, widget_xml);
                // TODO fix the Mapping of the border properties to out'line'
                OutlineSupport.handleLegacyBorder(widget, widget_xml);
            }
            // Parse updated XML
            return super.configureFromXML(model_reader, widget, widget_xml);
        }
    };

    private volatile WidgetProperty<WidgetColor> background_color;
    private volatile WidgetProperty<WidgetColor> line_color;
    private volatile WidgetProperty<Integer> line_width;
    private volatile WidgetProperty<LineStyle> line_style;

    public PolygonWidget()
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
        properties.add(background_color = propBackgroundColor.createProperty(this, new WidgetColor(50, 50, 255)));
    }

    @Override
    public Version getVersion()
    {
        return version;
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

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background_color;
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
}
