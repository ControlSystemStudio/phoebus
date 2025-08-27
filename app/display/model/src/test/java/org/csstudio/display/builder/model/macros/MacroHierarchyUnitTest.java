/*******************************************************************************
 * Copyright (c) 2015-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.macros;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Preferences;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.junit.jupiter.api.Test;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.framework.macros.Macros;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/** JUnit test of macro hierarchy
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacroHierarchyUnitTest
{
    /** Test Macro Hierarchy */
    @Test
    public void testMacroHierarchy()
    {
        // prefs -> model -> group1 -> group2 -> child

        final LabelWidget child = new LabelWidget();

        final GroupWidget group2 = new GroupWidget();
        group2.propMacros().getValue().add("EXAMPLE_MACRO", "In Group 2");
        group2.runtimeChildren().addChild(child);

        final GroupWidget group1 = new GroupWidget();
        group1.propMacros().getValue().add("EXAMPLE_MACRO", "In Group 1");
        group1.runtimeChildren().addChild(group2);

        final DisplayModel model = new DisplayModel();
        model.propMacros().getValue().add("TITLE", "Display Title");
        model.runtimeChildren().addChild(group1);

        Macros prefs = new Macros();
        prefs.add("EXAMPLE_MACRO", "Value from Preferences");

        // Expand macros of model, recursively, with prefs as input
        model.expandMacros(prefs);

        // Model inherits from prefs
        Macros macros = model.getEffectiveMacros();
        System.out.println(macros);
        assertThat(macros.getValue("EXAMPLE_MACRO"), equalTo("Value from Preferences"));

        // Groups replace...
        macros = group1.getEffectiveMacros();
        System.out.println(macros);
        assertThat(macros.getValue("EXAMPLE_MACRO"), equalTo("In Group 1"));

        macros = group2.getEffectiveMacros();
        System.out.println(macros);
        assertThat(macros.getValue("EXAMPLE_MACRO"), equalTo("In Group 2"));

        // child has replacement from group2 but also inherited macros
        macros = child.getEffectiveMacros();
        System.out.println(macros);
        assertThat(macros.getValue("EXAMPLE_MACRO"), equalTo("In Group 2"));
        assertThat(macros.getValue("TITLE"), equalTo("Display Title"));
    }

    /** Test access to widget properties, Java properties and environment */
    @Test
    public void testPropertiesAndEnvironment()
    {
        // Display model uses preferences
        final DisplayModel model = new DisplayModel();
        model.expandMacros(Preferences.getMacros());

        MacroValueProvider macros = model.getEffectiveMacros();
        assertThat(macros.getValue("EXAMPLE_MACRO"), nullValue());

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
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
            assertThat(value, nullValue());
        else
            assertThat(value, not(nullValue()));
    }

    /** Test when macros get expanded
     *  @throws Exception on error
     */
    @Test
    public void testTimeOfExpansion() throws Exception
    {
        // model -> group -> subgroup -> label
        final DisplayModel model = new DisplayModel();

        final GroupWidget group = new GroupWidget();
        model.runtimeChildren().addChild(group);

        final GroupWidget subgroup = new GroupWidget();
        group.runtimeChildren().addChild(subgroup);

        final LabelWidget label = new LabelWidget();
        subgroup.runtimeChildren().addChild(label);

        // Sanity check: Straight forward macro value replacement.
        // Display value replaced by group,
        // then replaced by subgroup,
        // so label sees "subgroup"
        model.propMacros().getValue().add("P", "display");
        group.propMacros().getValue().add("P", "group");
        subgroup.propMacros().getValue().add("P", "subgroup");
        model.expandMacros(null);
        Macros macros = label.getEffectiveMacros();
        System.out.println(macros.toExpandedString());
        assertThat(macros.getValue("P"), equalTo("subgroup"));

        // When are macros expanded?
        model.propMacros().getValue().add("P", "display");

        // Group has P=group, so SAVE=group
        group.propMacros().getValue().add("SAVE", "$(P)");

        // Subgroup had set P to subgroup, but now updates it to $(SAVE)=group
        subgroup.propMacros().getValue().add("P", "$(SAVE)");

        // With lazy macro expansion,
        // the label widget would have P=$(SAVE), SAVE=$(P),
        // so $(P) results in a "recursive macro" error.

        // When macros are expanded as the runtime starts..
        model.expandMacros(null);

        macros = label.getEffectiveMacros();
        System.out.println(macros.toExpandedString());
        assertThat(macros.getValue("P"), equalTo("group"));
        assertThat(MacroHandler.replace(macros, "$(P)"), equalTo("group"));
    }
}
