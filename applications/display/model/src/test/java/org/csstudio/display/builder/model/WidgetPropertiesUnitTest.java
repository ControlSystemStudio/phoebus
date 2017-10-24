/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.widgets.MultiStateLEDWidget;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;
import org.junit.Test;

/** JUnit test of widget properties, their order, categories
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetPropertiesUnitTest
{
    /** Check common widget properties */
    @Test
    public void testListingWidgetProperties()
    {
        final Widget widget = new CustomWidget();

        for (final WidgetProperty<?> property : widget.getProperties())
            System.out.println(property.getCategory().name() + " - " + property.getName());

        // Get list of property names
        final List<String> prop_names =
            widget.getProperties().stream().map(WidgetProperty::getName).collect(Collectors.toList());
        System.out.println(prop_names);

        // "quirk" was added last, but should appear before "x"
        // because it's in the WIDGET category, while "x" is a POSITION
        assertThat(widget.getProperty("quirk").getCategory(), equalTo(WidgetPropertyCategory.WIDGET));
        assertThat(widget.getProperty("x").getCategory(), equalTo(WidgetPropertyCategory.POSITION));
        assertTrue(WidgetPropertyCategory.WIDGET.ordinal() < WidgetPropertyCategory.POSITION.ordinal());
        final int x_idx = prop_names.indexOf("x");
        final int quirk_idx = prop_names.indexOf("quirk");
        assertTrue(x_idx >= 0);
        assertTrue(quirk_idx >= 0);
        assertTrue(quirk_idx < x_idx);
    }

    @Test
    public void testPropertyPath()
    {
        final Widget led = new MultiStateLEDWidget();

        final WidgetProperty<?> color1 = led.getProperty("states[1].color");
        System.out.println(color1.getValue());
        assertThat(color1.getValue().toString(), equalTo("On"));

        try
        {
            led.getProperty("states[1].bogus");
            fail("Accessed bogus element");
        }
        catch (IllegalArgumentException ex)
        {
            System.out.println("Properly detected " + ex.getMessage());
            assertThat(ex.getMessage(), containsString("bogus"));
        }
    }

    @Test
    public void testLegacyProperties()
    {
        final Widget plot = new XYPlotWidget();

        // Old and current name should lead to the same property
        WidgetProperty<?> legacy = plot.getProperty("axis_0_axis_title");
        WidgetProperty<?> current = plot.getProperty("x_axis.title");
        assertThat(legacy, sameInstance(current));

        legacy = plot.getProperty("axis_1_minimum");
        current = plot.getProperty("y_axes[0].minimum");
        assertThat(legacy, sameInstance(current));

        legacy = plot.getProperty("axis_1_auto_scale");
        current = plot.getProperty("y_axes[0].autoscale");
        assertThat(legacy, sameInstance(current));

        // getProperty() throws an exception for unknown names
        try
        {
            plot.getProperty("x_axis.not_the_title");
            fail("Didn't catch property name typo");
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            assertThat(ex.getMessage(), containsString("not_the_title"));
        }

        // checkProperty does _not_ resolve paths nor legacy names
        assertThat(plot.checkProperty("x_axis").isPresent(), equalTo(true));
        assertThat(plot.checkProperty("x_axis.title").isPresent(), equalTo(false));
        assertThat(plot.checkProperty("axis_0_axis_title").isPresent(), equalTo(false));
    }

    @Test
    public void testPropertyListing()
    {
        final Widget led = new MultiStateLEDWidget();
        final List<String> names = new ArrayList<>();
        for (WidgetProperty<?> property : led.getProperties())
            names.addAll(Widget.expandPropertyNames(property));

        System.out.println("Properties of " + led.getType());
        for (String name : names)
            System.out.println(name);
        assertThat(names, hasItem("name"));
        assertThat(names, hasItem("states[1].color"));

        // Check that all listed properties are actually found
        for (String name : names)
        {
            WidgetProperty<?> property = led.getProperty(name);
            assertThat(name, property, not(nullValue()));
        }
    }
}
