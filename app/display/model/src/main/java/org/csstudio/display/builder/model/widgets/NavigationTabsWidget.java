/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propDirection;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFile;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMacros;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propSelectedColor;
import static org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget.propGroupName;
import static org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget.runtimeModel;
import static org.csstudio.display.builder.model.widgets.TabsWidget.propActiveTab;
import static org.csstudio.display.builder.model.widgets.TabsWidget.propTabHeight;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.Direction;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.phoebus.framework.macros.Macros;

/** Widget with tabs to select amongst several embedded displays
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NavigationTabsWidget extends VisibleWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("navtabs", WidgetCategory.STRUCTURE,
            "Navigation Tabs",
            "/icons/navtabs.png",
            "Tabs to select amongst several embedded displays")
    {
        @Override
        public Widget createWidget()
        {
            return new NavigationTabsWidget();
        }
    };

    // 'state' structure that describes one state
    private static final StructuredWidgetProperty.Descriptor propTab =
        new StructuredWidgetProperty.Descriptor(WidgetPropertyCategory.BEHAVIOR, "tab", "Tab");

    /** Structure for one tab item and its embedded display */
    public static class TabProperty extends StructuredWidgetProperty
    {
        public TabProperty(final Widget widget, final int index)
        {
            super(propTab, widget,
                  Arrays.asList(propName.createProperty(widget, "Tab " + (index + 1)),
                                propFile.createProperty(widget, ""),
                                propMacros.createProperty(widget, new Macros()),
                                propGroupName.createProperty(widget, "")
                               ));
        }
        public WidgetProperty<String>       name()    { return getElement(0); }
        public WidgetProperty<String>       file()    { return getElement(1); }
        public WidgetProperty<Macros>       macros()  { return getElement(2); }
        public WidgetProperty<String>       group()   { return getElement(3); }
    }

    // 'tabs' array
    private static final ArrayWidgetProperty.Descriptor<TabProperty> propTabs =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.WIDGET, "tabs", "Tabs",
                (widget, index) -> new TabProperty(widget, index));

    private static final WidgetPropertyDescriptor<Integer> propTabWidth =
        CommonWidgetProperties.newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "tab_width", "Tab Width");

    private static final WidgetPropertyDescriptor<Integer> propTabSpacing =
        CommonWidgetProperties.newIntegerPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "tab_spacing", "Tab Spacing");

    private static final WidgetPropertyDescriptor<WidgetColor> propDeselectedColor =
            CommonWidgetProperties.newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "deselected_color", "Deselected Color");


    private volatile ArrayWidgetProperty<TabProperty> tabs;
    private volatile WidgetProperty<Direction> direction;
    private volatile WidgetProperty<Integer> tab_width;
    private volatile WidgetProperty<Integer> tab_height;
    private volatile WidgetProperty<Integer> tab_spacing;
    private volatile WidgetProperty<WidgetColor> selected_color;
    private volatile WidgetProperty<WidgetColor> deselected_color;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<Integer> active;
    private volatile WidgetProperty<DisplayModel> embedded_model;

    public NavigationTabsWidget()
    {
        // Default size similar to embedded display, plus space for tabs
        super(WIDGET_DESCRIPTOR.getType(),
              ActionButtonWidget.DEFAULT_WIDTH + EmbeddedDisplayWidget.DEFAULT_WIDTH,
              EmbeddedDisplayWidget.DEFAULT_HEIGHT);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(tabs = propTabs.createProperty(this, Arrays.asList(new TabProperty(this, 0))));
        properties.add(direction = propDirection.createProperty(this, Direction.VERTICAL));
        properties.add(tab_width = propTabWidth.createProperty(this, ActionButtonWidget.DEFAULT_WIDTH));
        properties.add(tab_height = propTabHeight.createProperty(this, ActionButtonWidget.DEFAULT_HEIGHT));
        properties.add(tab_spacing = propTabSpacing.createProperty(this, 2));
        properties.add(selected_color = propSelectedColor.createProperty(this, new WidgetColor(236, 236, 236)));
        properties.add(deselected_color = propDeselectedColor.createProperty(this, new WidgetColor(200, 200, 200)));
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(active = propActiveTab.createProperty(this, 0));
        properties.add(embedded_model = runtimeModel.createProperty(this, null));
    }

    /** @return 'tabs' property */
    public ArrayWidgetProperty<TabProperty> propTabs()
    {
        return tabs;
    }

    /** @return 'direction' property */
    public WidgetProperty<Direction> propDirection()
    {
        return direction;
    }

    /** @return 'tab_width' property */
    public WidgetProperty<Integer> propTabWidth()
    {
        return tab_width;
    }

    /** @return 'tab_height' property */
    public WidgetProperty<Integer> propTabHeight()
    {
        return tab_height;
    }

    /** @return 'tab_spacing' property */
    public WidgetProperty<Integer> propTabSpacing()
    {
        return tab_spacing;
    }

    /** @return 'selected_color' property */
    public WidgetProperty<WidgetColor> propSelectedColor()
    {
        return selected_color;
    }

    /** @return 'deselected_color' property */
    public WidgetProperty<WidgetColor> propDeselectedColor()
    {
        return deselected_color;
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

    /** @return 'embedded_model' property */
    public WidgetProperty<DisplayModel> runtimePropEmbeddedModel()
    {
        return embedded_model;
    }

    /** Current tab adds/replaces parent macros
     *  @return {@link Macros}
     */
    @Override
    public Macros getEffectiveMacros()
    {
        final Macros base = super.getEffectiveMacros();
        try
        {
            // Join macros of active tab
            int index = active.getValue();
            if (index >= 0  &&  index < tabs.size())
            {
                final TabProperty tab = tabs.getElement(index);
                return Macros.merge(base, tab.macros().getValue());
            }
        }
        catch (Throwable ex)
        {   // IndexOutOfBoundsException while tabs change size?
            logger.log(Level.WARNING, "Cannot access active tab macros", ex);
        }
        return base;
    }
}
