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

import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;


/**
 * Widget displaying date and/or time.
 *
 * @author Claudio Rosati, European Spallation Source ERIC
 * @version 1.0.0 23 Jan 2017
 */
@SuppressWarnings("nls")
public class DigitalClockWidget extends BaseClockWidget {

    public static final WidgetDescriptor WIDGET_DESCRIPTOR = new WidgetDescriptor(
        "digital_clock",
        WidgetCategory.MISC,
        "DigitalClock",
        "/icons/digital-clock.png",
        "Digital clock"
    ) {
        @Override
        public Widget createWidget ( ) {
            return new DigitalClockWidget();
        }
    };

    public enum Design {
        AMBER,
        BEIGE,
        BLACK,
        BLACK_RED,
        BLACK_YELLOW,
        BLUE,
        BLUE2,
        BLUE_BLACK,
        BLUE_BLUE,
        BLUE_DARKBLUE,
        BLUE_GRAY,
        BLUE_LIGHTBLUE,
        BLUE_LIGHTBLUE2,
        DARKAMBER,
        DARKBLUE,
        DARKGREEN,
        DARKPURPLE,
        GRAY,
        GRAY_PURPLE,
        GREEN,
        GREEN_BLACK,
        GREEN_DARKGREEN,
        LIGHTBLUE,
        LIGHTGREEN,
        LIGHTGREEN_BLACK,
        ORANGE,
        PURPLE,
        RED,
        RED_DARKRED,
        SECTIONS,
        STANDARD,
        STANDARD_GREEN,
        WHITE,
        YELLOW,
        YELLOW_BLACK,
        YOCTOPUCE
    }

    public enum LCDFont {
        DIGITAL,
        DIGITAL_BOLD,
        ELEKTRA,
        LCD,
        STANDARD
    }

    public static final WidgetPropertyDescriptor<Design>  propLcdDesign         = new WidgetPropertyDescriptor<Design> (WidgetPropertyCategory.WIDGET, "lcd_design",          Messages.WidgetProperties_LcdDesign) {
        @Override
        public EnumWidgetProperty<Design> createProperty ( Widget widget, Design defaultValue ) {
            return new EnumWidgetProperty<>(this, widget, defaultValue);
        }
    };
    public static final WidgetPropertyDescriptor<LCDFont> propLcdFont           = new WidgetPropertyDescriptor<LCDFont>(WidgetPropertyCategory.WIDGET, "lcd_font",            Messages.WidgetProperties_LcdFont) {
        @Override
        public EnumWidgetProperty<LCDFont> createProperty ( Widget widget, LCDFont defaultValue ) {
            return new EnumWidgetProperty<>(this, widget, defaultValue);
        }
    };

    public static final WidgetPropertyDescriptor<Boolean> propLcdCrystalEnabled = newBooleanPropertyDescriptor         (WidgetPropertyCategory.MISC,   "lcd_crystal_enabled", Messages.WidgetProperties_LcdCrystalEnabled);

    private volatile WidgetProperty<Boolean> lcdCrystalEnabled;
    private volatile WidgetProperty<Design>  lcdDesign;
    private volatile WidgetProperty<LCDFont> lcdFont;

    public DigitalClockWidget ( ) {
        super(WIDGET_DESCRIPTOR.getType(), 170, 90);
    }

    public WidgetProperty<Boolean> propLcdCrystalEnabled ( ) {
        return lcdCrystalEnabled;
    }

    public WidgetProperty<Design> propLcdDesign ( ) {
        return lcdDesign;
    }

    public WidgetProperty<LCDFont> propLcdFont ( ) {
        return lcdFont;
    }

    @Override
    protected void defineProperties ( final List<WidgetProperty<?>> properties ) {

        super.defineProperties(properties);

        properties.add(lcdDesign         = propLcdDesign.createProperty(this, Design.SECTIONS));
        properties.add(lcdFont           = propLcdFont.createProperty(this, LCDFont.DIGITAL_BOLD));

        properties.add(lcdCrystalEnabled = propLcdCrystalEnabled.createProperty(this, false));

    }

}
