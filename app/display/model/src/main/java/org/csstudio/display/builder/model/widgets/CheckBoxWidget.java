/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBit;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propConfirmDialogOptions;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propConfirmMessage;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPassword;

import java.util.Arrays;
import java.util.List;

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
import org.csstudio.display.builder.model.properties.ConfirmDialog;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;

@SuppressWarnings("nls")
/** Widget that can read/write a bit in a PV
 *  @author Amanda Carpenter
 */
public class CheckBoxWidget extends WritablePVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("checkbox", WidgetCategory.CONTROL,
            "Check Box",
            "/icons/checkbox.png",
            "Read/write a bit in a PV",
            Arrays.asList("org.csstudio.opibuilder.widgets.checkbox") )
    {
        @Override
        public Widget createWidget()
        {
            return new CheckBoxWidget();
        }
    };

    /** 'label' property: Text for label */
    public static final WidgetPropertyDescriptor<String> propLabel =
        newStringPropertyDescriptor(WidgetPropertyCategory.WIDGET, "label", Messages.Checkbox_Label);

    /** 'auto_size' property: Automatically adjust size of widget */
    public static final WidgetPropertyDescriptor<Boolean> propAutoSize =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "auto_size", Messages.AutoSize);

    private volatile WidgetProperty<Boolean> enabled;
    private volatile WidgetProperty<Integer> bit;
    private volatile WidgetProperty<String> label;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<Boolean> auto_size;
    private volatile WidgetProperty<ConfirmDialog> confirm_dialog;
    private volatile WidgetProperty<String> confirm_message;
    private volatile WidgetProperty<String> password;

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(bit = propBit.createProperty(this, 0));
        properties.add(label = propLabel.createProperty(this, Messages.Checkbox_Label));
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(auto_size = propAutoSize.createProperty(this, false));
        properties.add(enabled = propEnabled.createProperty(this, true));
        properties.add(confirm_dialog = propConfirmDialogOptions.createProperty(this, ConfirmDialog.NONE));
        properties.add(confirm_message = propConfirmMessage.createProperty(this, "Are your sure you want to do this?"));
        properties.add(password = propPassword.createProperty(this, ""));
    }

    public CheckBoxWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    /** @return 'bit' property */
    public WidgetProperty<Integer> propBit()
    {
        return bit;
    }

    /** @return 'label' property */
    public WidgetProperty<String> propLabel()
    {
        return label;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'auto_size' property */
    public WidgetProperty<Boolean> propAutoSize()
    {
        return auto_size;
    }

    /** @return 'enabled' property */
    public WidgetProperty<Boolean> propEnabled()
    {
        return enabled;
    }

    /** @return 'confirm_dialog' property */
    public WidgetProperty<ConfirmDialog> propConfirmDialog()
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
