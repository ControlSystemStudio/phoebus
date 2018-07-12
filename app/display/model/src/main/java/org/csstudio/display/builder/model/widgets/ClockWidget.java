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
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newDoublePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;

import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;


/**
 * Widget displaying date and/or time.
 *
 * @author Claudio Rosati, European Spallation Source ERIC
 * @version 1.0.0 18 Jan 2017
 */
@SuppressWarnings("nls")
public class ClockWidget extends BaseClockWidget {

    public static final WidgetDescriptor WIDGET_DESCRIPTOR = new WidgetDescriptor(
        "clock",
        WidgetCategory.MISC,
        "Clock",
        "/icons/clock.png",
        "Simple clock"
    ) {
        @Override
        public Widget createWidget ( ) {
            return new ClockWidget();
        }
    };

    public enum Skin {
        CLOCK,
        DB,
        FAT,
        INDUSTRIAL,
        PEAR,
        PLAIN,
        SLIM,
        TILE,
        YOTA2
    }

    public static final WidgetPropertyDescriptor<Skin>        propSkin                  = new WidgetPropertyDescriptor<>(WidgetPropertyCategory.WIDGET,   "skin",                     Messages.WidgetProperties_Skin) {
        @Override
        public EnumWidgetProperty<Skin> createProperty ( Widget widget, Skin defaultValue ) {
            return new EnumWidgetProperty<>(this, widget, defaultValue);
        }
    };

    public static final WidgetPropertyDescriptor<Boolean>     propDiscreteHours         = newBooleanPropertyDescriptor      (WidgetPropertyCategory.BEHAVIOR, "discrete_hours",           Messages.WidgetProperties_DiscreteHours);
    public static final WidgetPropertyDescriptor<Boolean>     propDiscreteMinutes       = newBooleanPropertyDescriptor      (WidgetPropertyCategory.BEHAVIOR, "discrete_minutes",         Messages.WidgetProperties_DiscreteMinutes);
    public static final WidgetPropertyDescriptor<Boolean>     propDiscreteSeconds       = newBooleanPropertyDescriptor      (WidgetPropertyCategory.BEHAVIOR, "discrete_seconds",         Messages.WidgetProperties_DiscreteSeconds);

    public static final WidgetPropertyDescriptor<WidgetColor> propDateColor             = newColorPropertyDescriptor        (WidgetPropertyCategory.MISC,     "date_color",               Messages.WidgetProperties_DateColor);
    public static final WidgetPropertyDescriptor<WidgetColor> propHourColor             = newColorPropertyDescriptor        (WidgetPropertyCategory.MISC,     "hour_color",               Messages.WidgetProperties_HourColor);
    public static final WidgetPropertyDescriptor<WidgetColor> propHourTickMarkColor     = newColorPropertyDescriptor        (WidgetPropertyCategory.MISC,     "hour_tick_mark_color",     Messages.WidgetProperties_HourTickMarkColor);
    public static final WidgetPropertyDescriptor<Boolean>     propHourTickMarkVisible   = newBooleanPropertyDescriptor      (WidgetPropertyCategory.MISC,     "hour_tick_mark_visible",   Messages.WidgetProperties_HourTickMarkVisible);
    public static final WidgetPropertyDescriptor<WidgetColor> propKnobColor             = newColorPropertyDescriptor        (WidgetPropertyCategory.MISC,     "knob_color",               Messages.WidgetProperties_KnobColor);
    public static final WidgetPropertyDescriptor<WidgetColor> propMinuteColor           = newColorPropertyDescriptor        (WidgetPropertyCategory.MISC,     "minute_color",             Messages.WidgetProperties_MinuteColor);
    public static final WidgetPropertyDescriptor<WidgetColor> propMinuteTickMarkColor   = newColorPropertyDescriptor        (WidgetPropertyCategory.MISC,     "minute_tick_mark_color",   Messages.WidgetProperties_MinuteTickMarkColor);
    public static final WidgetPropertyDescriptor<Boolean>     propMinuteTickMarkVisible = newBooleanPropertyDescriptor      (WidgetPropertyCategory.MISC,     "minute_tick_mark_visible", Messages.WidgetProperties_MinuteTickMarkVisible);
    public static final WidgetPropertyDescriptor<WidgetColor> propRingColor             = newColorPropertyDescriptor        (WidgetPropertyCategory.MISC,     "ring_color",               Messages.WidgetProperties_RingColor);
    public static final WidgetPropertyDescriptor<Double>      propRingWidth             = newDoublePropertyDescriptor       (WidgetPropertyCategory.MISC,     "ring_width",               Messages.WidgetProperties_RingWidth);
    public static final WidgetPropertyDescriptor<WidgetColor> propSecondColor           = newColorPropertyDescriptor        (WidgetPropertyCategory.MISC,     "second_color",             Messages.WidgetProperties_SecondColor);
    public static final WidgetPropertyDescriptor<WidgetColor> propTextColor             = newColorPropertyDescriptor        (WidgetPropertyCategory.MISC,     "text_color",               Messages.WidgetProperties_TextColor);
    public static final WidgetPropertyDescriptor<Boolean>     propTextVisible           = newBooleanPropertyDescriptor      (WidgetPropertyCategory.MISC,     "text_visible",             Messages.WidgetProperties_TextVisible);
    public static final WidgetPropertyDescriptor<WidgetColor> propTickLabelColor        = newColorPropertyDescriptor        (WidgetPropertyCategory.MISC,     "tick_label_color",         Messages.WidgetProperties_TickLabelColor);
    public static final WidgetPropertyDescriptor<Boolean>     propTickLabelsVisible     = newBooleanPropertyDescriptor      (WidgetPropertyCategory.MISC,     "tick_labels_visible",      Messages.WidgetProperties_TickLabelsVisible);
    public static final WidgetPropertyDescriptor<WidgetColor> propTitleColor            = newColorPropertyDescriptor        (WidgetPropertyCategory.MISC,     "title_color",              Messages.WidgetProperties_TitleColor);

