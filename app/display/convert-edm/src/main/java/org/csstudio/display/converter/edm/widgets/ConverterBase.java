/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.NamedWidgetColor;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmColor;
import org.csstudio.opibuilder.converter.model.EdmWidget;

/** Base for each converter
 *
 *  <p>Constructing a converter will convert an EDM
 *  widget into a corresponding Display Builder widget.
 *
 *  <p>Base class handles common properties like X, Y, Width, Height.
 *
 *  @author Kay Kasemir
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

        // TODO Correct offset of parent widget?
        widget.propX().setValue(t.getX());
        widget.propY().setValue(t.getY());
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
}
