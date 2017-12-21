/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newDoublePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLimitsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMaximum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMinimum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPrecision;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;

import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/**
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 8 Feb 2017
 */
public abstract class BaseGaugeWidget extends PVWidget {

    public static final WidgetPropertyDescriptor<Boolean>     propAutoScale      = newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "auto_scale",       Messages.WidgetProperties_AutoScale);
    public static final WidgetPropertyDescriptor<Boolean>     propValueVisible   = newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "value_visible",    Messages.WidgetProperties_ValueVisible);
    public static final WidgetPropertyDescriptor<Boolean>     propUnitFromPV     = newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "unit_from_pv",     Messages.WidgetProperties_UnitFromPV);

    public static final WidgetPropertyDescriptor<Double>      propLevelHiHi      = newDoublePropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "level_hihi",       Messages.WidgetProperties_LevelHiHi);
    public static final WidgetPropertyDescriptor<Double>      propLevelHigh      = newDoublePropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "level_high",       Messages.WidgetProperties_LevelHigh);
    public static final WidgetPropertyDescriptor<Double>      propLevelLoLo      = newDoublePropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "level_lolo",       Messages.WidgetProperties_LevelLoLo);
    public static final WidgetPropertyDescriptor<Double>      propLevelLow       = newDoublePropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "level_low",        Messages.WidgetProperties_LevelLow);
    public static final WidgetPropertyDescriptor<Boolean>     propShowHiHi       = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "show_hihi",        Messages.WidgetProperties_ShowHiHi);
    public static final WidgetPropertyDescriptor<Boolean>     propShowHigh       = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "show_high",        Messages.WidgetProperties_ShowHigh);
    public static final WidgetPropertyDescriptor<Boolean>     propShowLoLo       = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "show_lolo",        Messages.WidgetProperties_ShowLoLo);
    public static final WidgetPropertyDescriptor<Boolean>     propShowLow        = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "show_low",         Messages.WidgetProperties_ShowLow);
    public static final WidgetPropertyDescriptor<String>      propTitle          = newStringPropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "title",            Messages.WidgetProperties_Title);
    public static final WidgetPropertyDescriptor<String>      propUnit           = newStringPropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "unit",             Messages.WidgetProperties_Unit);

    public static final WidgetPropertyDescriptor<Double>      propMajorTickSpace = newDoublePropertyDescriptor (WidgetPropertyCategory.MISC,     "major_tick_space", Messages.WidgetProperties_MajorTickSpace);
    public static final WidgetPropertyDescriptor<Double>      propMinorTickSpace = newDoublePropertyDescriptor (WidgetPropertyCategory.MISC,     "minor_tick_space", Messages.WidgetProperties_MinorTickSpace);

    private volatile WidgetProperty<Boolean>     auto_scale;
    private volatile WidgetProperty<WidgetColor> background_color;
    private volatile WidgetProperty<Boolean>     enabled;
    private volatile WidgetProperty<WidgetColor> foreground_color;
    private volatile WidgetProperty<Double>      level_high;
    private volatile WidgetProperty<Double>      level_hihi;
    private volatile WidgetProperty<Double>      level_lolo;
    private volatile WidgetProperty<Double>      level_low;
    private volatile WidgetProperty<Boolean>     limits_from_pv;
    private volatile WidgetProperty<Double>      major_tick_space;
    private volatile WidgetProperty<Double>      maximum;
    private volatile WidgetProperty<Double>      minimum;
    private volatile WidgetProperty<Double>      minor_tick_space;
    private volatile WidgetProperty<Integer>     precision;
    private volatile WidgetProperty<Boolean>     show_high;
    private volatile WidgetProperty<Boolean>     show_hihi;
    private volatile WidgetProperty<Boolean>     show_lolo;
    private volatile WidgetProperty<Boolean>     show_low;
    private volatile WidgetProperty<String>      title;
    private volatile WidgetProperty<Boolean>     transparent;
    private volatile WidgetProperty<String>      unit;
    private volatile WidgetProperty<Boolean>     unit_from_pv;
    private volatile WidgetProperty<Boolean>     value_visible;

    /**
     * @param type Widget type.
     * @param default_width Default widget width.
     * @param default_height Default widget height.
     */
    public BaseGaugeWidget ( final String type, final int default_width, final int default_height ) {
        super(type, default_width, default_height);
    }

    @Override
    public WidgetConfigurator getConfigurator ( final Version persistedVersion ) throws Exception {
        return new BaseGaugeConfigurator(persistedVersion);
    }

    public WidgetProperty<Boolean> propAutoScale ( ) {
        return auto_scale;
    }

    public WidgetProperty<WidgetColor> propBackgroundColor ( ) {
        return background_color;
    }

    public WidgetProperty<Boolean> propEnabled ( ) {
        return enabled;
    }

    public WidgetProperty<WidgetColor> propForegroundColor ( ) {
        return foreground_color;
    }

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

    public WidgetProperty<Boolean> propLimitsFromPV ( ) {
        return limits_from_pv;
    }

    public WidgetProperty<Double> propMajorTickSpace ( ) {
        return major_tick_space;
    }

    public WidgetProperty<Double> propMaximum ( ) {
        return maximum;
    }

    public WidgetProperty<Double> propMinimum ( ) {
        return minimum;
    }

    public WidgetProperty<Double> propMinorTickSpace ( ) {
        return minor_tick_space;
    }

    public WidgetProperty<Integer> propPrecision ( ) {
        return precision;
    }

    public WidgetProperty<Boolean> propShowHiHi ( ) {
        return show_hihi;
    }

    public WidgetProperty<Boolean> propShowHigh ( ) {
        return show_high;
    }

    public WidgetProperty<Boolean> propShowLoLo ( ) {
        return show_lolo;
    }

    public WidgetProperty<Boolean> propShowLow ( ) {
        return show_low;
    }

    public WidgetProperty<String> propTitle ( ) {
        return title;
    }

    public WidgetProperty<Boolean> propTransparent ( ) {
        return transparent;
    }

    public WidgetProperty<String> propUnit ( ) {
        return unit;
    }

    public WidgetProperty<Boolean> propUnitFromPV ( ) {
        return unit_from_pv;
    }

    public WidgetProperty<Boolean> propValueVisible ( ) {
        return value_visible;
    }

    @Override
    protected void defineProperties ( final List<WidgetProperty<?>> properties ) {

        super.defineProperties(properties);

        properties.add(auto_scale       = propAutoScale.createProperty(this, true));

        properties.add(background_color = propBackgroundColor.createProperty(this, new WidgetColor(255, 254, 253)));
        properties.add(foreground_color = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(transparent      = propTransparent.createProperty(this, true));

        properties.add(precision        = propPrecision.createProperty(this, -1));
        properties.add(level_hihi       = propLevelHiHi.createProperty(this, 90.0));
        properties.add(level_high       = propLevelHigh.createProperty(this, 80.0));
        properties.add(level_low        = propLevelLow.createProperty(this, 20.0));
        properties.add(level_lolo       = propLevelLoLo.createProperty(this, 10.0));
        properties.add(show_hihi        = propShowHiHi.createProperty(this, true));
        properties.add(show_high        = propShowHigh.createProperty(this, true));
        properties.add(show_low         = propShowLow.createProperty(this, true));
        properties.add(show_lolo        = propShowLoLo.createProperty(this, true));
        properties.add(title            = propTitle.createProperty(this, ""));
        properties.add(unit             = propUnit.createProperty(this, ""));

        properties.add(minimum          = propMinimum.createProperty(this, 0.0));
        properties.add(maximum          = propMaximum.createProperty(this, 100.0));
        properties.add(limits_from_pv   = propLimitsFromPV.createProperty(this, true));
        properties.add(unit_from_pv     = propUnitFromPV.createProperty(this, true));
        properties.add(value_visible    = propValueVisible.createProperty(this, true));
        properties.add(enabled          = propEnabled.createProperty(this, true));

        properties.add(major_tick_space = propMajorTickSpace.createProperty(this, 10.0));
        properties.add(minor_tick_space = propMinorTickSpace.createProperty(this, 1.0));

    }

    /**
     * Custom configurator to read legacy *.opi files.
     */
    protected static class BaseGaugeConfigurator extends WidgetConfigurator {

        public BaseGaugeConfigurator ( Version xmlVersion ) {
            super(xmlVersion);
        }

        @Override
        public boolean configureFromXML ( final ModelReader reader, final Widget widget, final Element xml ) throws Exception {

            if ( !super.configureFromXML(reader, widget, xml) ) {
                return false;
            }

            if ( xml_version.getMajor() < 2 ) {

                BaseGaugeWidget gauge = (BaseGaugeWidget) widget;

                XMLUtil.getChildDouble(xml, "level_hi").ifPresent(v -> gauge.propLevelHigh().setValue(v));
                XMLUtil.getChildDouble(xml, "level_lo").ifPresent(v -> gauge.propLevelLow().setValue(v));
                XMLUtil.getChildBoolean(xml, "show_hi").ifPresent(s -> gauge.propShowHigh().setValue(s));
                XMLUtil.getChildBoolean(xml, "show_lo").ifPresent(s -> gauge.propShowLow().setValue(s));

                //  BOY meters are always opaque.
                gauge.propTransparent().setValue(false);

            }

            return true;

        }

    }

}