    private volatile WidgetProperty<WidgetColor> background_color;
    private volatile WidgetProperty<WidgetColor> ringColor;
    private volatile WidgetProperty<Double>      ringWidth;
    private volatile WidgetProperty<WidgetColor> dateColor;
    private volatile WidgetProperty<Boolean>     discreteHours;
    private volatile WidgetProperty<Boolean>     discreteMinutes;
    private volatile WidgetProperty<Boolean>     discreteSeconds;
    private volatile WidgetProperty<WidgetColor> hourColor;
    private volatile WidgetProperty<WidgetColor> hourTickMarkColor;
    private volatile WidgetProperty<Boolean>     hourTickMarkVisible;
    private volatile WidgetProperty<WidgetColor> knobColor;
    private volatile WidgetProperty<WidgetColor> minuteColor;
    private volatile WidgetProperty<WidgetColor> minuteTickMarkColor;
    private volatile WidgetProperty<Boolean>     minuteTickMarkVisible;
    private volatile WidgetProperty<WidgetColor> secondColor;
    private volatile WidgetProperty<Skin>        skin;
    private volatile WidgetProperty<WidgetColor> textColor;
    private volatile WidgetProperty<Boolean>     textVisible;
    private volatile WidgetProperty<WidgetColor> tickLabelColor;
    private volatile WidgetProperty<Boolean>     tickLabelsVisible;
    private volatile WidgetProperty<WidgetColor> titleColor;
    private volatile WidgetProperty<Boolean>     transparent;

    public ClockWidget ( ) {
        super(WIDGET_DESCRIPTOR.getType(), 120, 120);
    }

    public WidgetProperty<WidgetColor> propBackgroundColor ( ) {
        return background_color;
    }

    public WidgetProperty<WidgetColor> propDateColor ( ) {
        return dateColor;
    }

    public WidgetProperty<Boolean> propDiscreteHours ( ) {
        return discreteHours;
    }

    public WidgetProperty<Boolean> propDiscreteMinutes ( ) {
        return discreteMinutes;
    }

    public WidgetProperty<Boolean> propDiscreteSeconds ( ) {
        return discreteSeconds;
    }

    public WidgetProperty<WidgetColor> propHourColor ( ) {
        return hourColor;
    }

