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
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;

import java.util.List;
import java.util.Locale;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;


/**
 * Widget displaying date and/or time.
 *
 * @author Claudio Rosati, European Spallation Source ERIC
 * @version 1.0.0 7 Feb 2017
 */
@SuppressWarnings("nls")
public abstract class BaseClockWidget extends VisibleWidget {

    /** 'date_visible' */
    public static final WidgetPropertyDescriptor<Boolean>     propDateVisible    = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "date_visible",    Messages.WidgetProperties_DateVisible);
    /** 'second_visible' */
    public static final WidgetPropertyDescriptor<Boolean>     propSecondVisible  = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "second_visible",  Messages.WidgetProperties_SecondVisible);
    /** 'title' */
    public static final WidgetPropertyDescriptor<String>      propTitle          = newStringPropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "title",           Messages.WidgetProperties_Title);
    /** 'title_visible' */
    public static final WidgetPropertyDescriptor<Boolean>     propTitleVisible   = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "title_visible",   Messages.WidgetProperties_TitleVisible);

    /** 'running' */
    public static final WidgetPropertyDescriptor<Boolean>     propRunning        = newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "running",         Messages.WidgetProperties_Running);

    /** 'locale' */
    public static final WidgetPropertyDescriptor<String>      propLocale         = newStringPropertyDescriptor (WidgetPropertyCategory.MISC,     "locale",          Messages.WidgetProperties_Locale);
    /** 'shadows_enabled' */
    public static final WidgetPropertyDescriptor<Boolean>     propShadowsEnabled = newBooleanPropertyDescriptor(WidgetPropertyCategory.MISC,     "shadows_enabled", Messages.WidgetProperties_ShadowsEnabled);

    private volatile WidgetProperty<Boolean>     dateVisible;
    private volatile WidgetProperty<String>      locale;
    private volatile WidgetProperty<Boolean>     running;
    private volatile WidgetProperty<Boolean>     secondVisible;
    private volatile WidgetProperty<Boolean>     shadowsEnabled;
    private volatile WidgetProperty<String>      title;
    private volatile WidgetProperty<Boolean>     titleVisible;

    /** @param type Type
     *  @param default_width Width
     *  @param default_height Height
     */
    public BaseClockWidget ( final String type, final int default_width, final int default_height ) {
        super(type, default_width, default_height);
    }

    /** @return 'date_visible' */
    public WidgetProperty<Boolean> propDateVisible ( ) {
        return dateVisible;
    }

    /** @return 'locale' */
    public WidgetProperty<String> propLocale ( ) {
        return locale;
    }

    /** @return 'running' */
    public WidgetProperty<Boolean> propRunning ( ) {
        return running;
    }

    /** @return 'second_visible' */
    public WidgetProperty<Boolean> propSecondVisible ( ) {
        return secondVisible;
    }

    /** @return 'shadows_enabled' */
    public WidgetProperty<Boolean> propShadowsEnabled ( ) {
        return shadowsEnabled;
    }

    /** @return 'title' */
    public WidgetProperty<String> propTitle ( ) {
        return title;
    }

    /** @return 'title_visible' */
    public WidgetProperty<Boolean> propTitleVisible ( ) {
        return titleVisible;
    }

    @Override
    protected void defineProperties ( final List<WidgetProperty<?>> properties ) {

        super.defineProperties(properties);

        properties.add(dateVisible    = propDateVisible.createProperty(this, false));
        properties.add(secondVisible  = propSecondVisible.createProperty(this, true));
        properties.add(title          = propTitle.createProperty(this, ""));
        properties.add(titleVisible   = propTitleVisible.createProperty(this, false));

        properties.add(locale         = propLocale.createProperty(this, Locale.getDefault().toLanguageTag()));
        properties.add(shadowsEnabled = propShadowsEnabled.createProperty(this, true));

        //  Properties not visible in the property sheet.
        running = propRunning.createProperty(this, true);

    }

}
