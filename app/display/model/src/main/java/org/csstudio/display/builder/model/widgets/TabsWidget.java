/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propDirection;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMacros;
import static org.csstudio.display.builder.model.properties.InsetsWidgetProperty.runtimePropInsets;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
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
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.Direction;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** A Widget that arranges child widgets in 'tabs'.
 *
 *  <p>The widget has several tabs described by
 *  the {@link TabItemProperty}, each of which
 *  holds a list of child widgets.
 *
 *  <p>The 'parent' of those widgets is this Widget.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TabsWidget extends VisibleWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("tabs", WidgetCategory.STRUCTURE,
            Messages.TabsWidget_Name,
            "/icons/tabs.png",
            Messages.TabsWidget_Description,
            Arrays.asList("org.csstudio.opibuilder.widgets.tab"))
    {
        @Override
        public Widget createWidget()
        {
            return new TabsWidget();
        }
    };

    // Property that describes one tab item
    private final static StructuredWidgetProperty.Descriptor propTabItem =
        new StructuredWidgetProperty.Descriptor(WidgetPropertyCategory.DISPLAY, "tab", Messages.Tab_Item);

    /** Name, children of one tab */
    public static class TabItemProperty extends StructuredWidgetProperty
    {
        protected TabItemProperty(final Widget widget, final int index)
        {
            super(propTabItem, widget,
                  Arrays.asList(CommonWidgetProperties.propName.createProperty(widget, createTabText(index)),
                                new ChildrenProperty(widget)));
        }

        public WidgetProperty<String> name()
        {
            return getElement(0);
        }

        public ChildrenProperty children()
        {
            final WidgetProperty<List<Widget>> c = getElement(1);
            return (ChildrenProperty)c;
        }
    };

    private static final ArrayWidgetProperty.Descriptor<TabItemProperty> propTabs =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.WIDGET, "tabs", Messages.TabsWidget_Name,
                (widget, index) -> new TabItemProperty(widget, index));

    static final WidgetPropertyDescriptor<Integer> propTabHeight =
        CommonWidgetProperties.newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "tab_height", Messages.Tab_Height);

    static final WidgetPropertyDescriptor<Integer> propActiveTab =
        CommonWidgetProperties.newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "active_tab", Messages.ActiveTab,
                                                            0, Integer.MAX_VALUE);

    /** Custom WidgetConfigurator to load legacy file */
    private static class TabsWidgetConfigurator extends WidgetConfigurator
    {
        public TabsWidgetConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            final Optional<Integer> count_info = XMLUtil.getChildInteger(xml, "tab_count");
            if (xml_version.getMajor() < 2  &&  count_info.isPresent())
            {   // Legacy org.csstudio.opibuilder.widgets.tab used <tab_count>,
                // Create matching number of tabs
                final int count = count_info.get();
                final TabsWidget tabs_widget = (TabsWidget)widget;
                final ArrayWidgetProperty<TabItemProperty> tabs = tabs_widget.propTabs();
                while (count < tabs.size())
                    tabs.removeElement();
                while (count > tabs.size())
                    tabs.addElement();

                // Basics that apply to all tabs
                Optional<String> text = XMLUtil.getChildString(xml, "minimum_tab_height");
                if (text.isPresent())
                    tabs_widget.propTabHeight().setValue(Integer.parseInt(text.get()));

                text = XMLUtil.getChildString(xml, "horizontal_tabs");
                if (text.isPresent() && text.get().equals("false"))
                    tabs_widget.propDirection().setValue(Direction.VERTICAL);

                // Configure each tab from <tab_0_title>, <tab_1_title>, ...
                for (int i=0; i<count; ++i)
                {
                    text = XMLUtil.getChildString(xml, "tab_" + i + "_title");
                    if (text.isPresent())
                        tabs.getValue().get(i).name().setValue(text.get());
                }

                // Tab content was in sequence of
                // <widget typeId="org.csstudio.opibuilder.widgets.groupingContainer">
                // where detail was ignored except for the children of each group.
                int i = 0;
                for (Element content_xml : XMLUtil.getChildElements(xml, "widget"))
                {
                    if (! content_xml.getAttribute("typeId").contains("group"))
                    {
                        logger.log(Level.WARNING, "Legacy 'tab' widget misses content of tab " + i);
                        break;
                    }
                    model_reader.readWidgets(tabs.getValue().get(i).children(), content_xml);
                    ++i;
                }
            }
            return true;
        }
    }

    private volatile WidgetProperty<Macros> macros;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<Integer> active;
    private volatile ArrayWidgetProperty<TabItemProperty> tabs;
    private volatile WidgetProperty<Direction> direction;
    private volatile WidgetProperty<Integer> tab_height;
    private volatile WidgetProperty<int[]> insets;

    public TabsWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 400, 300);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(macros = propMacros.createProperty(this, new Macros()));
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(active = propActiveTab.createProperty(this, 0));
        properties.add(tabs = propTabs.createProperty(this, Arrays.asList(new TabItemProperty(this, 0),
                                                                             new TabItemProperty(this, 1))));
        properties.add(direction = propDirection.createProperty(this, Direction.HORIZONTAL));
        properties.add(tab_height = propTabHeight.createProperty(this, 30));
        properties.add(insets = runtimePropInsets.createProperty(this, new int[] { 0, 0 }));
    }

    private static String createTabText(final int index)
    {
        return MessageFormat.format(Messages.TabsWidget_TabNameFmt, index + 1);
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new TabsWidgetConfigurator(persisted_version);
    }

    /** @return Widget 'macros' */
    public WidgetProperty<Macros> widgetMacros()
    {
        return macros;
    }

    /** Group widget extends parent macros
     *  @return {@link Macros}
     */
    @Override
    public Macros getEffectiveMacros()
    {
        final Macros base = super.getEffectiveMacros();
        final Macros my_macros = widgetMacros().getValue();
        return Macros.merge(base, my_macros);
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'active_tab' property */
    public WidgetProperty<Integer> propActiveTab()
    {
        return active;
    }

    /** @return 'tabs' property */
    public ArrayWidgetProperty<TabItemProperty> propTabs()
    {
        return tabs;
    }

    /** @return 'direction' property */
    public WidgetProperty<Direction> propDirection()
    {
        return direction;
    }

    /** @return 'tab_height' property */
    public WidgetProperty<Integer> propTabHeight()
    {
        return tab_height;
    }

    /** @return 'insets' property */
    public WidgetProperty<int[]> runtimePropInsets()
    {
        return insets;
    }
}
