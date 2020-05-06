/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newColorPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFillColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLimitsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMaximum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMinimum;

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
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that displays a tank with variable fill level
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TankWidget extends PVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("tank", WidgetCategory.MONITOR,
            "Tank",
            "/icons/tank.png",
            "Tank that 'fills' relative to numeric value of a PV",
            Arrays.asList("org.csstudio.opibuilder.widgets.tank"))
    {
        @Override
        public Widget createWidget()
        {
            return new TankWidget();
        }
    };

    public static final WidgetPropertyDescriptor<WidgetColor> propEmptyColor =
        newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "empty_color", Messages.WidgetProperties_EmptyColor);
    public static final WidgetPropertyDescriptor<Boolean>   propScaleVisible =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "scale_visible", Messages.WidgetProperties_ScaleVisible);

    /** Widget configurator to read legacy *.opi files*/
    private static class CustomConfigurator extends WidgetConfigurator
    {
        public CustomConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element xml)
                throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            if (xml_version.getMajor() < 2)
            {
                final TankWidget tank = (TankWidget) widget;

                Element element = XMLUtil.getChildElement(xml, "color_fillbackground");
                if (element != null)
                    tank.empty_color.readFromXML(model_reader, element);

                element = XMLUtil.getChildElement(xml, "scale_font");
                if (element != null)
                    tank.font.readFromXML(model_reader, element);

                if (XMLUtil.getChildBoolean(xml, "show_markers").orElse(true)  &&
                    (XMLUtil.getChildBoolean(xml, "show_hi").orElse(true)   ||
                     XMLUtil.getChildBoolean(xml, "show_hihi").orElse(true) ||
                     XMLUtil.getChildBoolean(xml, "show_lo").orElse(true)   ||
                     XMLUtil.getChildBoolean(xml, "show_lolo").orElse(true)))
                {   // There was at least one marker,
                    // but this widget is not supporting the legacy markers.
                    // -> Adjust width so that tank uses roughly the same space,
                    //    _not_ extending into the region that used to be occupied
                    //    by the markers.
                    tank.propWidth().setValue(Math.max(tank.propWidth().getValue() - 50, 50));
                }
            }

            return true;
        }
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new CustomConfigurator(persisted_version);
    }

    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetColor> fill_color;
    private volatile WidgetProperty<WidgetColor> empty_color;
    private volatile WidgetProperty<Boolean> scale_visible;
    private volatile WidgetProperty<Boolean> limits_from_pv;
    private volatile WidgetProperty<Double> minimum;
    private volatile WidgetProperty<Double> maximum;

    public TankWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 150, 200);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.READ_BACKGROUND)));
        properties.add(fill_color = propFillColor.createProperty(this, new WidgetColor(0, 0, 255)));
        properties.add(empty_color = propEmptyColor.createProperty(this, new WidgetColor(192, 192, 192)));
        properties.add(scale_visible = propScaleVisible.createProperty(this, true));
        properties.add(limits_from_pv = propLimitsFromPV.createProperty(this, true));
        properties.add(minimum = propMinimum.createProperty(this, 0.0));
        properties.add(maximum = propMaximum.createProperty(this, 100.0));
    }

    @Override
    public WidgetProperty<?> getProperty(String name) throws IllegalArgumentException, IndexOutOfBoundsException
    {
        // Support legacy scripts/rules that access color_fillbackground
        if (name.equals("color_fillbackground"))
            return propEmptyColor();
        return super.getProperty(name);
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForeground()
    {
        return foreground;
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackground()
    {
        return background;
    }

    /** @return 'fill_color' property */
    public WidgetProperty<WidgetColor> propFillColor()
    {
        return fill_color;
    }

    /** @return 'empty_color' property */
    public WidgetProperty<WidgetColor> propEmptyColor()
    {
        return empty_color;
    }

    /** @return 'scale_visible' property */
    public WidgetProperty<Boolean> propScaleVisible()
    {
        return scale_visible;
    }

    /** @return 'limits_from_pv' property */
    public WidgetProperty<Boolean> propLimitsFromPV()
    {
        return limits_from_pv;
    }

    /** @return 'minimum' property */
    public WidgetProperty<Double> propMinimum()
    {
        return minimum;
    }

    /** @return 'maximum' property */
    public WidgetProperty<Double> propMaximum()
    {
        return maximum;
    }
}
