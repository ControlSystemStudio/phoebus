/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBorderColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBorderWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;

import java.util.List;
import java.util.Optional;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Helper for adding custom border to widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BorderSupport
{
    public static void addBorderProperties(final Widget widget, final List<WidgetProperty<?>> properties)
    {
        properties.add(propBorderWidth.createProperty(widget, 0));
        properties.add(propBorderColor.createProperty(widget, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
    }

    /** Helper for custom {@link WidgetConfigurator} to adjust legacy border
     *  @param widget
     *  @param xml
     *  @throws Exception
     */
    public static void handleLegacyBorder(final Widget widget, final Element xml) throws Exception
    {
        // Style tends to be a number, but could also be "None".
        final Optional<String> style_text = XMLUtil.getChildString(xml, "border_style");
        if (! style_text.isPresent())
            return;

        if ("none".equalsIgnoreCase(style_text.get()))
            return;

        final int style;
        try
        {
            style = Integer.parseInt(style_text.get());
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Invalid border_style '" + style_text.get() + "'");
        }

        // BOY supported 16 different border styles,
        // org.csstudio.opibuilder.visualparts.BorderStyle.
        // Now only supporting LINE.
        // Might in the future implement a few of the legacy styles,
        // but trying to avoid borders that mimic buttons or alarm colors.
        switch (style)
        {
        case  0: // NONE
        case 15: // EMPTY
        case  3: // LOWERED
            widget.getProperty(propBorderWidth).setValue(0);
            return;
        case  2: // RAISED
        case  4: // ETCHED
        case  5: // RIDGED
        case  6: // BUTTON_RAISED
        case  7: // BUTTON_PRESSED
            widget.getProperty(propBorderColor).setValue(WidgetColorService.getColor(NamedWidgetColors.TEXT));
            break;
        case 14: // ROUND_RECTANGLE_BACKGROUND
            // Forced the background color to be used even if 'transparent'
            widget.checkProperty(propTransparent.getName())
                  .ifPresent(trans -> trans.setValue(false));
            break;
        }

        // Border used to be inside the bounds,
        // shrinking the actual widget
        final int width = widget.getProperty(propBorderWidth).getValue();
        if (width > 0)
        {
            widget.propX().setValue(widget.propX().getValue() + width);
            widget.propY().setValue(widget.propY().getValue() + width);
            widget.propWidth().setValue(widget.propWidth().getValue() - 2*width);
            widget.propHeight().setValue(widget.propHeight().getValue() - 2*width);
        }
    }
}
