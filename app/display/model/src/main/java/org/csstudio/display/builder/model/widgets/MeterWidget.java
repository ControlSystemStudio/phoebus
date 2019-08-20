/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newColorPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFormat;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLimitsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMaximum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMinimum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPrecision;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propShowUnits;

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
import org.phoebus.ui.vtype.FormatOption;
import org.w3c.dom.Element;

/** Widget that displays a number on a meter
 *
 *  @author Kay Kasemir
 *  @author Claudio Rosati V2 meter widget
 */
@SuppressWarnings("nls")
public class MeterWidget extends PVWidget
{
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("meter", WidgetCategory.MONITOR,
            "Meter",
            "/icons/meter.png",
            "Displays current value of PV in Meter",
            Arrays.asList("org.csstudio.opibuilder.widgets.gauge",
                          "org.csstudio.opibuilder.widgets.meter"))
    {
        @Override
        public Widget createWidget()
        {
            return new MeterWidget();
        }
    };

    /** 1.0.0: BOY
     *  2.0.0: Display Builder meter based on 3rd party JFX lib
     *  3.0.0: Simpler meter, drawn in background
     */
    public static final Version METER_VERSION = new Version(3, 0, 0);

    /** Custom configurator to read legacy files */
    protected static class MeterConfigurator extends WidgetConfigurator
    {
        public MeterConfigurator(final Version xmlVersion)
        {
            super(xmlVersion);
        }

        @Override
        public boolean configureFromXML(final ModelReader reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (!super.configureFromXML(reader, widget, xml))
                return false;

            final MeterWidget meter = (MeterWidget) widget;

            if (xml_version.getMajor() < 2)
            {   // BOY

                final Element e = XMLUtil.getChildElement(xml, "scale_font");
                if (e != null)
                    meter.propFont().readFromXML(reader, e);

                XMLUtil.getChildBoolean(xml, "show_value_label")
                       .ifPresent(meter.propShowValue()::setValue);
                // Are any of the limits disabled, or 'Show Ramp' disabled?
                if ((!XMLUtil.getChildBoolean(xml, "show_hihi").orElse(true) &&
                     !XMLUtil.getChildBoolean(xml, "show_hi").orElse(true) &&
                     !XMLUtil.getChildBoolean(xml, "show_lo").orElse(true)  &&
                     !XMLUtil.getChildBoolean(xml, "show_lolo").orElse(true)
                    )
                    ||
                    !XMLUtil.getChildBoolean(xml, "show_markers").orElse(true))
                    meter.propShowLimits().setValue(false);
            }
            else if (xml_version.getMajor() < 3)
            {   // Display Builder meter based on 3rd party JFX lib
                XMLUtil.getChildBoolean(xml, "value_visible")
                       .ifPresent(meter.propShowValue()::setValue);
                XMLUtil.getChildBoolean(xml, "unit_from_pv")
                       .ifPresent(meter.propShowUnits()::setValue);

                if (!XMLUtil.getChildBoolean(xml, "show_hihi").orElse(true) &&
                    !XMLUtil.getChildBoolean(xml, "show_high").orElse(true) &&
                    !XMLUtil.getChildBoolean(xml, "show_low").orElse(true)  &&
                    !XMLUtil.getChildBoolean(xml, "show_lolo").orElse(true))
                    meter.propShowLimits().setValue(false);
            }




            return true;
        }
    }

    public static final WidgetPropertyDescriptor<Boolean> propShowValue =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_value", Messages.WidgetProperties_ShowValue);

    public static final WidgetPropertyDescriptor<Boolean> propShowLimits =
            newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_limits", Messages.WidgetProperties_ShowLimits);

    public static final WidgetPropertyDescriptor<WidgetColor> propNeedleColor =
        newColorPropertyDescriptor(WidgetPropertyCategory.MISC, "needle_color", Messages.WidgetProperties_NeedleColor);

    public static final WidgetPropertyDescriptor<WidgetColor> propKnobColor =
        newColorPropertyDescriptor(WidgetPropertyCategory.MISC, "knob_color", Messages.WidgetProperties_KnobColor);

    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<FormatOption> format;
    private volatile WidgetProperty<Integer> precision;
    private volatile WidgetProperty<Boolean> show_value;
    private volatile WidgetProperty<Boolean> show_units;
    private volatile WidgetProperty<Boolean> show_limits;
    private volatile WidgetProperty<WidgetColor> needle_color;
    private volatile WidgetProperty<WidgetColor> knob_color;
    private volatile WidgetProperty<Boolean> limits_from_pv;
    private volatile WidgetProperty<Double> minimum;
    private volatile WidgetProperty<Double> maximum;


    public MeterWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 240, 120);
    }

    @Override
    public Version getVersion()
    {
        return METER_VERSION;
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persistedVersion) throws Exception
    {
        return new MeterConfigurator(persistedVersion);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);

        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(format = propFormat.createProperty(this, FormatOption.DEFAULT));
        properties.add(precision = propPrecision.createProperty(this, -1));
        properties.add(show_value = propShowValue.createProperty(this, true));
        properties.add(show_units = propShowUnits.createProperty(this, true));
        properties.add(show_limits = propShowLimits.createProperty(this, true));
        properties.add(needle_color = propNeedleColor.createProperty(this, new WidgetColor(255, 5, 7)));
        properties.add(knob_color = propKnobColor.createProperty(this, new WidgetColor(177, 166, 155)));
        properties.add(limits_from_pv = propLimitsFromPV.createProperty(this, true));
        properties.add(minimum = propMinimum.createProperty(this, 0.0));
        properties.add(maximum = propMaximum.createProperty(this, 100.0));
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

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'format' property */
    public WidgetProperty<FormatOption> propFormat()
    {
        return format;
    }

    /** @return 'precision' property */
    public WidgetProperty<Integer> propPrecision()
    {
        return precision;
    }

    /** @return 'show_value' property */
    public WidgetProperty<Boolean> propShowValue()
    {
        return show_value;
    }

    /** @return 'show_units' property */
    public WidgetProperty<Boolean> propShowUnits()
    {
        return show_units;
    }

    /** @return 'show_limits' property */
    public WidgetProperty<Boolean> propShowLimits()
    {
        return show_limits;
    }

    /** @return 'needle_color' property */
    public WidgetProperty<WidgetColor> propNeedleColor()
    {
        return needle_color;
    }

    /** @return 'knob_color' property */
    public WidgetProperty<WidgetColor> propKnobColor()
    {
        return knob_color;
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
