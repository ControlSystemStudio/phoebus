/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propConfirmDialog;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propConfirmMessage;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontal;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propItemsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPassword;
import static org.csstudio.display.builder.model.widgets.ComboWidget.propItem;
import static org.csstudio.display.builder.model.widgets.ComboWidget.propItems;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;

/** Widget that writes to PV from selection of items
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class RadioWidget extends WritablePVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
            new WidgetDescriptor("radio", WidgetCategory.CONTROL,
                    "Radio Button",
                    "/icons/radiobutton.png",
                    "Selects one of multiple items using radio buttons",
                    Arrays.asList("org.csstudio.opibuilder.widgets.radioBox"))
    {
        @Override
        public Widget createWidget()
        {
            return new RadioWidget();
        }
    };

    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<List<WidgetProperty<String>>> items;
    private volatile WidgetProperty<Boolean> items_from_pv;
    private volatile WidgetProperty<Boolean> horizontal;
    private volatile WidgetProperty<Boolean> enabled;
    private volatile WidgetProperty<Boolean> confirm_dialog;
    private volatile WidgetProperty<String> confirm_message;
    private volatile WidgetProperty<String> password;

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
        properties.add(enabled = propEnabled.createProperty(this, true));
        properties.add(confirm_dialog = propConfirmDialog.createProperty(this, false));
        properties.add(confirm_message = propConfirmMessage.createProperty(this, "Are your sure you want to do this?"));
        properties.add(password = propPassword.createProperty(this, ""));
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

    /** @return 'enabled' property */
    public WidgetProperty<Boolean> propEnabled()
    {
        return enabled;
    }

    /** @return 'confirm_dialog' property */
    public WidgetProperty<Boolean> propConfirmDialog()
    {
        return confirm_dialog;
    }

    /** @return 'confirm_message' property */
    public WidgetProperty<String> propConfirmMessage()
    {
        return confirm_message;
    }

    /** @return 'password' property */
    public WidgetProperty<String> propPassword()
    {
        return password;
    }

    //  TODO: CR: Changing the name of a radio button item has no immediate effect,
    //            only after a resize the name is changed.
}
