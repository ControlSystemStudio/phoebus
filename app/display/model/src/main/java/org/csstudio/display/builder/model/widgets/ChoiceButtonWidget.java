/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propConfirmDialog;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propConfirmMessage;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontal;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontalAlignment;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propVerticalAlignment;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propItemsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPassword;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propSelectedColor;
import static org.csstudio.display.builder.model.widgets.ComboWidget.propItem;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;

/** Widget for PV with choices (enum)
 *
 *  <p>Creates one button per choice
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ChoiceButtonWidget extends WritablePVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
            new WidgetDescriptor("choice", WidgetCategory.CONTROL,
                    "Choice Button",
                    "/icons/choice_button.png",
                    "Selects one of multiple items using buttons",
                    Arrays.asList("org.csstudio.opibuilder.widgets.choiceButton"))
    {
        @Override
        public Widget createWidget()
        {
            return new ChoiceButtonWidget();
        }
    };

    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetColor> selected;
    private volatile ArrayWidgetProperty<WidgetProperty<String>> items;
    private volatile WidgetProperty<Boolean> items_from_pv;
    private volatile WidgetProperty<HorizontalAlignment> horizontal_alignment;
    private volatile WidgetProperty<VerticalAlignment> vertical_alignment;
    private volatile WidgetProperty<Boolean> horizontal;
    private volatile WidgetProperty<Boolean> enabled;
    private volatile WidgetProperty<Boolean> confirm_dialog;
    private volatile WidgetProperty<String> confirm_message;
    private volatile WidgetProperty<String> password;

    /** Constructor */
    public ChoiceButtonWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 100, 43);
    }

    private static ArrayWidgetProperty.Descriptor<WidgetProperty<String>> choiceItemDescriptor =
            new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.BEHAVIOR, "items", "Items",
                    (widget, index) -> CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR,
                                "item",
                                Messages.ComboWidget_Item).createProperty(widget, Messages.ComboWidget_Item + " " + (index + 1)));

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BUTTON_BACKGROUND)));
        properties.add(selected = propSelectedColor.createProperty(this, new WidgetColor(200, 200, 200)));
        items = choiceItemDescriptor.createProperty(this,
                Arrays.asList(propItem.createProperty(this, Messages.ComboWidget_Item + " 1"),
                        propItem.createProperty(this, Messages.ComboWidget_Item  + " 2")));
        properties.add(items);
        properties.add(items_from_pv = propItemsFromPV.createProperty(this, true));
        properties.add(horizontal_alignment = propHorizontalAlignment.createProperty(this, HorizontalAlignment.CENTER));
        properties.add(vertical_alignment = propVerticalAlignment.createProperty(this, VerticalAlignment.MIDDLE));
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

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'selected_color' property */
    public WidgetProperty<WidgetColor> propSelectedColor()
    {
        return selected;
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
    
    /** @return 'horizontal_alignment' property */
    public WidgetProperty<HorizontalAlignment> propHorizontalAlignment()
    {
        return horizontal_alignment;
    }
    
    /** @return 'vertical_alignment' property */
    public WidgetProperty<VerticalAlignment> propVerticalAlignment()
    {
        return vertical_alignment;
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
}
