/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.model.widgets;


import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newDoublePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newFilenamePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newIntegerPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
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
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/**
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 19 Jun 2017
 */
@SuppressWarnings("nls")
public class SymbolWidget extends PVWidget {

    /** Default symbol */
    public final static String DEFAULT_SYMBOL = "examples:/icons/default_symbol.png";

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR = new WidgetDescriptor(
        "symbol",
        WidgetCategory.MONITOR,
        "Symbol",
        "/icons/symbol.png",
        "A container of symbols displayed depending of the value of a PV",
        Arrays.asList(
            "org.csstudio.opibuilder.widgets.ImageBoolIndicator",
            "org.csstudio.opibuilder.widgets.symbol.bool.BoolMonitorWidget",
            "org.csstudio.opibuilder.widgets.symbol.multistate.MultistateMonitorWidget"
        )
    ) {
        @Override
        public Widget createWidget ( ) {
            return new SymbolWidget();
        }
    };

    /** Property */
    public static final WidgetPropertyDescriptor<Integer>                       propInitialIndex  = newIntegerPropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "initial_index",  Messages.WidgetProperties_InitialIndex, 0, Integer.MAX_VALUE);
    /** Property */
    public static final WidgetPropertyDescriptor<Boolean>                       propShowIndex     = newBooleanPropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "show_index",     Messages.WidgetProperties_ShowIndex);
    /** Property */
    public static final WidgetPropertyDescriptor<Double>                        propRotation      = newDoublePropertyDescriptor  (WidgetPropertyCategory.DISPLAY,  "rotation",       Messages.WidgetProperties_Rotation);
    private static final WidgetPropertyDescriptor<WidgetColor>                  propDisconnectOverlayColor = CommonWidgetProperties.newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "disconnect_overlay_color", Messages.WidgetProperties_DisconnectOverlayColor);

    /** Property */
    public static final WidgetPropertyDescriptor<Integer>                       propArrayIndex    = newIntegerPropertyDescriptor (WidgetPropertyCategory.BEHAVIOR, "array_index",    Messages.WidgetProperties_ArrayIndex, 0, Integer.MAX_VALUE);
    /** Property */
    public static final WidgetPropertyDescriptor<Boolean>                       propAutoSize      = newBooleanPropertyDescriptor (WidgetPropertyCategory.BEHAVIOR, "auto_size",      Messages.WidgetProperties_AutoSize);
    /** Property */
    public static final WidgetPropertyDescriptor<Boolean>                       propPreserveRatio = newBooleanPropertyDescriptor (WidgetPropertyCategory.BEHAVIOR, "preserve_ratio", Messages.WidgetProperties_PreserveRatio);
    /** Property */
    public static final WidgetPropertyDescriptor<String>                        propFallbackSymbol = newFilenamePropertyDescriptor (WidgetPropertyCategory.BEHAVIOR, "fallback_symbol", Messages.WidgetProperties_FallbackSymbol);
    public static final WidgetPropertyDescriptor<Boolean>                       propRunActionsOnMouseClick = newBooleanPropertyDescriptor (WidgetPropertyCategory.BEHAVIOR, "run_actions_on_mouse_click", Messages.WidgetProperties_RunActionsOnMouseClick);

    /** 'items' property: list of items (string properties) for combo box */
    public static final ArrayWidgetProperty.Descriptor<WidgetProperty<String> > propSymbols       = new ArrayWidgetProperty.Descriptor< >(
        WidgetPropertyCategory.WIDGET,
        "symbols",
        Messages.WidgetProperties_Symbols,
        (widget, index) -> {
            String symbol = DEFAULT_SYMBOL;
            try {
                if (index > 0)
                    symbol = ((SymbolWidget)widget).propSymbols().getElement(index - 1).getValue();
            } catch (IndexOutOfBoundsException e) {
                // It is expected when a widget with more than 2 symbols is parsed
                // and the property is being populated --> safe to ignore
            }
            return propSymbol(index).createProperty(widget, symbol);
        },
        0
    );

    private volatile WidgetProperty<Boolean>                     auto_size;
    private volatile WidgetProperty<Integer>                     array_index;
    private volatile WidgetProperty<WidgetColor>                 background;
    private volatile WidgetProperty<Boolean>                     enabled;
    private volatile WidgetProperty<Integer>                     initial_index;
    private volatile WidgetProperty<Boolean>                     preserve_ratio;
    private volatile WidgetProperty<Double>                      rotation;
    private volatile WidgetProperty<Boolean>                     show_index;
    private volatile ArrayWidgetProperty<WidgetProperty<String>> symbols;
    private volatile WidgetProperty<Boolean>                     transparent;
    private volatile String                                      importedFrom = null;
    private volatile WidgetProperty<String>                      fallbackSymbol;
    private volatile WidgetProperty<WidgetColor>                 disconnectOverlayColor;
    private volatile WidgetProperty<Boolean>                     run_actions_on_mouse_click;

    /** Returns 'symbol' property: element for list of 'symbols' property */
    private static WidgetPropertyDescriptor<String> propSymbol( int index ) {
        return newFilenamePropertyDescriptor(WidgetPropertyCategory.WIDGET, "symbol", Messages.WidgetProperties_Symbol + " " + index);
    }

    /** Constructor */
    public SymbolWidget ( ) {
        super(WIDGET_DESCRIPTOR.getType(), 100, 100);
    }

    /** @param index Symbol index
     *  @param fileName Symbol file
     */
    public void addOrReplaceSymbol( int index, String fileName ) {
        if ( index == symbols.size() ) {
            symbols.addElement(propSymbol(index).createProperty(this, fileName));
        } else if ( index < symbols.size() ) {
            symbols.getElement(index).setValue(fileName);
        } else {
            logger.warning("Out of bound index: " + index);
        }
    }

    /** Clear imported flag */
    public void clearImportedFrom ( ) {
        importedFrom = null;
    }

    @Override
    public WidgetConfigurator getConfigurator ( final Version persistedVersion ) throws Exception {
        return new SymbolConfigurator(persistedVersion);
    }

    /** @return imported info */
    public String getImportedFrom ( ) {
        return importedFrom;
    }

    /** @return property */
    public WidgetProperty<Integer> propArrayIndex ( ) {
        return array_index;
    }

    /** @return property */
    public WidgetProperty<Boolean> propAutoSize ( ) {
        return auto_size;
    }

    /** @return property */
    public WidgetProperty<WidgetColor> propBackgroundColor ( ) {
        return background;
    }

    /** @return property */
    public WidgetProperty<Boolean> propEnabled ( ) {
        return enabled;
    }

    /** @return property */
    public WidgetProperty<Integer> propInitialIndex ( ) {
        return initial_index;
    }

    /** @return property */
    public WidgetProperty<Boolean> propPreserveRatio ( ) {
        return preserve_ratio;
    }

    /** @return property */
    public WidgetProperty<Double> propRotation ( ) {
        return rotation;
    }

    /** @return property */
    public WidgetProperty<Boolean> propShowIndex ( ) {
        return show_index;
    }

    /** @return property */
    public ArrayWidgetProperty<WidgetProperty<String>> propSymbols ( ) {
        return symbols;
    }

    /** @return property */
    public WidgetProperty<Boolean> propTransparent ( ) {
        return transparent;
    }

    /** @return property */
    public WidgetProperty<String> propFallbackSymbol(){
        return fallbackSymbol;
    }

    /** @return property */
    public WidgetProperty<WidgetColor> propDiconnectOverlayColor(){
        return disconnectOverlayColor;
    }

    /** @return property */
    public WidgetProperty<Boolean> propRunActionsOnMouseClick() {
        return run_actions_on_mouse_click;
    }

    @Override
    protected void defineProperties ( final List<WidgetProperty<?>> properties ) {

        super.defineProperties(properties);

        properties.add(symbols        = propSymbols.createProperty(this, Collections.singletonList(propSymbol(0).createProperty(this, DEFAULT_SYMBOL))));

        properties.add(background     = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(initial_index  = propInitialIndex.createProperty(this, 0));
        properties.add(rotation       = propRotation.createProperty(this, 0.0));
        properties.add(show_index     = propShowIndex.createProperty(this, false));
        properties.add(transparent    = propTransparent.createProperty(this, true));

        properties.add(array_index    = propArrayIndex.createProperty(this, 0));
        properties.add(auto_size      = propAutoSize.createProperty(this, false));
        properties.add(enabled        = propEnabled.createProperty(this, true));
        properties.add(preserve_ratio = propPreserveRatio.createProperty(this, true));
        properties.add(fallbackSymbol = propFallbackSymbol.createProperty(this, DEFAULT_SYMBOL));
        WidgetColor alarmInvalidColor =
                WidgetColorService.getColor(NamedWidgetColors.ALARM_INVALID);
        WidgetColor defaultDisconnectedOverlayColor =
                new WidgetColor(alarmInvalidColor.getRed(), alarmInvalidColor.getGreen(), alarmInvalidColor.getBlue(), 128);
        properties.add(disconnectOverlayColor = propDisconnectOverlayColor.createProperty(this, defaultDisconnectedOverlayColor));
        {
            int indexOfPropActions = properties.indexOf(propActions());
            run_actions_on_mouse_click = propRunActionsOnMouseClick.createProperty(this, false);
            properties.add(indexOfPropActions + 1, run_actions_on_mouse_click);
        }

    }

    /**
     * Custom configurator to read legacy *.opi files.
     */
    protected static class SymbolConfigurator extends WidgetConfigurator {

        public SymbolConfigurator ( Version xmlVersion ) {
            super(xmlVersion);
        }

        @Override
        public boolean configureFromXML ( final ModelReader reader, final Widget widget, final Element xml ) throws Exception {

            if ( !super.configureFromXML(reader, widget, xml) ) {
                return false;
            }

            if ( xml_version.getMajor() < 2 ) {

                SymbolWidget symbol = (SymbolWidget) widget;
                List<String> fileNames = new ArrayList<>(2);

                symbol.importedFrom = xml.getAttribute("typeId");

                switch ( symbol.importedFrom ) {
                    case "org.csstudio.opibuilder.widgets.ImageBoolIndicator":
                        XMLUtil.getChildString(xml, "off_image").ifPresent(f -> fileNames.add(f));
                        XMLUtil.getChildString(xml, "on_image").ifPresent(f -> fileNames.add(f));
                        break;
                    case "org.csstudio.opibuilder.widgets.symbol.bool.BoolMonitorWidget":
                    case "org.csstudio.opibuilder.widgets.symbol.multistate.MultistateMonitorWidget":
                        XMLUtil.getChildString(xml, "image_file").ifPresent(f -> fileNames.add(f));
                        break;
                }

                for ( int i = 0; i < fileNames.size(); i++ ) {
                    symbol.addOrReplaceSymbol(i, fileNames.get(i));
                }

                XMLUtil.getChildBoolean(xml, "stretch_to_fit").ifPresent(stf -> symbol.propPreserveRatio().setValue(!stf));

            }

            return true;

        }

    }

    @Override
    protected String getInitialTooltip()
    {
        return "$(pv_name)\\n$(pv_value)\\n$(actions)";
    }
}
