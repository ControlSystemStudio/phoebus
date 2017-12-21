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
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newColorPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newIntegerPropertyDescriptor;

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
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;


/**
 * Widget displaying and editing a numeric PV value.
 *
 * @author Claudio Rosati, European Spallation Source ERIC
 * @version 1.0.0 25 Jan 2017
 */
public class MeterWidget extends BaseMeterWidget {

    public static final WidgetDescriptor WIDGET_DESCRIPTOR = new WidgetDescriptor(
        "meter",
        WidgetCategory.MONITOR,
        "Meter",
        "platform:/plugin/org.csstudio.display.builder.model/icons/meter.png",
        "Meter that can read a numeric PV",
        Arrays.asList(
            "org.csstudio.opibuilder.widgets.gauge",
            "org.csstudio.opibuilder.widgets.meter"
        )
    ) {
        @Override
        public Widget createWidget ( ) {
            return new MeterWidget();
        }
    };

    public enum KnobPosition {
        BOTTOM_CENTER,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER,
        CENTER_LEFT,
        CENTER_RIGHT,
        TOP_CENTER,
        TOP_LEFT,
        TOP_RIGHT
    }

    public enum Skin {
        GAUGE,
        HORIZONTAL,
        QUARTER,
        THREE_QUARTERS,
        VERTICAL
    }

    public enum ScaleDirection {
        CLOCKWISE,
        COUNTER_CLOCKWISE
    }

    public static final WidgetPropertyDescriptor<Skin>           propSkin                = new WidgetPropertyDescriptor<Skin>          (WidgetPropertyCategory.WIDGET,   "skin",                   Messages.WidgetProperties_Skin) {
        @Override
        public EnumWidgetProperty<Skin> createProperty ( Widget widget, Skin defaultValue ) {
            return new EnumWidgetProperty<>(this, widget, defaultValue);
        }
    };
    public static final WidgetPropertyDescriptor<KnobPosition>   propKnobPosition        = new WidgetPropertyDescriptor<KnobPosition>  (WidgetPropertyCategory.WIDGET,   "knob_position",          Messages.WidgetProperties_KnobPosition) {
        @Override
        public EnumWidgetProperty<KnobPosition> createProperty ( Widget widget, KnobPosition defaultValue ) {
            return new EnumWidgetProperty<>(this, widget, defaultValue);
        }
    };

