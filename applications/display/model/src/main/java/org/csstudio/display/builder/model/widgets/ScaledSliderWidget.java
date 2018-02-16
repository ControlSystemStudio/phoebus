/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newDoublePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newIntegerPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontal;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propIncrement;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLimitsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMaximum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMinimum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropConfigure;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
import org.csstudio.display.builder.model.properties.FontWidgetProperty;
import org.csstudio.display.builder.model.properties.RuntimeEventProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that can read/write numeric PV via scaled slider
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ScaledSliderWidget extends WritablePVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("scaledslider", WidgetCategory.CONTROL,
            "Scaled Slider",
            "/icons/scaled_slider.png",
            "A scaled slider that can read/write a numeric PV",
            // Can also represent "org.csstudio.opibuilder.widgets.knob",
            // but using KnobWidget for that
            Arrays.asList("org.csstudio.opibuilder.widgets.scaledslider"))
    {
        @Override
        public Widget createWidget()
        {
            return new ScaledSliderWidget();
        }
    };

    /** Display 'scale_font': Font for scale */
    public static final WidgetPropertyDescriptor<WidgetFont> displayScaleFont =
        new WidgetPropertyDescriptor<WidgetFont>(
            WidgetPropertyCategory.DISPLAY, "scale_font", Messages.WidgetProperties_Font)
    {
        @Override
        public WidgetProperty<WidgetFont> createProperty(final Widget widget,
                                                         final WidgetFont font)
        {
            return new FontWidgetProperty(this, widget, font);
        }
    };

    /** 'show_scale' property: Show scale for scaled widget. */
    public static final WidgetPropertyDescriptor<Boolean> propShowScale =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_scale", Messages.WidgetProperties_ShowScale);

    /** 'show_minor_ticks' property: Show tick marks on scale. */
    public static final WidgetPropertyDescriptor<Boolean> propShowMinorTicks =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_minor_ticks", Messages.WidgetProperties_ShowMinorTicks);

    /** 'scale_format' property: Formatter for scale labels; follows java DecimalFormat */
    public static final WidgetPropertyDescriptor<String> propScaleFormat =
        newStringPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "scale_format", Messages.WidgetProperties_ScaleFormat);

    /** 'major_tick_step_hint' property: Minimum space, in pixels, between major tick marks. */
    public static final WidgetPropertyDescriptor<Integer> propMajorTickStepHint =
        newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "major_tick_step_hint", Messages.WidgetProperties_MajorTickStepHint);

    /** 'level_high' property: Level of HIGH value for widget*/
    public static final WidgetPropertyDescriptor<Double> propLevelHigh =
        newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "level_high", Messages.WidgetProperties_LevelHigh);

    /** 'level_hihi' property: Level of HIHI value for widget*/
    public static final WidgetPropertyDescriptor<Double> propLevelHiHi =
        newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "level_hihi", Messages.WidgetProperties_LevelHiHi);

    /** 'level_low' property: Level of LOW value for widget*/
    public static final WidgetPropertyDescriptor<Double> propLevelLow =
        newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "level_low", Messages.WidgetProperties_LevelLow);

    /** 'level_lolo' property: Level of LOW value for widget*/
    public static final WidgetPropertyDescriptor<Double> propLevelLoLo =
        newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "level_lolo", Messages.WidgetProperties_LevelLoLo);

    /** 'show_high' property: Whether to show HIGH marker*/
    public static final WidgetPropertyDescriptor<Boolean> propShowHigh =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_high", Messages.WidgetProperties_ShowHigh);

    /** 'show_hihi' property: Whether to show HIHI marker*/
    public static final WidgetPropertyDescriptor<Boolean> propShowHiHi =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_hihi", Messages.WidgetProperties_ShowHiHi);

    /** 'show_low' property: Whether to show LOW marker*/
    public static final WidgetPropertyDescriptor<Boolean> propShowLow =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_low", Messages.WidgetProperties_ShowLow);

    /** 'show_lo' property: Whether to show LOLO marker*/
    public static final WidgetPropertyDescriptor<Boolean> propShowLoLo =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_lolo", Messages.WidgetProperties_ShowLoLo);

    private static class CustomConfigurator extends WidgetConfigurator
    {
        public CustomConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            if (xml_version.getMajor() < 2)
            {
                ScrollBarWidget.IncrementConfigurator.handleLegacyIncrement(widget, xml);

                final ScaledSliderWidget slider = (ScaledSliderWidget) widget;
                XMLUtil.getChildDouble(xml, "level_lo").ifPresent(value -> slider.level_low.setValue(value));
                XMLUtil.getChildDouble(xml, "level_hi").ifPresent(value -> slider.level_high.setValue(value));
                XMLUtil.getChildBoolean(xml, "show_lo").ifPresent(show -> slider.show_low.setValue(show));
                XMLUtil.getChildBoolean(xml, "show_hi").ifPresent(show -> slider.show_high.setValue(show));
                XMLUtil.getChildBoolean(xml, "transparent_background").ifPresent(trans -> slider.transparent.setValue(trans));

                // There used to be another 'show_markers' to override the individual show_low/hi/.. settings
                final Optional<Boolean> show_markers = XMLUtil.getChildBoolean(xml, "show_markers");
                boolean hide_markers = show_markers.isPresent()  &&  show_markers.get() == false;

                // 'No scale' also used to imply no markers
                if (slider.propShowScale().getValue() == false)
                    hide_markers = true;

                if (hide_markers)
                {
                    slider.propShowLoLo().setValue(false);
                    slider.propShowLow().setValue(false);
                    slider.propShowHigh().setValue(false);
                    slider.propShowHiHi().setValue(false);
                }

                final Element element = XMLUtil.getChildElement(xml, "scale_font");
                if (element != null)
                    slider.font.readFromXML(model_reader, element);

                // Is this a legacy 'Knob'?
                if (xml.getAttribute("typeId").equals("org.csstudio.opibuilder.widgets.knob"))
                {
                    // Guess orientation based on width : height ratio
                    slider.propHorizontal().setValue(slider.propWidth().getValue() >= slider.propHeight().getValue());
                }
            }
            return true;
        }
    };

    private volatile WidgetProperty<Boolean> horizontal;
    private volatile WidgetProperty<Double> increment;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<Boolean> transparent;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<Double> minimum;
    private volatile WidgetProperty<Double> maximum;
    private volatile WidgetProperty<Boolean> limits_from_pv;
    private volatile WidgetProperty<Boolean> enabled;
    private volatile WidgetProperty<Integer> major_tick_step_hint;
    private volatile WidgetProperty<Boolean> show_scale;
    private volatile WidgetProperty<Boolean> show_minor_ticks;
    private volatile WidgetProperty<String> scale_format;
    private volatile WidgetProperty<Double> level_high;
    private volatile WidgetProperty<Double> level_hihi;
    private volatile WidgetProperty<Double> level_low;
    private volatile WidgetProperty<Double> level_lolo;
    private volatile WidgetProperty<Boolean> show_high;
    private volatile WidgetProperty<Boolean> show_hihi;
    private volatile WidgetProperty<Boolean> show_low;
    private volatile WidgetProperty<Boolean> show_lolo;
    private volatile RuntimeEventProperty configure;

    public ScaledSliderWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 400, 55);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(horizontal = propHorizontal.createProperty(this, true));
        properties.add(increment = propIncrement.createProperty(this, 1.0));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(transparent = propTransparent.createProperty(this, true));
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(minimum = propMinimum.createProperty(this, 0.0));
        properties.add(maximum = propMaximum.createProperty(this, 100.0));
        properties.add(limits_from_pv = propLimitsFromPV.createProperty(this, true));
        properties.add(enabled = propEnabled.createProperty(this, true));
        properties.add(show_scale = propShowScale.createProperty(this, true));
        properties.add(show_minor_ticks = propShowMinorTicks.createProperty(this, true));
        properties.add(major_tick_step_hint = propMajorTickStepHint.createProperty(this, 40));
        properties.add(scale_format = propScaleFormat.createProperty(this, "#.##"));
        properties.add(level_hihi = propLevelHiHi.createProperty(this, 90.0));
        properties.add(level_high = propLevelHigh.createProperty(this, 80.0));
        properties.add(level_low = propLevelLow.createProperty(this, 20.0));
        properties.add(level_lolo = propLevelLoLo.createProperty(this, 10.0));
        properties.add(show_hihi = propShowHiHi.createProperty(this, true));
        properties.add(show_high = propShowHigh.createProperty(this, true));
        properties.add(show_low = propShowLow.createProperty(this, true));
        properties.add(show_lolo = propShowLoLo.createProperty(this, true));
        properties.add(configure = (RuntimeEventProperty) runtimePropConfigure.createProperty(this, null));
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version) throws Exception
    {
        return new CustomConfigurator(persisted_version);
    }

    /** @return 'horizontal' property */
    public WidgetProperty<Boolean> propHorizontal()
    {
        return horizontal;
    }

    /** @return 'increment' property */
    public WidgetProperty<Double> propIncrement()
    {
        return increment;
    }

    /** @return 'foreground' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'background' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'transparent' property */
    public WidgetProperty<Boolean> propTransparent()
    {
        return transparent;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
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

    /** @return 'limits_from_pv' property */
    public WidgetProperty<Boolean> propLimitsFromPV()
    {
        return limits_from_pv;
    }

    /** @return 'enabled' property */
    public WidgetProperty<Boolean> propEnabled()
    {
        return enabled;
    }

    /** @return 'show_scale' property */
    public WidgetProperty<Boolean> propShowScale()
    {
        return show_scale;
    }

    /** @return 'major_tick_step_hint' property */
    public WidgetProperty<Integer> propMajorTickStepHint()
    {
        return major_tick_step_hint;
    }

    /** @return 'show_minor_ticks' property */
    public WidgetProperty<Boolean> propShowMinorTicks()
    {
        return show_minor_ticks;
    }

    /** @return 'scale_format' property */
    public WidgetProperty<String> propScaleFormat()
    {
        return scale_format;
    }

    /** @return 'level_hi' property */
    public WidgetProperty<Double> propLevelHi()
    {
        return level_high;
    }

    /** @return 'level_hihi' property */
    public WidgetProperty<Double> propLevelHiHi()
    {
        return level_hihi;
    }

    /** @return 'level_lo' property */
    public WidgetProperty<Double> propLevelLo()
    {
        return level_low;
    }

    /** @return 'level_lolo' property */
    public WidgetProperty<Double> propLevelLoLo()
    {
        return level_lolo;
    }

    /** @return 'show_high' property */
    public WidgetProperty<Boolean> propShowHigh()
    {
        return show_high;
    }

    /** @return 'show_hihi' property */
    public WidgetProperty<Boolean> propShowHiHi()
    {
        return show_hihi;
    }

    /** @return 'show_low' property */
    public WidgetProperty<Boolean> propShowLow()
    {
        return show_low;
    }

    /** @return 'show_lolo' property */
    public WidgetProperty<Boolean> propShowLoLo()
    {
        return show_lolo;
    }

    /** @return 'configure' property */
    public RuntimeEventProperty runtimePropConfigure()
    {
        return configure;
    }
}