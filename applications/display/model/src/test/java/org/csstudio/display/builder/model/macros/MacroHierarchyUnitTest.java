/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.macros;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Preferences;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.junit.Test;

/** JUnit test of macro hierarchy
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacroHierarchyUnitTest
{
    /** Test Macro Hierarchy
     *  @throws Exception on error
     */
    @Test
    public void testMacroHierarchy() throws Exception
    {
        // Macros start out empty
        MacroValueProvider macros = new Macros();
        System.out.println(macros);
        assertThat(macros.toString(), equalTo("[]"));

        // Preferences (at least in test setup where there is no preferences service)
        macros = Preferences.getMacros();
        System.out.println(macros);
        assertThat(macros.getValue("EXAMPLE_MACRO"), equalTo("Value from Preferences"));

        // Display model uses preferences
        final DisplayModel model = new DisplayModel();
        macros = model.getEffectiveMacros();
        System.out.println(macros);
        assertThat(macros.getValue("EXAMPLE_MACRO"), equalTo("Value from Preferences"));

        // .. but display can replace this value
        model.propMacros().getValue().add("EXAMPLE_MACRO", "Value from Display");
        macros = model.getEffectiveMacros();
        System.out.println(macros);
        assertThat(macros.getValue("EXAMPLE_MACRO"), equalTo("Value from Display"));

        // Similar, groups can replace macros
        final LabelWidget child = new LabelWidget();

        final GroupWidget group2 = new GroupWidget();
        group2.propMacros().getValue().add("EXAMPLE_MACRO", "In Group 2");
        group2.runtimeChildren().addChild(child);

        final GroupWidget group1 = new GroupWidget();
        group1.propMacros().getValue().add("EXAMPLE_MACRO", "In Group 1");
        group1.runtimeChildren().addChild(group2);

        model.runtimeChildren().addChild(group1);

        macros = group1.getEffectiveMacros();
        System.out.println(macros);
        assertThat(macros.getValue("EXAMPLE_MACRO"), equalTo("In Group 1"));

        macros = group2.getEffectiveMacros();
        System.out.println(macros);
        assertThat(macros.getValue("EXAMPLE_MACRO"), equalTo("In Group 2"));

        macros = child.getEffectiveMacros();
        System.out.println(macros);
        assertThat(macros.getValue("EXAMPLE_MACRO"), equalTo("In Group 2"));

        // Finally, the EmbeddedDisplayWidget can replace macros,
        // but testing that requires the runtime to load the embedded content
        // --> Leaving that to examples/macros
    }

    /** Test access to widget properties, Java properties and environment
     *  @throws Exception on error
     */
    @Test
    public void testPropertiesAndEnvironment() throws Exception
    {
        // Display model uses preferences
        final DisplayModel model = new DisplayModel();
        MacroValueProvider macros = model.getEffectiveMacros();
        assertThat(macros.getValue("EXAMPLE_MACRO"), equalTo("Value from Preferences"));

        // Can also fall back to widget properties
        assertThat(macros.getValue("type"), nullValue());
        macros = model.getMacrosOrProperties();
        String value = macros.getValue("type");
        assertThat(value, equalTo("display"));

        // Check fall back to Java properties
        value = macros.getValue("os.name");
        System.out.println("Java property os.name: " + value);
        assertThat(value, not(nullValue()));

        // Check fall back to environment variables
        value = macros.getValue("HOME");
        System.out.println("Environment variable $HOME: " + value);
        assertThat(value, not(nullValue()));
    }
}
