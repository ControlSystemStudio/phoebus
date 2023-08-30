
package org.csstudio.display.extra.widgets.linearmeter;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.*;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.ui.vtype.FormatOption;
import org.w3c.dom.Element;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.*;

@SuppressWarnings("nls")
public class LinearMeterWidget extends PVWidget
{
    public LinearMeterWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 240, 120);
    }

    public static WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("linearmeter", WidgetCategory.MONITOR,
            "LinearMeter",
            "/icons/linear-meter.png",
            "Compact monitor widget for the value of a PV.",
            Arrays.asList(""))
        {
            @Override
            public Widget createWidget()
            {
                return new LinearMeterWidget();
            }
        };

    /**
     * 1.0.0: Linear meter by Claudio Rosatti based on 3rd party library
     * 2.0.0: Simple linear meter, based on meter v 3.0.0, drawn in background
     */
    public static Version METER_VERSION = new Version(2, 0, 0);

    /** Custom configurator to read legacy files */
    protected static class LinearMeterConfigurator extends WidgetConfigurator
    {
        //TODO: This has to be fixed for the Linear Meter. Current implementation is
        // for the Meter widget and is not valid. No version 3.

        public LinearMeterConfigurator(Version xmlVersion)
        {
            super(xmlVersion);
        }

        @Override
        public boolean configureFromXML(ModelReader reader, Widget widget,
                                        Element xml) throws Exception
        {
            if (!super.configureFromXML(reader, widget, xml))
                return false;

            LinearMeterWidget meter = (LinearMeterWidget) widget;

            if (xml_version.getMajor() < 2)
            {   // BOY

                Element e = XMLUtil.getChildElement(xml, "scale_font");
                if (e != null)
                    meter.propFont().readFromXML(reader, e);

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

    public static WidgetPropertyDescriptor<Boolean> propShowLimits =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_limits", Messages.WidgetProperties_ShowLimits);

    public static WidgetPropertyDescriptor<Boolean> propDisplayHorizontal =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "displayHorizontal", Messages.WidgetProperties_Horizontal);

    public static WidgetPropertyDescriptor<Boolean>   propScaleVisible =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "scale_visible", Messages.WidgetProperties_ScaleVisible);

    public static WidgetPropertyDescriptor<WidgetColor> propNeedleColor =
        newColorPropertyDescriptor(WidgetPropertyCategory.MISC, "needle_color", Messages.WidgetProperties_NeedleColor);

    public static WidgetPropertyDescriptor<WidgetColor> knobColor_descriptor =
        newColorPropertyDescriptor(WidgetPropertyCategory.MISC, "knob_color", Messages.WidgetProperties_KnobColor);

    public static WidgetPropertyDescriptor<Integer> knobSize_descriptor =
            newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "knob_size", "Knob Size");

    public static WidgetPropertyDescriptor<Double> propLevelHiHi =
        newDoublePropertyDescriptor (WidgetPropertyCategory.BEHAVIOR,  "level_hihi", Messages.WidgetProperties_LevelHiHi);

    public static WidgetPropertyDescriptor<Double> propLevelHigh =
        newDoublePropertyDescriptor (WidgetPropertyCategory.BEHAVIOR,  "level_high", Messages.WidgetProperties_LevelHigh);

    public static WidgetPropertyDescriptor<Double> propLevelLoLo =
        newDoublePropertyDescriptor (WidgetPropertyCategory.BEHAVIOR,  "level_lolo", Messages.WidgetProperties_LevelLoLo);

    public static WidgetPropertyDescriptor<Double> propLevelLow =
        newDoublePropertyDescriptor (WidgetPropertyCategory.BEHAVIOR,  "level_low", Messages.WidgetProperties_LevelLow);

    public static StructuredWidgetProperty.Descriptor colorsStructuredWidget_descriptor =
            new StructuredWidgetProperty.Descriptor(WidgetPropertyCategory.DISPLAY, "colors", "Colors");

    public static WidgetPropertyDescriptor<WidgetColor> propNormalStatusColor_descriptor =
            newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "normal_status_color", "Normal Status Color");

    public static WidgetPropertyDescriptor<WidgetColor> minorWarningColor_descriptor =
            newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "minor_warning_color", "Low & High Warning Color");

    public static WidgetPropertyDescriptor<WidgetColor> majorWarningColor_descriptor =
            newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "major_warning_color", "LoLo & HiHi Warning Color");

    public static WidgetPropertyDescriptor<Boolean> isGradientEnabled_descriptor =
            newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "is_gradient_enabled", "Enable Gradient");

    public static WidgetPropertyDescriptor<Boolean> isHighlightingOfInactiveRegionsEnabled_descriptor =
            newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "is_highlighting_of_active_regions_enabled", "Highlight Active Region");

    public static WidgetPropertyDescriptor<Integer> needleWidth_descriptor =
            newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "needle_width", "Needle Width");

    private WidgetProperty<WidgetColor> foreground;
    private WidgetProperty<WidgetColor> background;
    private WidgetProperty<WidgetFont> font;
    private WidgetProperty<FormatOption> format;
    private WidgetProperty<Boolean> show_units;
    private WidgetProperty<Boolean> show_limits;
    private WidgetProperty<WidgetColor> needle_color;
    private WidgetProperty<Boolean> scale_visible;
    private WidgetProperty<WidgetColor> knob_color;
    private WidgetProperty<Integer> knobSize;
    private WidgetProperty<Boolean> limits_from_pv;
    private WidgetProperty<Double> minimum;
    private WidgetProperty<Double> maximum;
    private WidgetProperty<Double> level_high;
    private WidgetProperty<Double> level_hihi;
    private WidgetProperty<Double> level_lolo;
    private WidgetProperty<Double> level_low;
    private WidgetProperty<Boolean> displayHorizontal;

    private StructuredWidgetProperty colorsStructuredWidget;
    private WidgetProperty<Boolean> isGradientEnabled;
    private WidgetProperty<Boolean> isHighlightingOfInactiveRegionsEnabled;
    private WidgetProperty<Integer> needleWidth;
    private WidgetProperty<WidgetColor> normalStatusColor;
    private WidgetProperty<WidgetColor> minorWarningColor;
    private WidgetProperty<WidgetColor> majorWarningColor;

    @Override
    public Version getVersion()
    {
        return METER_VERSION;
    }

    @Override
    public WidgetConfigurator getConfigurator(Version persistedVersion) throws Exception
    {
        return new LinearMeterConfigurator(persistedVersion);
    }

    @Override
    protected void defineProperties(List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);

        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(format = propFormat.createProperty(this, FormatOption.DEFAULT));
        properties.add(show_units = propShowUnits.createProperty(this, true));
        properties.add(scale_visible = propScaleVisible.createProperty(this, true));
        properties.add(show_limits = propShowLimits.createProperty(this, true));
        properties.add(limits_from_pv = propLimitsFromPV.createProperty(this, true));
        properties.add(minimum = propMinimum.createProperty(this, 0.0));
        properties.add(maximum = propMaximum.createProperty(this, 100.0));
        properties.add(displayHorizontal = propDisplayHorizontal.createProperty(this, true));
        properties.add(knobSize = knobSize_descriptor.createProperty(this, 8));
        foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT));
        background = propBackgroundColor.createProperty(this, new WidgetColor(0, 0, 0, 0));
        needle_color = propNeedleColor.createProperty(this, new WidgetColor(0, 0, 0, 255));
        knob_color = knobColor_descriptor.createProperty(this, new WidgetColor(0, 0, 0, 255));
        normalStatusColor = propNormalStatusColor_descriptor.createProperty(this, new WidgetColor(194,198,195));
        minorWarningColor = minorWarningColor_descriptor.createProperty(this, new WidgetColor(242, 148, 141));
        majorWarningColor = majorWarningColor_descriptor.createProperty(this, new WidgetColor(240, 60, 46));
        isGradientEnabled = isGradientEnabled_descriptor.createProperty(this, false);
        isHighlightingOfInactiveRegionsEnabled = isHighlightingOfInactiveRegionsEnabled_descriptor.createProperty(this, true);
        properties.add(needleWidth = needleWidth_descriptor.createProperty(this, 1));
        List<WidgetProperty<?>> colorSelectionWidgets = Arrays.asList(foreground,
                                                                      background,
                                                                      needle_color,
                                                                      knob_color,
                                                                      normalStatusColor,
                                                                      minorWarningColor,
                                                                      majorWarningColor,
                                                                      isGradientEnabled,
                                                                      isHighlightingOfInactiveRegionsEnabled);
        properties.add(colorsStructuredWidget = colorsStructuredWidget_descriptor.createProperty(this,
                                                                                                 colorSelectionWidgets));
        properties.add(level_lolo = propLevelLoLo.createProperty(this, 10.0));
        properties.add(level_low = propLevelLow.createProperty(this, 20.0));
        properties.add(level_high = propLevelHigh.createProperty(this, 80.0));
        properties.add(level_hihi = propLevelHiHi.createProperty(this, 90.0));
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

    /** @return 'show_units' property */
    public WidgetProperty<Boolean> propShowUnits()
    {
        return show_units;
    }

    /** @return 'scale_visible' property */
    public WidgetProperty<Boolean> propScaleVisible()
    {
        return scale_visible;
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

    public WidgetProperty<Integer> propKnobSize() { return knobSize; }

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

    public WidgetProperty<Boolean> propDisplayHorizontal() { return displayHorizontal; }

    public WidgetProperty<Double> propLevelHiHi ( ) {
        return level_hihi;
    }

    public WidgetProperty<Double> propLevelHigh ( ) {
        return level_high;
    }

    public WidgetProperty<Double> propLevelLoLo ( ) {
        return level_lolo;
    }

    public WidgetProperty<Double> propLevelLow ( ) {
        return level_low;
    }

    public WidgetProperty<Boolean> propIsGradientEnabled () {
        return isGradientEnabled;
    }

    public WidgetProperty<Boolean> propIsHighlightActiveRegionEnabled () {
        return isHighlightingOfInactiveRegionsEnabled;
    }

    public WidgetProperty<WidgetColor> propNormalStatusColor() {
        return normalStatusColor;
    }

    public WidgetProperty<WidgetColor> propMinorWarningColor() {
        return minorWarningColor;
    }
    
    public WidgetProperty<WidgetColor> propMajorWarningColor() {
        return majorWarningColor;
    }

    public WidgetProperty<Integer> propNeedleWidth() { return needleWidth; }

}
