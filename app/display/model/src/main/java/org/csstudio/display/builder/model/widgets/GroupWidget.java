/*******************************************************************************
 * Copyright (c) 2015-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;
import static org.csstudio.display.builder.model.properties.InsetsWidgetProperty.runtimePropExtendedInsets;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.ChildrenProperty;
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
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** A Group Widget contains child widgets.
 *
 *  <p>In the editor, moving the group will move all the widgets inside the group.
 *  Groups are also a convenient way to copy and paste a collection of widgets.
 *
 *  <p>Model Widgets within the group use coordinates relative to the group,
 *  i.e. a child at (x, y) = (0, 0) would be in the left upper corner of the group
 *  and <em>not</em> in the left upper corner of the display.
 *
 *  <p>At runtime, the group may add a labeled border to visually frame
 *  its child widgets, which further offsets the child widgets by the width of
 *  the border.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class GroupWidget extends MacroWidget
{
    /** Group Widget version */
    public static final Version GROUP_WIDGET_VERSION = new Version(3, 0, 0);

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
            new WidgetDescriptor("group", WidgetCategory.STRUCTURE,
                    Messages.GroupWidget_Name,
                    "/icons/group.png",
                    Messages.GroupWidget_Description,
                    Arrays.asList("org.csstudio.opibuilder.widgets.groupingContainer"))
            {
                @Override
                public Widget createWidget()
                {
                    return new GroupWidget();
                }
            };

    /** Group widget style */
    public enum Style
    {
        /** Fill group border */
        GROUP(Messages.Style_Group),
        /** Title bar */
        TITLE(Messages.Style_Title),
        /** Single-line border */
        LINE(Messages.Style_Line),
        /** Nothing */
        NONE(Messages.Style_None);

        private final String name;

        private Style(final String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    /** 'style' property */
    static final WidgetPropertyDescriptor<Style> propStyle =
            new WidgetPropertyDescriptor<>(
                    WidgetPropertyCategory.DISPLAY, "style", Messages.Style)
            {
                @Override
                public EnumWidgetProperty<Style> createProperty(final Widget widget,
                                                                final Style default_value)
                {
                    return new EnumWidgetProperty<>(this, widget, default_value);
                }
            };

    /** Convert legacy "border_style"
     *
     *  @param border_style Legacy &lt;border_style> value
     *  @return {@link Style}
     */
    static Style convertLegacyStyle(final int border_style)
    {
        switch (border_style)
        {
            case  0: // NONE
            case 15: // EMPTY
                return Style.NONE;

            case  1: // LINE
            case  2: // RAISED
            case  3: // LOWERED
            case  4: // ETCHED
            case  5: // RIDGED
            case  6: // BUTTON_RAISED
            case  7: // BUTTON_PRESSED
            case  8: // DOTTED
            case  9: // DASHED
            case 10: // DASH_DOT
            case 11: // DASH_DOT_DOT
            case 14: // ROUND_RECTANGLE_BACKGROUND
                return Style.LINE;

            case 12: // TITLE_BAR
                return Style.TITLE;

            case 13: // GROUP_BOX
            default:
                return Style.GROUP;
        }
    }

    /** Custom WidgetConfigurator to load legacy file */
    private static class GroupWidgetConfigurator extends WidgetConfigurator
    {
        public GroupWidgetConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            final GroupWidget group_widget = (GroupWidget) widget;
            if (xml_version.getMajor() < 2)
            {

                // Translate border styles
                XMLUtil.getChildInteger(xml, "border_style")
                        .ifPresent(old -> group_widget.style.setValue(convertLegacyStyle(old)));

                // Legacy had 'border_color'.
                // It wasn't used by Group Box style, which had built-in gray,
                // but was used by the label and other lines
                // -> Use as 'foreground_color'
                final Element text = XMLUtil.getChildElement(xml, "border_color");
                if (text != null)
                    group_widget.line.readFromXML(model_reader, text);
                if (group_widget.style.getValue() != Style.TITLE)
                    group_widget.foreground.readFromXML(model_reader, text);
            }

            if (xml_version.getMajor() < 3) {
                if(xml_version.getMajor() > 1){
                    final Element text_foreground = XMLUtil.getChildElement(xml, "foreground_color");
                    if (text_foreground != null)
                        group_widget.line.readFromXML(model_reader, text_foreground);
                }
                final Element text_background = XMLUtil.getChildElement(xml, "background_color");
                if (text_background != null && group_widget.style.getValue() == Style.TITLE)
                    group_widget.foreground.readFromXML(model_reader, text_background);
            }

            return true;
        }
    }

    private volatile ChildrenProperty children;
    private volatile WidgetProperty<Style> style;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetColor> line;
    private volatile WidgetProperty<Boolean> transparent;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<int[]> insets;

    /** Constructor */
    public GroupWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 300, 200);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(children = new ChildrenProperty(this));
        properties.add(style = propStyle.createProperty(this, Style.GROUP));
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(line = propLineColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(transparent = propTransparent.createProperty(this, false));
        properties.add(insets = runtimePropExtendedInsets.createProperty(this, new int[] { 0, 0, 0, 0 }));
    }

    @Override
    public WidgetProperty<?> getProperty(String name) throws IllegalArgumentException, IndexOutOfBoundsException
    {
        // Support legacy scripts that access border color
        if (name.equals("border_color"))
            return foreground;
        return super.getProperty(name);
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new GroupWidgetConfigurator(persisted_version);
    }

    /** {@inheritDoc} */
    @Override
    public void expandMacros(final Macros input)
    {
        // Expand the group's macros
        super.expandMacros(input);

        // Expand child widget macros using the group macros as input
        for (Widget child : runtimeChildren().getValue())
            child.expandMacros(propMacros().getValue());
    }

    @Override
    public Version getVersion()
    {
        return GROUP_WIDGET_VERSION;
    }

    /** @return Runtime 'children' property */
    public ChildrenProperty runtimeChildren()
    {
        return children;
    }

    /** @return 'style' property */
    public WidgetProperty<Style> propStyle()
    {
        return style;
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

    public WidgetProperty<WidgetColor> propLineColor()
    {
        return line;
    }

    /** @return 'transparent' property */
    public WidgetProperty<Boolean> propTransparent()
    {
        return transparent;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'insets' property */
    public WidgetProperty<int[]> runtimePropInsets()
    {
        return insets;
    }
}
