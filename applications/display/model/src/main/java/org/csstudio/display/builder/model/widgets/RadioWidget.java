/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontal;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propItemsFromPV;

import java.util.Arrays;
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
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;

/** Widget that writes to PV from selection of items
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class RadioWidget extends PVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
            new WidgetDescriptor("radio", WidgetCategory.CONTROL,
                    "Radio Button",
                    "/icons/radiobutton.png",
                    "Selects one of multiple items using radio buttons",
                    Arrays.asList("org.csstudio.opibuilder.widgets.radioBox",
                                  "org.csstudio.opibuilder.widgets.choiceButton"))
    {
        @Override
        public Widget createWidget()
        {
            return new RadioWidget();
        }
    };

    /** 'item' property: element for list of 'items' property */
    private static final WidgetPropertyDescriptor<String> propItem =
            CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "item", Messages.ComboWidget_Item);

    /** 'items' property: list of items (string properties) for combo box */
    public static final WidgetPropertyDescriptor< List<WidgetProperty<String>> > propItems =
            new ArrayWidgetProperty.Descriptor< WidgetProperty<String> >(WidgetPropertyCategory.BEHAVIOR, "items", Messages.ComboWidget_Items,
                                                                         (widget, index) -> propItem.createProperty(widget, "Item " + index));

    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<List<WidgetProperty<String>>> items;
    private volatile WidgetProperty<Boolean> items_from_pv;
    private volatile WidgetProperty<Boolean> horizontal;

    public RadioWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 100, 43);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(items = propItems.createProperty(this, Arrays.asList(propItem.createProperty(this, "Item 1"), propItem.createProperty(this, "Item 2"))));
        properties.add(items_from_pv = propItemsFromPV.createProperty(this, true));
        properties.add(horizontal = propHorizontal.createProperty(this, true));
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'items_from_PV' property */
    public WidgetProperty<Boolean> propItemsFromPV()
    {
        return items_from_pv;
    }

    /** @return 'items' property */
    public WidgetProperty< List<WidgetProperty<String>> > propItems()
    {
        return items;
    }

    /** @return 'horizontal' property */
    public WidgetProperty<Boolean> propHorizontal()
    {
        return horizontal;
    }

    //  TODO: CR: Changing the name of a radio button item has no immediate effect,
    //            only after a resize the name is changed.

}
