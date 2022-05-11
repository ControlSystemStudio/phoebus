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

    /** Widget descriptor */
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

    /** Design */
    public enum Design {
        /** Design option */
        AMBER,
        /** Design option */
        BEIGE,
        /** Design option */
        BLACK,
        /** Design option */
        BLACK_RED,
        /** Design option */
        BLACK_YELLOW,
        /** Design option */
        BLUE,
        /** Design option */
        BLUE2,
        /** Design option */
        BLUE_BLACK,
        /** Design option */
        BLUE_BLUE,
        /** Design option */
        BLUE_DARKBLUE,
        /** Design option */
        BLUE_GRAY,
        /** Design option */
        BLUE_LIGHTBLUE,
        /** Design option */
        BLUE_LIGHTBLUE2,
        /** Design option */
        DARKAMBER,
        /** Design option */
        DARKBLUE,
        /** Design option */
        DARKGREEN,
        /** Design option */
        DARKPURPLE,
        /** Design option */
        GRAY,
        /** Design option */
        GRAY_PURPLE,
        /** Design option */
        GREEN,
        /** Design option */
        GREEN_BLACK,
        /** Design option */
        GREEN_DARKGREEN,
        /** Design option */
        LIGHTBLUE,
        /** Design option */
        LIGHTGREEN,
        /** Design option */
        LIGHTGREEN_BLACK,
        /** Design option */
        ORANGE,
        /** Design option */
        PURPLE,
        /** Design option */
        RED,
        /** Design option */
        RED_DARKRED,
        /** Design option */
        SECTIONS,
        /** Design option */
        STANDARD,
        /** Design option */
        STANDARD_GREEN,
        /** Design option */
        WHITE,
        /** Design option */
        YELLOW,
        /** Design option */
        YELLOW_BLACK,
        /** Design option */
        YOCTOPUCE
    }

    /** Font */
    public enum LCDFont {
        /** Font option */
        DIGITAL,
        /** Font option */
        DIGITAL_BOLD,
        /** Font option */
        ELEKTRA,
        /** Font option */
        LCD,
        /** Font option */
        STANDARD
    }

    /** Property */
    public static final WidgetPropertyDescriptor<Design>  propLcdDesign         = new WidgetPropertyDescriptor<Design> (WidgetPropertyCategory.WIDGET, "lcd_design",          Messages.WidgetProperties_LcdDesign) {
        @Override
        public EnumWidgetProperty<Design> createProperty ( Widget widget, Design defaultValue ) {
            return new EnumWidgetProperty<>(this, widget, defaultValue);
        }
    };
    /** Property */
    public static final WidgetPropertyDescriptor<LCDFont> propLcdFont           = new WidgetPropertyDescriptor<LCDFont>(WidgetPropertyCategory.WIDGET, "lcd_font",            Messages.WidgetProperties_LcdFont) {
        @Override
        public EnumWidgetProperty<LCDFont> createProperty ( Widget widget, LCDFont defaultValue ) {
            return new EnumWidgetProperty<>(this, widget, defaultValue);
        }
    };

    /** Property */
    public static final WidgetPropertyDescriptor<Boolean> propLcdCrystalEnabled = newBooleanPropertyDescriptor         (WidgetPropertyCategory.MISC,   "lcd_crystal_enabled", Messages.WidgetProperties_LcdCrystalEnabled);

    private volatile WidgetProperty<Boolean> lcdCrystalEnabled;
    private volatile WidgetProperty<Design>  lcdDesign;
    private volatile WidgetProperty<LCDFont> lcdFont;

    /** Constructor */
    public DigitalClockWidget ( ) {
        super(WIDGET_DESCRIPTOR.getType(), 170, 90);
    }

    /** @return Property */
    public WidgetProperty<Boolean> propLcdCrystalEnabled ( ) {
        return lcdCrystalEnabled;
    }

    /** @return Property */
    public WidgetProperty<Design> propLcdDesign ( ) {
        return lcdDesign;
    }

    /** @return Property */
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