    public WidgetProperty<WidgetColor> propHourTickMarkColor ( ) {
        return hourTickMarkColor;
    }

    public WidgetProperty<Boolean> propHourTickMarkVisible ( ) {
        return hourTickMarkVisible;
    }

    public WidgetProperty<WidgetColor> propKnobColor ( ) {
        return knobColor;
    }

    public WidgetProperty<WidgetColor> propMinuteColor ( ) {
        return minuteColor;
    }

    public WidgetProperty<WidgetColor> propMinuteTickMarkColor ( ) {
        return minuteTickMarkColor;
    }

    public WidgetProperty<Boolean> propMinuteTickMarkVisible ( ) {
        return minuteTickMarkVisible;
    }

    public WidgetProperty<WidgetColor> propRingColor ( ) {
        return ringColor;
    }

    public WidgetProperty<Double> propRingWidth ( ) {
        return ringWidth;
    }

    public WidgetProperty<WidgetColor> propSecondColor ( ) {
        return secondColor;
    }

    public WidgetProperty<Skin> propSkin ( ) {
        return skin;
    }

    public WidgetProperty<WidgetColor> propTextColor ( ) {
        return textColor;
    }

    public WidgetProperty<Boolean> propTextVisible ( ) {
        return textVisible;
    }

    public WidgetProperty<WidgetColor> propTickLabelColor ( ) {
        return tickLabelColor;
    }

    public WidgetProperty<Boolean> propTickLabelsVisible ( ) {
        return tickLabelsVisible;
    }

    public WidgetProperty<WidgetColor> propTitleColor ( ) {
        return titleColor;
    }

    public WidgetProperty<Boolean> propTransparent ( ) {
        return transparent;
    }

    @Override
    protected void defineProperties ( final List<WidgetProperty<?>> properties ) {

        super.defineProperties(properties);

        properties.add(skin                  = propSkin.createProperty(this, Skin.PLAIN));

        properties.add(background_color      = propBackgroundColor.createProperty(this, new WidgetColor(230, 230, 153)));
        properties.add(transparent           = propTransparent.createProperty(this, false));

        properties.add(discreteHours         = propDiscreteHours.createProperty(this, false));
        properties.add(discreteMinutes       = propDiscreteMinutes.createProperty(this, false));
        properties.add(discreteSeconds       = propDiscreteSeconds.createProperty(this, false));

        properties.add(dateColor             = propDateColor.createProperty(this, new WidgetColor(102, 51, 102)));
        properties.add(hourColor             = propHourColor.createProperty(this, new WidgetColor(255, 127, 80)));
        properties.add(hourTickMarkColor     = propHourTickMarkColor.createProperty(this, new WidgetColor(196, 127, 80)));
        properties.add(hourTickMarkVisible   = propHourTickMarkVisible.createProperty(this, true));
        properties.add(knobColor             = propKnobColor.createProperty(this, new WidgetColor(196, 127, 80)));
        properties.add(minuteColor           = propMinuteColor.createProperty(this, new WidgetColor(255, 136, 98)));
        properties.add(minuteTickMarkColor   = propMinuteTickMarkColor.createProperty(this, new WidgetColor(196, 136, 98)));
        properties.add(minuteTickMarkVisible = propMinuteTickMarkVisible.createProperty(this, true));
        properties.add(ringColor             = propRingColor.createProperty(this, new WidgetColor(153, 230, 230)));
        properties.add(ringWidth             = propRingWidth.createProperty(this, 0.0));
        properties.add(secondColor           = propSecondColor.createProperty(this, new WidgetColor(98, 196, 136)));
        properties.add(textColor             = propTextColor.createProperty(this, new WidgetColor(136, 196, 136)));
        properties.add(textVisible           = propTextVisible.createProperty(this, false));
        properties.add(tickLabelColor        = propTickLabelColor.createProperty(this, new WidgetColor(196, 136, 136)));
        properties.add(tickLabelsVisible     = propTickLabelsVisible.createProperty(this, true));
        properties.add(titleColor            = propTitleColor.createProperty(this, new WidgetColor(136, 196, 136)));

    }

}