    public static final WidgetPropertyDescriptor<Boolean>        propAverage             = newBooleanPropertyDescriptor                (WidgetPropertyCategory.MISC,     "average",                Messages.WidgetProperties_Average);
    public static final WidgetPropertyDescriptor<WidgetColor>    propAverageColor        = newColorPropertyDescriptor                  (WidgetPropertyCategory.MISC,     "average_color",          Messages.WidgetProperties_AverageColor);
    public static final WidgetPropertyDescriptor<Integer>        propAverageSamples      = newIntegerPropertyDescriptor                (WidgetPropertyCategory.MISC,     "average_samples",        Messages.WidgetProperties_AverageSamples, 1, 1000);
    public static final WidgetPropertyDescriptor<WidgetColor>    propKnobColor           = newColorPropertyDescriptor                  (WidgetPropertyCategory.MISC,     "knob_color",             Messages.WidgetProperties_KnobColor);
    public static final WidgetPropertyDescriptor<Boolean>        propMajorTickVisible    = newBooleanPropertyDescriptor                (WidgetPropertyCategory.MISC,     "major_tick_visible",     Messages.WidgetProperties_MajorTickVisible);
    public static final WidgetPropertyDescriptor<Boolean>        propMediumTickVisible   = newBooleanPropertyDescriptor                (WidgetPropertyCategory.MISC,     "medium_tick_visible",    Messages.WidgetProperties_MediumTickVisible);
    public static final WidgetPropertyDescriptor<Boolean>        propMinorTickVisible    = newBooleanPropertyDescriptor                (WidgetPropertyCategory.MISC,     "minor_tick_visible",     Messages.WidgetProperties_MinorTickVisible);
    public static final WidgetPropertyDescriptor<WidgetColor>    propNeedleColor         = newColorPropertyDescriptor                  (WidgetPropertyCategory.MISC,     "needle_color",           Messages.WidgetProperties_NeedleColor);
    public static final WidgetPropertyDescriptor<Boolean>        propOnlyExtremaVisible  = newBooleanPropertyDescriptor                (WidgetPropertyCategory.MISC,     "only_extrema_visible",   Messages.WidgetProperties_OnlyExtremaVisible);
    public static final WidgetPropertyDescriptor<ScaleDirection> propScaleDirection      = new WidgetPropertyDescriptor<ScaleDirection>(WidgetPropertyCategory.MISC,     "scale_direction",        Messages.WidgetProperties_ScaleDirection) {
        @Override
        public EnumWidgetProperty<ScaleDirection> createProperty ( Widget widget, ScaleDirection defaultValue ) {
            return new EnumWidgetProperty<>(this, widget, defaultValue);
        }
    };
    public static final WidgetPropertyDescriptor<Integer>        propTickLabelDecimals   = newIntegerPropertyDescriptor                (WidgetPropertyCategory.MISC,     "tick_label_decimals",    Messages.WidgetProperties_TickLabelDecimals, 0, 3);
    public static final WidgetPropertyDescriptor<Boolean>        propTickLabelsVisible   = newBooleanPropertyDescriptor                (WidgetPropertyCategory.MISC,     "tick_labels_visible",    Messages.WidgetProperties_TickLabelsVisible);
    public static final WidgetPropertyDescriptor<Boolean>        propTickMarkRingVisible = newBooleanPropertyDescriptor                (WidgetPropertyCategory.MISC,     "tick_mark_ring_visible", Messages.WidgetProperties_TickMarkRingVisible);

    private volatile WidgetProperty<Boolean>        average;
    private volatile WidgetProperty<WidgetColor>    average_color;
    private volatile WidgetProperty<Integer>        average_samples;
    private volatile WidgetProperty<WidgetColor>    knob_color;
    private volatile WidgetProperty<KnobPosition>   knob_position;
    private volatile WidgetProperty<Boolean>        major_tick_visible;
    private volatile WidgetProperty<Boolean>        medium_tick_visible;
    private volatile WidgetProperty<Boolean>        minor_tick_visible;
    private volatile WidgetProperty<WidgetColor>    needle_color;
    private volatile WidgetProperty<Boolean>        only_extrema_visible;
    private volatile WidgetProperty<ScaleDirection> scale_direction;
    private volatile WidgetProperty<Skin>           skin;
    private volatile WidgetProperty<Integer>        tick_label_decimals;
    private volatile WidgetProperty<Boolean>        tick_labels_visible;
    private volatile WidgetProperty<Boolean>        tick_mark_ring_visible;

    public MeterWidget ( ) {
        super(WIDGET_DESCRIPTOR.getType(), 240, 120);
    }

    @Override
    public WidgetConfigurator getConfigurator ( final Version persistedVersion ) throws Exception {
        return new MeterConfigurator(persistedVersion);
    }

    public WidgetProperty<Boolean> propAverage ( ) {
        return average;
    }

    public WidgetProperty<WidgetColor> propAverageColor ( ) {
        return average_color;
    }

    public WidgetProperty<Integer> propAverageSamples ( ) {
        return average_samples;
    }

    public WidgetProperty<WidgetColor> propKnobColor ( ) {
        return knob_color;
    }

    public WidgetProperty<KnobPosition> propKnobPosition ( ) {
        return knob_position;
    }

    public WidgetProperty<Boolean> propMajorTickVisible ( ) {
        return major_tick_visible;
    }

    public WidgetProperty<Boolean> propMediumTickVisible ( ) {
        return medium_tick_visible;
    }

    public WidgetProperty<Boolean> propMinorTickVisible ( ) {
        return minor_tick_visible;
    }

    public WidgetProperty<WidgetColor> propNeedleColor ( ) {
        return needle_color;
    }

