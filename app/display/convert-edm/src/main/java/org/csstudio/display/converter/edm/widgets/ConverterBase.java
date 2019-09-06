/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.NamedWidgetColor;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.properties.WidgetFontStyle;
import org.csstudio.display.converter.edm.ConverterPreferences;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.StringSplitter;
import org.csstudio.opibuilder.converter.model.EdmColor;
import org.csstudio.opibuilder.converter.model.EdmFont;
import org.csstudio.opibuilder.converter.model.EdmWidget;

/** Base for each converter
 *
 *  <p>Constructing a converter will convert an EDM
 *  widget into a corresponding Display Builder widget.
 *
 *  <p>Base class handles common properties like X, Y, Width, Height.
 *
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 *
 *  @param <W> Display Manager {@link Widget} type
 */
@SuppressWarnings("nls")
public abstract class ConverterBase<W extends Widget>
{
    protected final W widget;

    public ConverterBase(final EdmConverter converter, final Widget parent, final EdmWidget t)
    {
        widget = createWidget();
        widget.propName().setValue(t.getType());

        // Correct offset of parent widget
        widget.propX().setValue(t.getX() - converter.getOffsetX());
        widget.propY().setValue(t.getY() - converter.getOffsetY());
        widget.propWidth().setValue(t.getW());
        widget.propHeight().setValue(t.getH());

        // TODO See OpiWidget for visPv


        final ChildrenProperty parent_children = ChildrenProperty.getChildren(parent);
        if (parent_children == null)
            throw new IllegalStateException("Cannot add as child to " + parent);
        parent_children.addChild(widget);
    }

    protected abstract W createWidget();

    /** @param edm EDM Color
     *  @param prop Display builder color property to set from EDM color
     */
    public static void convertColor(final EdmColor edm,
                                    final WidgetProperty<WidgetColor> prop)
    {
        // TODO See OpiColor
        if (edm.isDynamic() || edm.isBlinking())
            throw new IllegalStateException("Can only handle static colors");

        // EDM uses 16 bit color values
        final int red   = edm.getRed()   >> 8,
                  green = edm.getGreen() >> 8,
                  blue  = edm.getBlue()  >> 8;
        final String name = edm.getName();
        if (name != null  &&  !name.isBlank())
            prop.setValue(new NamedWidgetColor(name, red, green, blue));
        else
            prop.setValue(new WidgetColor(red, green, blue));
    }

    /** @param edm EDM font
     *  @param prop Display builder font property to set from EDM font
     */
    public static void convertFont(final EdmFont edm,
                                   final WidgetProperty<WidgetFont> prop)
    {
        final String family = ConverterPreferences.mapFont(edm.getName());

        final WidgetFontStyle style;
        if (edm.isBold() && edm.isItalic())
            style = WidgetFontStyle.BOLD_ITALIC;
        else if (edm.isBold())
            style = WidgetFontStyle.BOLD;
        else if (edm.isItalic())
            style = WidgetFontStyle.ITALIC;
        else
            style = WidgetFontStyle.REGULAR;

        final double size = edm.getSize();
        prop.setValue(new WidgetFont(family, style, size));
    }

    /**
     * If pvName is a LOC or CALC EDM PV, attempt to convert it to a syntax
     * understood by CSS.
     *
     * If conversion fails or it is a regular PV, return the unchanged PV name.
     * @param pvName PV name to convert
     * @return converted PV name
     */
    public static String convertPVName(String pvName)
    {
        if (pvName.startsWith("LOC"))
            pvName = parseLocPV(pvName);
        else if (pvName.startsWith("CALC"))
            logger.log(Level.WARNING, "Not converting " + pvName);
        return pvName;
    }

    /**
     * Convert an EDM local PV into a CSS local PV.
     * @param pvName local EDM PV name
     * @return local CSS PV name
     */
    private static String parseLocPV(String pvName)
    {
        if (pvName.startsWith("LOC\\"))
        {
            try
            {
                String newName = pvName.replace("$(!W)", "$(DID)");
                newName = newName.replaceAll("\\x24\\x28\\x21[A-Z]{1}\\x29", "\\$(DID)");
                String[] parts = StringSplitter.splitIgnoreInQuotes(newName, '=', true);
                StringBuilder sb = new StringBuilder("loc://");
                sb.append(parts[0].substring(5));
                if (parts.length > 1)
                {
                    String type = "";
                    String initValue = parts[1];
                    if (parts[1].startsWith("d:"))
                    {
                        type = "<VDouble>";
                        initValue = parts[1].substring(2);
                    }
                    else if (parts[1].startsWith("i:"))
                    {
//                        type = "<VDouble>";
                        initValue = parts[1].substring(2);
                    }
                    else if (parts[1].startsWith("s:"))
                    {
//                          type = "<VString>";
                        initValue = "\""+parts[1].substring(2)+"\"";
                    }
                    else if (parts[1].startsWith("e:"))
                    {   // Enumerated pv
                        // cannot be
                        // converted.
                        // TODO loc://xxx<VEnum> is now supported
                        return pvName;
                    }
                    //doesn't append type yet to support utility pv.
                    sb.append("(").append(initValue).append(")");
                }
                return sb.toString();
            }
            catch (Exception e)
            {
                // Ignore
            }
        }
        return pvName;
    }
}
