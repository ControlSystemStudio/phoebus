/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMacros;
import static org.csstudio.display.builder.model.properties.InsetsWidgetProperty.runtimePropInsets;

import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.framework.macros.Macros;

/**
 * An Array Widget contains copies of a child widget. Each copy is assigned the
 * value of one element of a PV.
 *
 * @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ArrayWidget extends PVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("array", WidgetCategory.STRUCTURE,
            Messages.ArrayWidget_Name,
                    "/icons/array.png",
            Messages.ArrayWidget_Description,
            Arrays.asList("org.csstudio.opibuilder.widgets.array"))
    {
        @Override
        public Widget createWidget()
        {
            return new ArrayWidget();
        }
    };

    /** {@link ChildrenProperty} wrapper that adjusts writing to XML*/
    public static class ArrayWidgetChildrenProperty extends ChildrenProperty
    {
        public ArrayWidgetChildrenProperty(Widget widget)
        {
            super(widget);
        }

        @Override
        public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
        {
            if (!getValue().isEmpty())
                model_writer.writeWidgets(getValue().subList(0, 1));
        }
    }

    private volatile WidgetProperty<Macros> macros;
    private volatile ChildrenProperty children;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<int[]> insets;

    public ArrayWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 100, 300);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(macros = propMacros.createProperty(this, new Macros()));
        properties.add(children = new ArrayWidgetChildrenProperty(this));
        properties.add(foreground = propForegroundColor.createProperty(this,
                WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this,
                WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(insets = runtimePropInsets.createProperty(this, new int[] { 0, 0 }));
    }

    /**
     * Array widget extends parent macros
     *
     * @return {@link Macros}
     */
    @Override
    public Macros getEffectiveMacros()
    {
        final Macros base = super.getEffectiveMacros();
        final Macros my_macros = propMacros().getValue();
        return Macros.merge(base, my_macros);
    }

    /** @return 'macros' property */
    public WidgetProperty<Macros> propMacros()
    {
        return macros;
    }

    /** @return Runtime 'children' property*/
    public ChildrenProperty runtimeChildren()
    {
        return children;
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> displayBackgroundColor()
    {
        return background;
    }

    /** @return Runtime 'insets' property */
    public WidgetProperty<int[]> runtimePropInsets()
    {
        return insets;
    }
}