    public WidgetProperty<Boolean> propOnlyExtremaVisible ( ) {
        return only_extrema_visible;
    }

    public WidgetProperty<ScaleDirection> propScaleDirection ( ) {
        return scale_direction;
    }

    public WidgetProperty<Skin> propSkin ( ) {
        return skin;
    }

    public WidgetProperty<Integer> propTickLabelDecimals ( ) {
        return tick_label_decimals;
    }

    public WidgetProperty<Boolean> propTickLabelsVisible ( ) {
        return tick_labels_visible;
    }

    public WidgetProperty<Boolean> propTickMarkRingVisible ( ) {
        return tick_mark_ring_visible;
    }

    @Override
    protected void defineProperties ( final List<WidgetProperty<?>> properties ) {

        super.defineProperties(properties);

        properties.add(skin                   = propSkin.createProperty(this, Skin.HORIZONTAL));
        properties.add(knob_position          = propKnobPosition.createProperty(this, KnobPosition.BOTTOM_CENTER));

        properties.add(average                = propAverage.createProperty(this, false));
        properties.add(average_color          = propAverageColor.createProperty(this, new WidgetColor(13, 23, 251)));
        properties.add(average_samples        = propAverageSamples.createProperty(this, 100));
        properties.add(knob_color             = propKnobColor.createProperty(this, new WidgetColor(177, 166, 155)));
        properties.add(major_tick_visible     = propMajorTickVisible.createProperty(this, true));
        properties.add(medium_tick_visible    = propMediumTickVisible.createProperty(this, true));
        properties.add(minor_tick_visible     = propMinorTickVisible.createProperty(this, true));
        properties.add(needle_color           = propNeedleColor.createProperty(this, new WidgetColor(255, 5, 7)));
        properties.add(only_extrema_visible   = propOnlyExtremaVisible.createProperty(this, false));
        properties.add(scale_direction        = propScaleDirection.createProperty(this, ScaleDirection.CLOCKWISE));
        properties.add(tick_label_decimals    = propTickLabelDecimals.createProperty(this, 0));
        properties.add(tick_labels_visible    = propTickLabelsVisible.createProperty(this, true));
        properties.add(tick_mark_ring_visible = propTickMarkRingVisible.createProperty(this, false));

    }

    /**
     * Custom configurator to read legacy *.opi files.
     */
    protected static class MeterConfigurator extends BaseMeterConfigurator{

        public MeterConfigurator ( Version xmlVersion ) {
            super(xmlVersion);
        }

        @Override
        public boolean configureFromXML ( final ModelReader reader, final Widget widget, final Element xml ) throws Exception {

            if ( !super.configureFromXML(reader, widget, xml) ) {
                return false;
            }

            MeterWidget meter = (MeterWidget) widget;

            if ( xml_version.getMajor() < 2 ) {

                switch ( xml.getAttribute("typeId") ) {
                    case "org.csstudio.opibuilder.widgets.gauge":
                        meter.propSkin().setValue(Skin.GAUGE);
                        break;
                    case "org.csstudio.opibuilder.widgets.meter":
                    default:
                        meter.propSkin().setValue(Skin.HORIZONTAL);
                        break;
                }

                XMLUtil.getChildBoolean(xml, "show_minor_ticks").ifPresent(s -> {
                    meter.propMediumTickVisible().setValue(s);
                    meter.propMinorTickVisible().setValue(s);
                });
                XMLUtil.getChildBoolean(xml, "show_scale").ifPresent(s -> {
                    meter.propMajorTickVisible().setValue(s);
                    meter.propMediumTickVisible().setValue(s && meter.propMediumTickVisible().getValue());
                    meter.propMinorTickVisible().setValue(s && meter.propMinorTickVisible().getValue());
                    meter.propTickLabelsVisible().setValue(s);
                });
                XMLUtil.getChildBoolean(xml, "show_value_label").ifPresent(s -> meter.propValueVisible().setValue(s));

                meter.propHighlightZones().setValue(false);

            }

            return true;

        }

    }

}
