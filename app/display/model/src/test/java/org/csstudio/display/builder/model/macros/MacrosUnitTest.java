/*******************************************************************************
 * Copyright (c) 2015-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.macros;

import org.junit.jupiter.api.Test;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.Macros;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.LabelWidget;

/** JUnit test of macro handling
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacrosUnitTest
{
    /** Test redefinition of macros
     *  @throws Exception on error
     */
    @Test
    public void testMacroRedefinition() throws Exception
    {
        final Macros macros = new Macros();

        // Macro 101
        macros.add("X", "x");
        assertThat(MacroHandler.replace(macros, "$(X)"), equalTo("x"));

        // Macros can be re-defined
        macros.add("X", "a");
        assertThat(MacroHandler.replace(macros, "$(X)"), equalTo("a"));

        // This may be recursive, using the previous value
        macros.add("X", "$(X)$(X)$(X)$(X)$(X)");

        System.out.println("Specs: " + macros.toString());
        System.out.println("Value: " + macros.toExpandedString());

        assertThat(MacroHandler.replace(macros, "$(X)"), equalTo("aaaaa"));
    }

    @Test
    public void testXML() throws Exception
    {
        final Macros macros = new Macros();
        macros.add("N", "13");
        macros.add("S", "System");
        macros.add("N", "42");

        final String xml = MacroXMLUtil.toString(macros);
        System.out.println(xml);
        assertThat(xml, equalTo("<N>13</N><S>System</S><N>42</N>"));

        final Macros readback = MacroXMLUtil.readMacros(xml);
        assertThat(readback.getValue("S"), equalTo("System"));
        assertThat(readback.getValue("N"), equalTo("42"));
        assertThat(readback.getNames(), hasItems("S", "N"));
    }

    /** Test 'swap' of macros
     *  @throws Exception on error
     */
    @Test
    public void testMacroSwap() throws Exception
    {
        // display -> group -> label
        final DisplayModel display = new DisplayModel();

        final GroupWidget group = new GroupWidget();
        display.runtimeChildren().addChild(group);

        final LabelWidget label = new LabelWidget();
        group.runtimeChildren().addChild(label);

        // Display sets A=a, B=b
        display.propMacros().getValue().add("A", "a");
        display.propMacros().getValue().add("B", "b");

        // Group swaps them into A=b, B=a (and leaves X)
        group.propMacros().getValue().add("X", "$(A)");
        group.propMacros().getValue().add("A", "$(B)");
        group.propMacros().getValue().add("B", "$(X)");

         // Expanded macros from display down
        display.expandMacros(null);

        // Display keeps original settings
        Macros macros = display.getEffectiveMacros();
        System.out.println("Display: " + macros.toExpandedString());
        assertThat(MacroHandler.replace(macros, "$(A)"), equalTo("a"));
        assertThat(MacroHandler.replace(macros, "$(B)"), equalTo("b"));

        // Label gets the swapped A/B from the group
        macros = label.getEffectiveMacros();
        System.out.println("Label  : " + macros.toExpandedString());
        assertThat(MacroHandler.replace(macros, "$(A)"), equalTo("b"));
        assertThat(MacroHandler.replace(macros, "$(B)"), equalTo("a"));
    }
}
