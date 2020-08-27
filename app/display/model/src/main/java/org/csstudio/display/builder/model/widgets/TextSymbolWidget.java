/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newIntegerPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontalAlignment;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propRotationStep;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propVerticalAlignment;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propWrapWords;

import java.util.Collections;
import java.util.List;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.RotationStep;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;

/**
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 3 Aug 2017
 */
public class TextSymbolWidget extends PVWidget {

    public static final WidgetDescriptor WIDGET_DESCRIPTOR = new WidgetDescriptor(
        "text-symbol",
        WidgetCategory.MONITOR,
        "Text Symbol",
        "/icons/text-symbol.png",
        "A container of text symbols displayed depending of the value of a PV"
    ) {
        @Override
        public Widget createWidget ( ) {
            return new TextSymbolWidget();
        }
    };

    public static final WidgetPropertyDescriptor<Integer>                       propArrayIndex = newIntegerPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "array_index", Messages.WidgetProperties_ArrayIndex, 0, Integer.MAX_VALUE);

    /** 'symbol' property: element for list of 'symbols' property */
    private static final WidgetPropertyDescriptor<String>                       propSymbol     = newStringPropertyDescriptor (WidgetPropertyCategory.WIDGET,   "symbol",      Messages.WidgetProperties_Symbol);

    /** 'items' property: list of items (string properties) for combo box */
    public static final ArrayWidgetProperty.Descriptor<WidgetProperty<String> > propSymbols    = new ArrayWidgetProperty.Descriptor< WidgetProperty<String> >(
        WidgetPropertyCategory.WIDGET,
        "symbols",
        Messages.WidgetProperties_Symbols,
        (widget, index) -> propSymbol.createProperty(widget, "\u263A")
    );

    private static final WidgetPropertyDescriptor<String>                       runtimePropSymbolValue = newStringPropertyDescriptor(WidgetPropertyCategory.RUNTIME, "symbol_value", Messages.WidgetProperties_SymbolValue);

    private volatile WidgetProperty<Integer>                     array_index;
    private volatile WidgetProperty<WidgetColor>                 background;
    private volatile WidgetProperty<Boolean>                     enabled;
    private volatile WidgetProperty<WidgetFont>                  font;
    private volatile WidgetProperty<WidgetColor>                 foreground;
    private volatile WidgetProperty<HorizontalAlignment>         horizontal_alignment;
    private volatile WidgetProperty<RotationStep>                rotation_step;
    private volatile ArrayWidgetProperty<WidgetProperty<String>> symbols;
    private volatile WidgetProperty<Boolean>                     transparent;
    private volatile WidgetProperty<VerticalAlignment>           vertical_alignment;
    private volatile WidgetProperty<Boolean>                     wrap_words;
    private volatile WidgetProperty<String>                      symbol_value;

    public TextSymbolWidget ( ) {
        super(WIDGET_DESCRIPTOR.getType(), 32, 32);
    }

    public WidgetProperty<Integer> propArrayIndex ( ) {
        return array_index;
    }

    public WidgetProperty<WidgetColor> propBackgroundColor ( ) {
        return background;
    }

    public WidgetProperty<Boolean> propEnabled ( ) {
        return enabled;
    }

    public WidgetProperty<WidgetFont> propFont ( ) {
        return font;
    }

    public WidgetProperty<WidgetColor> propForegroundColor ( ) {
        return foreground;
    }

    public WidgetProperty<HorizontalAlignment> propHorizontalAlignment ( ) {
        return horizontal_alignment;
    }

    public WidgetProperty<RotationStep> propRotationStep ( ) {
        return rotation_step;
    }

    public ArrayWidgetProperty<WidgetProperty<String>> propSymbols ( ) {
        return symbols;
    }

    public WidgetProperty<Boolean> propTransparent ( ) {
        return transparent;
    }

    public WidgetProperty<VerticalAlignment> propVerticalAlignment ( ) {
        return vertical_alignment;
    }

    public WidgetProperty<Boolean> propWrapWords ( ) {
        return wrap_words;
    }

    public WidgetProperty<String> runtimePropSymbolValue ( ) {
        return symbol_value;
    }

    @Override
    protected void defineProperties ( final List<WidgetProperty<?>> properties ) {

        super.defineProperties(properties);

        properties.add(symbols              = propSymbols.createProperty(this, Collections.emptyList()));

        properties.add(font                 = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground           = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background           = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(transparent          = propTransparent.createProperty(this, true));
        properties.add(horizontal_alignment = propHorizontalAlignment.createProperty(this, HorizontalAlignment.CENTER));
        properties.add(vertical_alignment   = propVerticalAlignment.createProperty(this, VerticalAlignment.MIDDLE));
        properties.add(rotation_step        = propRotationStep.createProperty(this, RotationStep.NONE));
        properties.add(wrap_words           = propWrapWords.createProperty(this, true));

        properties.add(array_index          = propArrayIndex.createProperty(this, 0));
        properties.add(enabled              = propEnabled.createProperty(this, true));

        properties.add(symbol_value         = runtimePropSymbolValue.createProperty(this, ""));

    }

    @Override
    protected String getInitialTooltip()
    {
        // Show the symbol value too
        return super.getInitialTooltip() + "\n$(symbol_value)";
    }
}
