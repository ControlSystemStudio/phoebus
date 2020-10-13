/*******************************************************************************
 * Copyright (c) 2016-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newDoublePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineStyle;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;

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
import org.csstudio.display.builder.model.properties.LineStyle;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that displays an arc
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class ArcWidget extends MacroWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("arc", WidgetCategory.GRAPHIC,
            "Arc",
            "/icons/arc.png",
            "An arc",
            Arrays.asList("org.csstudio.opibuilder.widgets.arc"))
    {
        @Override
        public Widget createWidget()
        {
            return new ArcWidget();
        }
    };


    /** Custom configurator to read legacy *.opi files */
    private static class CustomConfigurator extends WidgetConfigurator
    {
        public CustomConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element widget_xml)
                throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, widget_xml))
                return false;

            if (xml_version.getMajor() < 2)
            {
                final ArcWidget arc = (ArcWidget) widget;
                // Foreground color has been renamed to line color
                final Element el = XMLUtil.getChildElement(widget_xml, "foreground_color");
                if (el != null)
                    arc.propLineColor().readFromXML(model_reader, el);

                // 'Fill' is similar to the new 'transparent' option
                XMLUtil.getChildBoolean(widget_xml, "fill")
                       .ifPresent(fill -> arc.propTransparent().setValue(! fill));

                MacroWidget.importPVName(model_reader, widget, widget_xml);
                // Map border properties to out'line'
                OutlineSupport.handleLegacyBorder(widget, widget_xml);
            }
            return true;
        }
    }

    private static final WidgetPropertyDescriptor<Double> propAngleStart =
            newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "start_angle", Messages.WidgetProperties_AngleStart);

    private static final WidgetPropertyDescriptor<Double> propAngleSize =
            newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "total_angle", Messages.WidgetProperties_AngleSize);

    // fill color
    private WidgetProperty<WidgetColor> background;
    private WidgetProperty<Boolean> transparent;
    // line color and width
    private WidgetProperty<WidgetColor> line_color;
    private WidgetProperty<Integer> line_width;
    private volatile WidgetProperty<LineStyle> line_style;
    // start/size degree of arc (0-365)
    private WidgetProperty<Double> arc_start;
    private WidgetProperty<Double> arc_size;


	public ArcWidget()
	{
		super(WIDGET_DESCRIPTOR.getType(), 100, 100);
	}

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new CustomConfigurator(persisted_version);
    }

	// By default create an arc with dark blue line, light blue interior, no transparency, 90 degree angle from 0-90
    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(arc_start = propAngleStart.createProperty(this, 0.0));
        properties.add(arc_size = propAngleSize.createProperty(this, 90.0));
        properties.add(line_width = propLineWidth.createProperty(this, 3));
        properties.add(line_color = propLineColor.createProperty(this, new WidgetColor(0, 0, 255)));
        properties.add(line_style = propLineStyle.createProperty(this, LineStyle.SOLID));
        properties.add(background = propBackgroundColor.createProperty(this, new WidgetColor(30, 144, 255)));
        properties.add(transparent = propTransparent.createProperty(this, false));
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

    /** @return 'arc_start' property */
    public WidgetProperty<Double> propArcStart()
    {
        return arc_start;
    }

    /** @return 'arc_size' property */
    public WidgetProperty<Double> propArcSize()
    {
        return arc_size;
    }
}
