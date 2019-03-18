/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propType;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propX;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.csstudio.display.builder.model.widgets.TextUpdateWidget;
import org.csstudio.display.builder.model.widgets.VisibleWidget;
import org.junit.Test;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.Macros;

/** JUnit test of widget properties
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetPropertyUnitTest
{
    /** Check common widget properties */
    @Test
    public void testCommonWidgetProperty()
    {
        final Widget widget = new VisibleWidget("generic");
        System.out.println(widget);
        widget.setPropertyValue(propName, "test1");
        assertThat(widget.getName(), equalTo("test1"));
        assertThat(widget.getProperty(propName).getValue(), equalTo("test1"));

        assertThat(widget.getProperty("name").getValue(), instanceOf(String.class));
        assertThat(widget.getProperty("x").getValue(), instanceOf(Integer.class));
        assertThat(widget.getProperty("visible").getValue(), instanceOf(Boolean.class));
    }

    /** Check property write access */
    @Test
    public void testPropertyWrite()
    {
        final Widget widget = new Widget("generic");
        final WidgetProperty<Integer> property = widget.getProperty(propX);
        assertThat(property.getValue(), equalTo(0));
        assertThat(property.isDefaultValue(), equalTo(true));

        property.setValue(21);
        assertThat(property.getValue(), equalTo(21));
        assertThat(property.isDefaultValue(), equalTo(false));
    }

    class TestWidget extends Widget
    {
        TestWidget()
        {
            super("generic");
        }

        @Override
        protected void defineProperties(final List<WidgetProperty<?>> properties)
        {
            super.defineProperties(properties);
            properties.add(CustomWidget.propZeroTen.createProperty(this, 5));
            try
            {
                properties.add(CustomWidget.propZeroTen.createProperty(this, -5));
                throw new Error("Failed to detect default value -5 outside of range 0-10");
            }
            catch (final IllegalArgumentException ex)
            {
                // Expected...
                System.out.println("Detected: " + ex.getMessage());
            }
        }
    }

    /** Check property value range
     *  @throws Exception on error
     */
    @Test
    public void testPropertyValueRange() throws Exception
    {
        final Widget widget = new TestWidget();

        final WidgetProperty<Integer> property = widget.getProperty(CustomWidget.propZeroTen);
        assertThat(property.getValue(), equalTo(5));
        assertThat(property.getDefaultValue(), equalTo(5));

        property.setValue(7);
        assertThat(property.getValue(), equalTo(7));

        property.setValue(12);
        assertThat(property.getValue(), equalTo(10));

        property.setValue(-12);
        assertThat(property.getValue(), equalTo(0));
    }

    /** Check read-only access */
    @Test
    public void testReadonly()
    {
        final Widget widget = new Widget("generic");
        final WidgetProperty<String> type = widget.getProperty(propType);
        final WidgetProperty<String> name = widget.getProperty(propName);
        assertThat(type.isReadonly(), equalTo(true));
        assertThat(name.isReadonly(), equalTo(false));

        assertThat(type.getValue(), equalTo("generic"));
        type.setValue("other type");
        assertThat(type.getValue(), equalTo("generic"));
    }

    /** Example enum with end-user labels */
    enum Align
    {
        LEFT("Left"), CENTER("Center"), RIGHT("Right");

        private final String label;

        private Align(final String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    };

    WidgetPropertyDescriptor<Align> alignHoriz =
            new WidgetPropertyDescriptor<>(WidgetPropertyCategory.DISPLAY, "horiz_align", "Horizontal alignment")
    {
        @Override
        public WidgetProperty<Align> createProperty(final Widget widget, final Align default_value)
        {
            return new EnumWidgetProperty<>(this, widget, default_value);
        }
    };

    /* Test enumerated property read/write API */
    @Test
    public void testEnum() throws Exception
    {
        final DisplayModel widget = new DisplayModel();
        final Macros macros = new Macros();
        macros.add("ALIGN", "1");
        widget.propMacros().setValue(macros);

        final EnumWidgetProperty<Align> prop = new EnumWidgetProperty<>(alignHoriz, widget, Align.LEFT);
        System.out.println(prop);
        assertThat(prop.getValue(), equalTo(Align.LEFT));

        System.out.println(Arrays.toString(prop.getLabels()));
        assertThat(prop.getLabels(), equalTo(new String[] { "Left", "Center", "Right" } ));

        // Set value as enum
        prop.setValue(Align.RIGHT);
        System.out.println(prop);
        assertThat(prop.getValue(), equalTo(Align.RIGHT));

        // Set value as object, using the enum
        prop.setValueFromObject(Align.LEFT);
        System.out.println(prop);
        assertThat(prop.getValue(), equalTo(Align.LEFT));

        // Set value from ordinal
        prop.setValueFromObject(2);
        System.out.println(prop);
        assertThat(prop.getValue(), equalTo(Align.RIGHT));

        // Set value from string with ordinal
        prop.setValueFromObject("1");
        System.out.println(prop);
        assertThat(prop.getValue(), equalTo(Align.CENTER));
        assertThat(prop.getSpecification(), equalTo("1"));

        // Capture invalid ordinal
        try
        {
            prop.setValueFromObject(20);
            fail("Allowed invalid ordinal");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage().toLowerCase(), containsString("invalid ordinal"));
        }

        // Capture use of label or name instead of ordinal
        try
        {
            prop.setValueFromObject("CENTER");
            fail("Allowed name instead of ordinal");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage().toLowerCase(), containsString("expects ordinal"));
        }

        // Check handling of specification and macros
        prop.setSpecification("0");
        System.out.println(prop);
        assertThat(prop.getValue(), equalTo(Align.LEFT));

        prop.setSpecification("$(ALIGN)");
        System.out.println(prop);
        assertThat(prop.getValue(), equalTo(Align.CENTER));
        System.out.println(prop);
        assertThat(prop.getSpecification(), equalTo("$(ALIGN)"));
    }

    @Test
    public void testMacroDefault()
    {
        // Widget with X position set to $(X), where that macro has the value 0
        final DisplayModel display = new DisplayModel();
        display.propMacros().getValue().add("X", "0");

        final LabelWidget widget = new LabelWidget();
        display.runtimeChildren().addChild(widget);

        ((MacroizedWidgetProperty<Integer>)widget.propX()).setSpecification("$(X)");

        // The X position value matches the default
        assertThat(widget.propX().getValue(), equalTo(0));

        // .. but the property doesn't have the default value,
        // i.e. it must be saved by the editor, since $(X) could
        // in other invocation evaluate to anything but 0.
        assertThat(widget.propX().isDefaultValue(), equalTo(false));
    }

    @Test
    public void testMacrosAndProperties() throws Exception
    {
        final DisplayModel display = new DisplayModel();

        final TextUpdateWidget widget = new TextUpdateWidget();
        display.runtimeChildren().addChild(widget);

        // "name" property of widget can be accessed as macro
        widget.propName().setValue("fred");
        assertThat(MacroHandler.replace(widget.getMacrosOrProperties(), "$(name)"), equalTo("fred"));

        // If there is actually a "name" macro, that takes precedence
        display.propMacros().getValue().add("name", "The Name");
        assertThat(MacroHandler.replace(widget.getMacrosOrProperties(), "$(name)"), equalTo("The Name"));

        // Bad recursion: Property was set to macro which in turn requests that same property
        ((MacroizedWidgetProperty<String>)widget.propPVName()).setSpecification("$(pv_name)");
        MacroHandler.replace(widget.getMacrosOrProperties(), "$(pv_name)");
        // Macro remains unresolved (with many warnings on console)
        assertThat(widget.propPVName().getValue().toLowerCase(), containsString("recursive"));
    }
}
